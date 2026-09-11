(ns machinery-repair.operation
  "OperationActor -- one repair-shop coordination operation = one supervised
  actor run, expressed as a langgraph-clj StateGraph.

  The Repair Coordinator Advisor is sealed into a single node (:advise);
  its proposal is ALWAYS routed through the Repair Governor (:govern) and
  the rollout phase gate (:decide) before anything commits to the SSoT.

  Everything is injected (swappable):
    - the Store (MemStore today; Datomic/kotoba-server is the next seam)
    - the Advisor (mock | real LLM)
    - the Phase (0->3 rollout)

  One graph run = one repair operation (intake -> advise -> govern -> decide
  -> commit | hold | escalate). No unbounded inner loop -- each operation
  is auditable and checkpointed.

  Human-in-the-loop = approval workflow:
  `interrupt-before #{:request-approval}` pauses and hands to a human
  (shop manager/lead technician). The approver resumes with
  `{:approval {:status :approved}}` (or :rejected)."
  (:require [langgraph.graph :as g]
            [machinery-repair.advisor :as advisor]
            [machinery-repair.governor :as governor]
            [machinery-repair.phase :as phase]
            [machinery-repair.store :as store]))

(defn- commit-fact [request context proposal]
  {:t          :committed
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :commit
   :basis      (:cites proposal)
   :summary    (:summary proposal)})

(defn- commit-record [request _context proposal]
  {:effect  (:effect proposal)
   :path    [(:subject request)]
   :value   (or (:value proposal) {})
   :payload (:value proposal)})

(defn build
  "Compiles an OperationActor graph bound to `store` (any Store).
  opts:
    :advisor      -- a machinery-repair.advisor/Advisor (default: mock-advisor)
    :checkpointer -- langgraph checkpointer (default: in-mem)"
  [store & [{:keys [advisor]
             :or   {advisor (advisor/mock-advisor)}}]]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}   ; injected actor-id/role/phase
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}   ; :commit | :hold | :escalate
         :record      {:default nil}
         :approval    {:default nil}
         :audit       {:reducer into :default []}}})

      (g/add-node :intake (fn [s] s))

      ;; Repair Coordinator Advisor inference (the contained intelligence node)
      (g/add-node :advise
        (fn [{:keys [request]}]
          (let [p (advisor/-advise advisor store request)]
            {:proposal p :audit [(advisor/trace request p)]})))

      ;; Repair Governor -- independent censor (separate system from LLM)
      (g/add-node :govern
        (fn [{:keys [request context proposal]}]
          {:verdict (governor/check request context proposal store)}))

      ;; Decide: governor disposition, then phase gate (which can only add caution)
      (g/add-node :decide
        (fn [{:keys [request context verdict proposal]}]
          (let [base (phase/verdict->disposition verdict)
                ph   (:phase context phase/default-phase)
                {:keys [disposition reason]} (phase/gate ph request base)]
            (case disposition
              :hold
              {:disposition :hold
               :audit [(cond-> (governor/hold-fact request context verdict)
                         reason (assoc :phase-reason reason :phase ph))]}

              :escalate
              {:disposition :escalate
               :audit [{:t :approval-requested
                        :op (:op request)
                        :subject (:subject request)
                        :reason (or reason :escalation)
                        :phase ph}]}

              :commit
              {:disposition :commit
               :record (commit-record request context proposal)
               :audit [(commit-fact request context proposal)]}))))

      ;; Request human approval (holds here until external resume)
      (g/add-node :request-approval
        (fn [{:keys [request context audit]}]
          {:audit (conj audit
                         {:t :approval-requested-operator
                          :op (:op request)
                          :subject (:subject request)
                          :phase (:phase context)})}))

      ;; Terminal node (commit) -- every commit/hold/escalate outcome lands
      ;; in the store's append-only ledger (store/append-ledger!), not just
      ;; the graph run's transient :audit channel.
      (g/add-node :commit
        (fn [{:keys [disposition audit]}]
          (let [final (case disposition
                        :commit   {:t :operation-complete :status :committed}
                        :hold     {:t :operation-complete :status :held}
                        :escalate {:t :operation-complete :status :escalated}
                        {:t :operation-complete :status :unknown})]
            (store/append-ledger! store final)
            {:audit (conj audit final)})))

      ;; Edges: standard flow
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)

      ;; From decide: to approval or commit
      (g/add-conditional-edges :decide
        (fn [{:keys [disposition]}]
          (if (= disposition :escalate) :request-approval :commit))
        {:request-approval :request-approval :commit :commit})

      ;; Approval resumed externally
      (g/add-edge :request-approval :commit)

      ;; Start
      (g/set-entry-point :intake)
      (g/set-finish-point :commit)

      (g/compile-graph)))
