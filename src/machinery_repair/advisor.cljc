(ns machinery-repair.advisor
  "Repair Coordinator Advisor -- the contained LLM/decision node.

  This is a mock advisor for development/testing. A real advisor would
  integrate with an LLM to generate repair intake assessments, technician
  dispatch decisions, parts recommendations, and safety concern flagging
  based on incoming requests and the store's history.

  The advisor has NO authority to:
    - verify clients or equipment (Governor owns that)
    - sign off on repair completion (technician only)
    - certify equipment safety (technician only)
    - override safety flags (human + technician only)

  It CAN propose:
    - intake assessments (:intake-repair-order)
    - technician dispatch (:schedule-technician-dispatch)
    - parts ordering (:order-parts)
    - safety concern escalation (:flag-safety-concern)")

(defprotocol Advisor
  "Decision-making interface for repair coordination."
  (-advise [this store request]
    "Generate a proposal in response to a request.
    Returns {:op :propose :effect :effect-type :value {...} :cites [...] :confidence 0.0-1.0 :summary \"...\"}"))

(deftype MockAdvisor []
  Advisor
  (-advise [_ _store request]
    (let [{:keys [op]} request]
      {:op op
       :effect :propose
       :value (:details request {})
       :cites []
       :confidence 0.8
       :summary (str "Mock proposal for " op)})))

(defn mock-advisor
  "Create a development-mode mock advisor."
  []
  (MockAdvisor.))

(defn trace
  "Audit fact for an advisor proposal (advisory event log)."
  [request proposal]
  {:t :advisor-proposed
   :op (:op request)
   :subject (:subject request)
   :confidence (:confidence proposal)
   :summary (:summary proposal)})
