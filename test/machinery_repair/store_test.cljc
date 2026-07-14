(ns machinery-repair.store-test
  (:require [clojure.test :refer [deftest testing is]]
            [machinery-repair.store :as store]))

(deftest mem-store-empty
  (testing "Empty store has no records"
    (let [s (store/mem-store)]
      (is (nil? (store/client-record s "client-1")))
      (is (nil? (store/equipment-record s "eq-1"))))))

(deftest mem-store-init-data
  (testing "Store can be initialized with data"
    (let [clients {"client-1" {:client-id "client-1" :client-name "Test"}}
          s (store/mem-store {:clients clients})]
      (is (= "Test" (:client-name (store/client-record s "client-1")))))))

(deftest client-verification
  (testing "Client must have all required fields to be verified"
    (let [incomplete {"c1" {:client-id "c1" :client-name "Test"}}
          complete {"c2" {:client-id "c2"
                         :client-name "Test"
                         :contact-phone "555-1234"
                         :contact-email "test@example.com"}}
          s-incomplete (store/mem-store {:clients incomplete})
          s-complete (store/mem-store {:clients complete})]
      (is (not (store/has-verified-client? s-incomplete "c1")))
      (is (store/has-verified-client? s-complete "c2")))))

(deftest equipment-verification
  (testing "Equipment must have all required fields to be verified"
    (let [incomplete {"eq-1" {:equipment-id "eq-1" :equipment-type "drill"}}
          complete {"eq-2" {:equipment-id "eq-2"
                           :equipment-type "drill"
                           :model "XL-3000"
                           :serial-number "SN-12345"
                           :site-location "Building A"
                           :failure-description "Motor not spinning"}}
          s-incomplete (store/mem-store {:equipment incomplete})
          s-complete (store/mem-store {:equipment complete})]
      (is (not (store/has-verified-equipment? s-incomplete "eq-1")))
      (is (store/has-verified-equipment? s-complete "eq-2")))))

(deftest safety-flags
  (testing "Safety flags can be added and checked"
    (let [s (store/mem-store)]
      (is (not (store/safety-flag-exists? s "eq-1")))
      (store/add-safety-flag s "eq-1" {:concern "high-voltage-exposed"})
      (is (store/safety-flag-exists? s "eq-1")))))

(deftest safety-flag-resolution
  (testing "Resolved safety flags are marked as such"
    (let [s (store/mem-store)]
      (store/add-safety-flag s "eq-1" {:concern "test"})
      (store/resolve-safety-flag s "eq-1")
      ; After resolution, flag still exists but is marked resolved
      ; (human must validate before clearing completely)
      (is (store/safety-flag-exists? s "eq-1")))))

(deftest work-history
  (testing "Repair history can be tracked per client"
    (let [history {"client-1" [{:repair-id "r1" :date "2026-07-01"}
                               {:repair-id "r2" :date "2026-07-02"}]}
          s (store/mem-store {:work-history history})]
      (is (= 2 (count (store/repair-history s "client-1")))))))
