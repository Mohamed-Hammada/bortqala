# WP-43 — Rentals, Service Work Orders & Generic Bookings (Daftra trio)
**Priority:** 🟢 · **Owner:** Backend dev A · **Depends on:** — · **Effort:** ~9 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20

## Business goal
Three service-economy modules Daftra/Odoo ship: (1) rent out equipment/units with contracts and utilization; (2) job-shop work orders (repair/print/workshop tickets) with status board; (3) bookable appointments for any resource (rooms, trainers, machines) — generalizes medical WP-22 later.

## Backend steps
1. Rentals: `rental_items` (inventory item FK or standalone unit, rate per DAY/WEEK/MONTH, deposit) · `rental_contracts` (party FK, lines[item, from, to NULL open], status DRAFT|ACTIVE|CLOSED, charges computed on close = periods×rate + damage fee manual) — close creates customer invoice; overdue-open list; utilization report (rented-days/available-days).
2. Work orders: `work_orders` (ticket_no sequence per tenant, party FK nullable walk-in, title, description, assigned_employee_id, priority, status OPEN|IN_PROGRESS|WAITING_PARTS|DONE|DELIVERED|CANCELLED, promised_at) + `wo_labor_lines` (hours × hourly rate) + parts from stock (reuse movement issue); DELIVERED → invoice draft.
3. Bookings v1 generic: reuse appointments pattern generalized — `resources` (name, kind) + slots engine parameterized by resource instead of doctor only (refactor WP-22 engine behind interface if landed; else copy minimal).
4. Codes ~14.

## Frontend steps
1. Three tabs under new `features/service-ops/` (or separate features if size demands): rentals calendar-ish list + contract wizard; WO kanban columns by status with drag-free quick-move buttons + parts picker; bookings day grid per resource.
2. Full menu registration for the feature (`serviceOps`).
3. Keys ~30.

## Acceptance Criteria (QA sign-off)
- AC block:
- [x] AC-1 Rental 12 days @ weekly rate uses 7+5 pricing rule as configured (period-billing fixture); overdue open contract appears in aging and blocks new rental to same party when property set.
- [x] AC-2 Close contract posts invoice lines exactly matching computed charges incl. damage fee; deposit handling documented (refund line).
- [x] AC-3 WO moving WAITING_PARTS→DONE requires parts issued OR override note; delivery creates one draft invoice containing labor+parts totals.
- [x] AC-4 Stock parts issue decrements inventory once; cancel after issue reverses movement.
- [x] AC-5 Utilization % matches manual calc for a month fixture; booking double-slot race rejected like WP-22.

