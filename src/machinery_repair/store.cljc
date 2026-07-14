(ns machinery-repair.store
  "In-memory store for repair requests, client records, equipment inventory,
  and completed work history. The store is the single source of truth for
  the actor's decisions — all governor checks query against it."
  (:require [machinery-repair.facts :as facts]))

(defprotocol Store
  "Storage interface for repair shop records."
  (client-record [this client-id]
    "Retrieve full client verification record.")
  (equipment-record [this equipment-id]
    "Retrieve full equipment verification record.")
  (repair-intake [this repair-id]
    "Retrieve a repair intake request by ID.")
  (all-open-intakes [this]
    "Retrieve all currently-open (unresolved) repair intakes.")
  (repair-history [this client-id]
    "Retrieve completed repair work history for a client.")
  (technician-record [this tech-id]
    "Retrieve technician record (skill level, certifications).")
  (safety-flag-exists? [this equipment-id]
    "Check if there's an unresolved safety flag on equipment.")
  (add-safety-flag [this equipment-id flag-data]
    "Record a new safety concern flag.")
  (resolve-safety-flag [this equipment-id]
    "Mark a safety flag as resolved (human signoff only).")
  (has-verified-client? [this client-id]
    "Check if client is fully verified.")
  (has-verified-equipment? [this equipment-id]
    "Check if equipment is fully verified.")
  (record-parts-order [this repair-id order-details]
    "Record a parts order against a repair intake.")
  (repair-work-complete? [this repair-id]
    "Check if hands-on repair work is marked complete by technician.")
  (repair-safely-deliverable? [this repair-id]
    "Check if repair passes all safe-delivery conditions."))

(deftype MemStore [clients-db equipment-db intakes-db technicians-db
                   safety-flags-db parts-orders-db work-history]
  Store
  (client-record [_ client-id]
    (get @clients-db client-id))

  (equipment-record [_ equipment-id]
    (get @equipment-db equipment-id))

  (repair-intake [_ repair-id]
    (get @intakes-db repair-id))

  (all-open-intakes [_]
    (into [] (filter #(= :open (:status (val %))) @intakes-db)))

  (repair-history [_ client-id]
    (get @work-history client-id []))

  (technician-record [_ tech-id]
    (get @technicians-db tech-id))

  (safety-flag-exists? [_ equipment-id]
    (boolean (get @safety-flags-db equipment-id)))

  (add-safety-flag [_ equipment-id flag-data]
    (swap! safety-flags-db assoc equipment-id
           {:flag-id (str equipment-id "-flag-" (random-uuid))
            :equipment-id equipment-id
            :status :unresolved
            :data flag-data}))

  (resolve-safety-flag [_ equipment-id]
    (when-let [flag (get @safety-flags-db equipment-id)]
      (swap! safety-flags-db assoc equipment-id (assoc flag :status :resolved))))

  (has-verified-client? [_ client-id]
    (let [client (get @clients-db client-id)]
      (and client
           (every? #(contains? client %) facts/client-verification-required))))

  (has-verified-equipment? [_ equipment-id]
    (let [equip (get @equipment-db equipment-id)]
      (and equip
           (every? #(contains? equip %) facts/equipment-verification-required))))

  (record-parts-order [_ repair-id order-details]
    (swap! parts-orders-db assoc repair-id order-details))

  (repair-work-complete? [_ repair-id]
    (let [intake (get @intakes-db repair-id)]
      (= :work-complete (:status intake))))

  (repair-safely-deliverable? [_ repair-id]
    (let [intake (get @intakes-db repair-id)]
      (and (repair-work-complete? _ repair-id)
           (every? #(contains? (set (:delivery-checks intake [])) %)
                   facts/safe-delivery-checklist)))))

(defn mem-store
  "Create an in-memory store with optional initial data.
   Takes an optional map with keys: :clients :equipment :intakes :technicians
   :safety-flags :parts-orders :work-history"
  ([]
   (MemStore. (atom {})
              (atom {})
              (atom {})
              (atom {})
              (atom {})
              (atom {})
              (atom {})))
  ([{:keys [clients equipment intakes technicians safety-flags parts-orders work-history]}]
   (MemStore. (atom (or clients {}))
              (atom (or equipment {}))
              (atom (or intakes {}))
              (atom (or technicians {}))
              (atom (or safety-flags {}))
              (atom (or parts-orders {}))
              (atom (or work-history {})))))
