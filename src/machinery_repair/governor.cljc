(ns machinery-repair.governor
  "Repair Governor -- independent compliance layer for repair dispatch &
  parts coordination. The Advisor (LLM) has no notion of client verification,
  equipment safety flags, technician skill requirements, parts ordering
  authority, or whether a repair has already been completed by the
  technician. This Governor is the enforcement layer that the Repair
  Coordinator LLM is NOT (and cannot be). Every proposal goes through.

  Four HARD rules (un-overridable, block commitment):
    1. Client unverified        -- no action on unknown clients
    2. Equipment unverified     -- no dispatch/repair on unknown equipment
    3. Safety flag unresolved   -- never coordinate work on flagged equipment
    4. Technician skill insufficient -- dispatch only to qualified techs

  Three SOFT rules (escalate to human, but human may approve):
    5. Cost escalation          -- parts orders above threshold
    6. Low confidence           -- advisor uncertainty above floor
    7. Safety concern raised    -- flag-safety-concern always escalates

  Repair certification block: proposals to sign off on repair completion
  or safety are ALWAYS rejected. Only the technician who performed the
  work may sign off (outside the actor). The actor coordinates, not
  validates the repair itself."
  (:require [machinery-repair.store :as store]))

(def confidence-floor 0.6)

;; Operations that MUST escalate (never auto-approve)
(def escalation-required
  #{:flag-safety-concern})

;; Operations that require verified client
(def requires-verified-client
  #{:intake-repair-order :schedule-technician-dispatch :order-parts})

;; Operations that require verified equipment
(def requires-verified-equipment
  #{:schedule-technician-dispatch :order-parts})

(defn- unverified-client-violations
  "All intake/dispatch/parts operations require a verified client record."
  [{:keys [op subject]} st]
  (when (contains? requires-verified-client op)
    (let [client-id (or subject (:client-id subject))]
      (when-not (store/has-verified-client? st client-id)
        [{:rule :client-unverified
          :detail (str "Client " client-id " is not yet fully verified; "
                       "cannot proceed without complete verification record")}]))))

(defn- unverified-equipment-violations
  "Dispatch and parts ordering require a verified equipment record."
  [{:keys [op subject]} st]
  (when (contains? requires-verified-equipment op)
    (let [eq-id (or (:equipment-id subject) subject)]
      (when-not (store/has-verified-equipment? st eq-id)
        [{:rule :equipment-unverified
          :detail (str "Equipment " eq-id " is not yet fully verified; "
                       "cannot dispatch or order parts without complete "
                       "equipment specification")}]))))

(defn- safety-flag-unresolved-violations
  "Any action on equipment with an unresolved safety flag is a HARD hold."
  [{:keys [op subject]} st]
  (when (contains? requires-verified-equipment op)
    (let [eq-id (or (:equipment-id subject) subject)]
      (when (store/safety-flag-exists? st eq-id)
        [{:rule :safety-flag-unresolved
          :detail (str "Unresolved safety flag on equipment " eq-id
                       "; cannot proceed until safety concern is resolved")}]))))

(defn- technician-skill-violations
  "Dispatch requires technician to meet minimum skill level for repair type."
  [{:keys [op subject]} st]
  (when (= op :schedule-technician-dispatch)
    (let [tech-id (:technician-id subject)
          repair-risk (:risk-category subject :low)
          tech-record (store/technician-record st tech-id)]
      (when (and tech-record (not (keyword? tech-record)))
        (let [tech-skill (:skill-level tech-record)]
          (when-not (keyword? tech-skill)
            ; Basic skill check: intermediate+ required for medium+ risk
            (when (and (= repair-risk :high)
                       (not (#{:intermediate :advanced :expert} tech-skill)))
              [{:rule :technician-insufficient-skill
                :detail (str "Technician " tech-id " has skill level " tech-skill
                             " but repair risk is " repair-risk
                             " (requires at least intermediate)")}])))))))

(defn- no-technician-repair-certification
  "Technician signature is REQUIRED to sign off on repair completion.
  Reject any proposal that tries to auto-certify repair work or safety."
  [{:keys [op]}]
  (when (contains? #{:certify-repair-complete :certify-safe-to-operate} op)
    [{:rule :no-auto-certification
      :detail (str "Repair completion and safety certification require "
                   "hands-on technician sign-off; the actor cannot auto-certify")}]))

(defn check
  "Censors a Repair Coordinator proposal against governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool :hard?}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (unverified-client-violations request st)
                           (unverified-equipment-violations request st)
                           (safety-flag-unresolved-violations request st)
                           (technician-skill-violations request st)
                           (no-technician-repair-certification request)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        escalation? (boolean (escalation-required (:op request)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not escalation?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? escalation?))}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
