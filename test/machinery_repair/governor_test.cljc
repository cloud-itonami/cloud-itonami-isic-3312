(ns machinery-repair.governor-test
  (:require [clojure.test :refer [deftest testing is]]
            [machinery-repair.governor :as governor]
            [machinery-repair.store :as store]))

(deftest check-unverified-client
  (testing "Intake on unverified client is rejected"
    (let [s (store/mem-store)
          request {:op :intake-repair-order :subject "client-123"}
          verdict (governor/check request {} {} s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict))
      (is (some #(= :client-unverified (:rule %)) (:violations verdict))))))

(deftest check-verified-client
  (testing "Intake on verified client passes"
    (let [clients {"client-123" {:client-id "client-123"
                                 :client-name "Test Shop"
                                 :contact-phone "555-1234"
                                 :contact-email "test@example.com"}}
          s (store/mem-store {:clients clients})
          request {:op :intake-repair-order :subject "client-123"}
          proposal {:effect :propose :confidence 0.9}
          verdict (governor/check request {} proposal s)]
      (is (:ok? verdict)))))

(deftest check-unverified-equipment
  (testing "Dispatch on unverified equipment is rejected"
    (let [clients {"client-123" {:client-id "client-123"
                                 :client-name "Test"
                                 :contact-phone "555-1234"
                                 :contact-email "test@example.com"}}
          s (store/mem-store {:clients clients})
          request {:op :schedule-technician-dispatch
                   :subject {:client-id "client-123" :equipment-id "eq-1"}}
          verdict (governor/check request {} {} s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict)))))

(deftest check-low-confidence
  (testing "Low confidence triggers escalation"
    (let [clients {"client-123" {:client-id "client-123"
                                 :client-name "Test"
                                 :contact-phone "555-1234"
                                 :contact-email "test@example.com"}}
          s (store/mem-store {:clients clients})
          request {:op :intake-repair-order :subject "client-123"}
          proposal {:effect :propose :confidence 0.4}
          verdict (governor/check request {} proposal s)]
      (is (not (:ok? verdict)))
      (is (:escalate? verdict)))))

(deftest check-safety-concern-escalates
  (testing "Safety concern flag always escalates"
    (let [s (store/mem-store)
          request {:op :flag-safety-concern :subject "eq-1"}
          proposal {:effect :propose :confidence 0.9}
          verdict (governor/check request {} proposal s)]
      (is (:escalate? verdict)))))

(deftest no-auto-repair-certification
  (testing "Reject proposals to auto-certify repair work"
    (let [s (store/mem-store)
          request {:op :certify-repair-complete :subject "repair-1"}
          verdict (governor/check request {} {} s)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict)))))
