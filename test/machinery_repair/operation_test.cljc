(ns machinery-repair.operation-test
  (:require [clojure.test :refer [deftest testing is]]
            [langgraph.graph :as g]
            [machinery-repair.operation :as operation]
            [machinery-repair.store :as store]))

(defn- run-op! [store request & [context]]
  (g/invoke (operation/build store)
            {:request request :context (or context {:phase :phase-3})}))

(deftest ledger-empty-until-run
  (testing "A fresh store's ledger is empty before any operation runs"
    (let [s (store/mem-store)]
      (is (empty? (store/ledger s))))))

(deftest hard-violation-holds-and-appends-hold-ledger-entry
  (testing "Unverified client blocks intake -- HARD hold, ledger gets a :held entry"
    (let [s (store/mem-store)
          result (run-op! s {:op :intake-repair-order :subject "client-unknown"})]
      (is (= :hold (:disposition result)))
      (is (= 1 (count (store/ledger s))))
      (is (= :held (:status (first (store/ledger s))))))))

(deftest safety-concern-always-escalates
  (testing ":flag-safety-concern always escalates, never auto-commits, even at phase-3"
    (let [s (store/mem-store)
          result (run-op! s {:op :flag-safety-concern :subject "eq-1"})]
      (is (= :escalate (:disposition result)))
      (is (= :escalated (:status (first (store/ledger s))))))))

(deftest clean-proposal-commits-at-phase-3-and-appends-ledger-entry
  (testing "A governor-clean, phase-3-allowed proposal commits and lands in the ledger"
    (let [clients {"client-1" {:client-id "client-1"
                                :client-name "Test"
                                :contact-phone "555-1234"
                                :contact-email "test@example.com"}}
          s (store/mem-store {:clients clients})
          result (run-op! s {:op :intake-repair-order :subject "client-1"})]
      (is (= :commit (:disposition result)))
      (is (= 1 (count (store/ledger s))))
      (is (= :committed (:status (first (store/ledger s))))))))

(deftest phase-0-requires-approval-even-for-clean-proposal
  (testing "Phase 0 requires human approval for every commit-eligible op"
    (let [clients {"client-1" {:client-id "client-1"
                                :client-name "Test"
                                :contact-phone "555-1234"
                                :contact-email "test@example.com"}}
          s (store/mem-store {:clients clients})
          result (run-op! s {:op :intake-repair-order :subject "client-1"} {:phase :phase-0})]
      (is (= :escalate (:disposition result)))
      (is (= :escalated (:status (first (store/ledger s))))))))

(deftest each-run-appends-exactly-one-ledger-entry
  (testing "Ledger accumulates one entry per operation run, never more"
    (let [s (store/mem-store)]
      (run-op! s {:op :flag-safety-concern :subject "eq-1"})
      (run-op! s {:op :flag-safety-concern :subject "eq-2"})
      (is (= 2 (count (store/ledger s)))))))
