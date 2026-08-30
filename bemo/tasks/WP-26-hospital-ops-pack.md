# WP-26 — Hospital Ops Pack: ADT Beds, OT, Nursing (hospital-grade)
**Priority:** 🟢 · **Owner:** Squad BE H + FE E · **Depends on:** WP-15 + WP-21 · **Effort:** ~2 weeks
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.2/§14.9/§14.10

## Business goal
Hospital inpatient core: admit/discharge/transfer with a live bed board, operating-theater scheduling with implants charging, and nursing records (MAR, intake/output). Ship after clinic MVP proves the medical vertical.

## Backend steps
1. Structure tables: `wards` → `rooms` → `beds` (status FREE|OCCUPIED|MAINTENANCE|ISOLATION). `admissions` (patient FK, bed FK history via `admission_bed_stays`, admitted_at, discharged_at NULL, discharge_summary_text NULL, status ADMITTED|DISCHARGED; unique open-admission-per-patient).
2. ADT endpoints: admit (assign free bed — race-safe via conditional update), transfer (closes bed-stay, opens new; both evidence rows), discharge (requires summary ≥ N chars config).
3. OT: `ot_schedule` (theater room, surgery_type, patient, planned_start/duration, actual times nullable, anesthesia_note) + charge lines for implants/consumables picked from stock (reuse WP-23 dispense path into patient bill).
4. Nursing: `medication_administrations` (admission FK, pharmacy item, due_at, status DUE|GIVEN|REFUSED|HELD, nurse_id, note) generated from active Rx; `fluid_io_entries` (intake/output ml, time); nursing notes text stream.
5. KPIs: occupancy % per ward, bed turnover, ALOS (avg length of stay) endpoint feeding dashboards.
6. Codes: `BED_*`, `ADMISSION_*`, `OT_*`, `MAR_*` families (~14).

## Frontend steps
1. Bed board: wards × rooms grid, color+icon coded statuses (never color alone), click bed → patient card / admit dialog; live refresh ≤5s.
2. Admission chart page: timeline of stays/transfers, MAR worksheet grouped by due-time with Given/Refused/Held quick actions, I/O tally running total.
3. OT board by theater/day + surgery detail drawer.
4. Keys `clinic.adt* / ot* / nursing*` (~30).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Two nurses admitting to the LAST free bed concurrently: exactly one succeeds (conditional-update test), loser gets translated "bed just occupied".
- [x] AC-2 Transfer closes prior bed (FREE again) and occupies target atomically; bed-stay evidence chain complete for billing.
- [x] AC-3 Discharge blocked until summary length met; ALOS calc matches manual fixture across 10 admissions incl. same-day discharge.
- [x] AC-4 MAR generation creates one row per scheduled dose from active Rx; GIVEN consumes stock via dispense path (ties WP-23); REFUSED/HELD leave stock untouched.
- [x] AC-5 Implant charge during OT posts to patient bill and decrements inventory once (idempotent confirm).
- [x] AC-6 Occupancy board reflects every state change ≤5s across two browsers; all states distinguishable without color (icon/text test).
