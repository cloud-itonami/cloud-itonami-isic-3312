(ns machinery-repair.phase-test
  (:require [clojure.test :refer [deftest testing is]]
            [machinery-repair.phase :as phase]))

(deftest verdict-to-disposition-hard-violation
  (testing "Hard violations become :hold"
    (let [verdict {:ok? false :hard? true :escalate? false}
          disposition (phase/verdict->disposition verdict)]
      (is (= :hold disposition)))))

(deftest verdict-to-disposition-escalation
  (testing "Escalations become :escalate"
    (let [verdict {:ok? false :hard? false :escalate? true}
          disposition (phase/verdict->disposition verdict)]
      (is (= :escalate disposition)))))

(deftest verdict-to-disposition-clean
  (testing "Clean passes become :commit"
    (let [verdict {:ok? true :hard? false :escalate? false}
          disposition (phase/verdict->disposition verdict)]
      (is (= :commit disposition)))))

(deftest phase-0-requires-approval
  (testing "Phase 0 (dev) requires human approval for clean passes"
    (let [request {:op :intake-repair-order}
          result (phase/gate :phase-0 request :commit)]
      (is (= :escalate (:disposition result)))
      (is (= :phase-requires-approval (:reason result))))))

(deftest phase-2-auto-approves
  (testing "Phase 2 (full ops) auto-approves clean passes"
    (let [request {:op :intake-repair-order}
          result (phase/gate :phase-2 request :commit)]
      (is (= :commit (:disposition result))))))

(deftest phase-escalation-stays-escalation
  (testing "Escalations remain escalations regardless of phase"
    (doseq [ph [:phase-0 :phase-1 :phase-2 :phase-3]]
      (let [request {:op :intake-repair-order}
            result (phase/gate ph request :escalate)]
        (is (= :escalate (:disposition result)))
        (is (= :escalation-required (:reason result)))))))

(deftest phase-holds-stay-holds
  (testing "Governor holds remain holds regardless of phase"
    (doseq [ph [:phase-0 :phase-1 :phase-2 :phase-3]]
      (let [request {:op :intake-repair-order}
            result (phase/gate ph request :hold)]
        (is (= :hold (:disposition result)))
        (is (= :governor-hard-violation (:reason result)))))))
