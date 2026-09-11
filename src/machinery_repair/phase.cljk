(ns machinery-repair.phase
  "Rollout phases for repair shop coordination actor. Phases gate which
  operations are permitted at which maturity levels.

    Phase 0: Development/Testing (all operations, human approval required)
    Phase 1: Limited Ops (intake + dispatch only, cost escalation on parts)
    Phase 2: Full Ops (all except high-cost parts, cost escalation threshold rises)
    Phase 3: Autonomous (full autonomous except flagged/safety escalations)

  Each phase is a cautious step forward. No operation auto-commits at any
  phase (all require approval, explicitly or via phase-escalation rule).
  Safety escalations are identical across all phases.")

(def default-phase :phase-0)

(def phase-definitions
  {:phase-0 {:name "Development"
             :allowed-ops #{:intake-repair-order
                           :schedule-technician-dispatch
                           :order-parts
                           :flag-safety-concern}
             :requires-approval? true
             :auto-approve? false}

   :phase-1 {:name "Limited Operations"
             :allowed-ops #{:intake-repair-order
                           :schedule-technician-dispatch
                           :order-parts
                           :flag-safety-concern}
             :requires-approval? true
             :cost-escalation-threshold 500
             :auto-approve? false}

   :phase-2 {:name "Full Operations"
             :allowed-ops #{:intake-repair-order
                           :schedule-technician-dispatch
                           :order-parts
                           :flag-safety-concern}
             :requires-approval? false
             :cost-escalation-threshold 1000
             :auto-approve? true}

   :phase-3 {:name "Autonomous"
             :allowed-ops #{:intake-repair-order
                           :schedule-technician-dispatch
                           :order-parts
                           :flag-safety-concern}
             :requires-approval? false
             :cost-escalation-threshold 2000
             :auto-approve? true}})

(defn gate
  "Apply phase rules to a governor verdict. Returns {:disposition :commit|:hold|:escalate
  :reason (when gated)}."
  [phase _request base-disposition]
  (let [phase-def (get phase-definitions phase default-phase)]
    (cond
      ;; Hard blocks stay blocks regardless of phase
      (= base-disposition :hold)
      {:disposition :hold
       :reason :governor-hard-violation}

      ;; Escalation for low-confidence or safety concerns stays escalation
      (= base-disposition :escalate)
      {:disposition :escalate
       :reason :escalation-required}

      ;; Commit path: respect phase approval requirement
      (= base-disposition :commit)
      (if (:requires-approval? phase-def)
        {:disposition :escalate
         :reason :phase-requires-approval}
        {:disposition :commit
         :reason (str "auto-approved by " (:name phase-def))}))))

(defn verdict->disposition
  "Convert governor verdict to initial disposition before phase gating.
  Governor holds become :hold, escalations become :escalate, clean passes
  become :commit."
  [{:keys [ok? hard? escalate?]}]
  (cond
    hard? :hold
    escalate? :escalate
    ok? :commit
    :else :hold))
