# cloud-itonami-3312

Open Business Blueprint for **ISIC Rev.5 3312**: repair of machinery
(diagnostic, overhaul and certified repair of industrial equipment
such as pumps, compressors, turbines and manufacturing machinery).

This repository designs a forkable OSS business for community
machinery repair: repair-authority and certification-scope
management, robotics-assisted diagnostics, teardown, part
replacement and post-repair inspection, and repair/certification
records — run by a qualified operator so an industrial repair shop
keeps its own certification and repair history instead of renting a
closed repair-operations platform.

## Scope note: repair, not manufacturing or cleaning

`cloud-itonami-isic-3020` ("Community Railway Rolling Stock
Manufacturing") and other manufacturing verticals in this fleet build
NEW equipment; this repository is deliberately scoped to the SEPARATE
business of repairing and overhauling EXISTING industrial equipment
owned by other operators. Also distinct from `cloud-itonami-unspsc-73`
("Independent Industrial Cleaning & Certification Robotics"): that
business cleans and certifies contamination-free equipment/facilities,
while this repository repairs and recertifies the FUNCTIONAL
condition of the equipment itself -- a different scope of work under
a different regulatory regime. Machinery repair frequently requires
its own certifications distinct from original manufacture: the ASME
"R" stamp for authorized repair of pressure vessels/boilers in the
US, EU Pressure Equipment Directive (PED)-compliant repair procedures,
and OEM certified-service-provider programs that gate warranty
validity on using an authorized repair process.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (diagnostic scanning,
teardown/reassembly assist, non-destructive-testing inspection)
operate under an actor that proposes actions and an independent
**Machinery Repair Governor** that gates them. The governor never
releases a repaired unit back into service itself;
`:high`/`:safety-critical` actions (a repair step outside verified
repair-authority scope, a return-to-service release without a
completed post-repair inspection, a certification record without
verified evidence) require human sign-off.

## Core Contract

```text
intake + identity + repair-authority/certification scope + work order
        |
        v
Machinery Repair Advisor -> Machinery Repair Governor -> repair record, inspection record, release, or human approval
        |
        v
robot actions (gated) + repair record + certification record + audit ledger
```

No automated advice can release a unit back into service the governor
refuses, advance a repair step outside its verified repair-authority
scope, or publish a certification record without governor approval
and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `3312`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) — missions, actions, safety-stops, telemetry proofs
- [`kotoba-lang/eda`](https://github.com/kotoba-lang/eda) — repair-authority artifact management
- [`kotoba-lang/cae`](https://github.com/kotoba-lang/cae) — structural/stress-analysis simulation evidence

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
