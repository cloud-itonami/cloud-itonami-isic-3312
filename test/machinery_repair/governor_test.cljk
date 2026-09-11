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

(deftest check-map-subject-resolves-client-id
  (testing "A map-shaped subject resolves :client-id -- a verified client plus
           verified equipment clears every HARD rule for dispatch"
    (let [clients {"client-123" {:client-id "client-123"
                                 :client-name "Test"
                                 :contact-phone "555-1234"
                                 :contact-email "test@example.com"}}
          equipment {"eq-1" {:equipment-id "eq-1"
                             :equipment-type "pump"
                             :model "P-100"
                             :serial-number "SN-1"
                             :site-location "Bay 3"
                             :failure-description "seal leak"}}
          s (store/mem-store {:clients clients :equipment equipment})
          request {:op :schedule-technician-dispatch
                   :subject {:client-id "client-123" :equipment-id "eq-1"
                             :technician-id "tech-1"}}
          verdict (governor/check request {} {:confidence 0.9} s)]
      (is (not (:hard? verdict)))
      (is (empty? (:violations verdict)))
      (is (:ok? verdict)))))

(deftest check-safety-flag-blocks-parts-order
  (testing "An unresolved safety flag HARD-holds a parts order on verified equipment"
    (let [clients {"client-123" {:client-id "client-123"
                                 :client-name "Test"
                                 :contact-phone "555-1234"
                                 :contact-email "test@example.com"}}
          equipment {"eq-9" {:equipment-id "eq-9"
                             :equipment-type "turbine"
                             :model "T-9"
                             :serial-number "SN-9"
                             :site-location "Hall B"
                             :failure-description "vibration"}}
          flags {"eq-9" {:equipment-id "eq-9" :status :unresolved}}
          s (store/mem-store {:clients clients :equipment equipment
                              :safety-flags flags})
          request {:op :order-parts
                   :subject {:client-id "client-123" :equipment-id "eq-9"}}
          verdict (governor/check request {} {:confidence 0.9} s)]
      (is (:hard? verdict))
      (is (some #(= :safety-flag-unresolved (:rule %)) (:violations verdict))))))

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
