(ns machinery-repair.facts
  "Domain facts and static constants for machinery repair operations.

  A repair shop coordinates intake of broken/malfunctioning equipment,
  dispatch of technicians, and parts procurement. The actor manages
  back-office workflow (intake verification, scheduling coordination,
  parts ordering) — NOT hands-on repair execution (that is the
  technician's exclusive domain).

  Repair operations follow a standard protocol:
    1. Client intake & equipment verification
    2. Technician dispatch/site assessment
    3. Parts procurement if needed
    4. Repair work (technician authority, outside actor scope)
    5. Safety signoff & delivery

  Key facts:
  - All actions require verified client & equipment records
  - Parts orders may trigger cost-override escalation
  - Safety concerns always escalate (human-only resolution)
  - Repair/safety certification is technician-only (no actor override)")

;; Minimum cost threshold (in shop's currency) for parts-order auto-escalation
(def parts-cost-escalation-threshold 500)

;; Confidence floor for non-critical operations
(def confidence-floor 0.6)

;; Minimum required tech skill level to dispatch for a repair
(def technician-skill-required :intermediate)

;; Safe delivery conditions that must be verified post-repair
(def safe-delivery-checklist
  #{:equipment-test-passed
    :client-acceptance-signed
    :safety-label-updated
    :documentation-complete})

;; Risk categories for repairs (used to determine escalation)
(def risk-categories
  {:high    "repair involves safety-critical equipment or high-voltage"
   :medium  "repair is routine but requires parts order"
   :low     "diagnostic or minor adjustment only"})

;; Cost range for auto-approval (under this, parts orders don't escalate)
(def auto-approve-parts-range [0 parts-cost-escalation-threshold])

;; Client verification fields required before any action
(def client-verification-required
  #{:client-id :client-name :contact-phone :contact-email})

;; Equipment verification fields required before repair dispatch
(def equipment-verification-required
  #{:equipment-id :equipment-type :model :serial-number :site-location :failure-description})
