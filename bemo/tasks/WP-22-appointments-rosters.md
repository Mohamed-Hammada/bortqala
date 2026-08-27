# WP-22 — Appointments, Doctor Rosters & Reminders
**Priority:** 🟢 · **Owner:** Full-stack B · **Depends on:** WP-15 · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.3

## Business goal
Replace walk-in-only flow with booked appointments: doctor slot templates, booking, reminders via WhatsApp/SMS (reusing CRM channel plumbing), no-show tracking.

## Backend steps
1. Tables: `doctor_rosters` (doctor_employee_id, weekday 0..6, start_time, end_time, slot_minutes 10..60, valid_from/to) · `appointments` (patient FK, doctor FK, starts_at Instant epoch-millis, status BOOKED|CONFIRMED|CHECKED_IN|NO_SHOW|CANCELLED|DONE, source WALKIN|PHONE|ONLINE|WHATSAPP, reminder_sent_at NULL, unique(doctor, starts_at)).
2. Slot engine: generate open slots from roster minus booked minus leave (leave module integration if present). Endpoint `GET /api/v1/clinic/slots?doctorId=&date=`.
3. Booking rules: no past slots, double-book blocked (`SLOT_TAKEN`), cancel frees slot; CHECKED_IN links/creates a clinic_visit (WP-15).
4. Reminder job: daily cron for next-day appointments → enqueue outbound message via CRM omnichannel sender (channel per patient preference; log attempt on reminder_sent_at).
5. No-show KPI: counts feed dashboard endpoint.
6. Codes: `SLOT_TAKEN/SLOT_OUTSIDE_ROSTER/APPT_PAST/Roster overlap codes`.

## Frontend steps
1. Week calendar grid per doctor (CSS grid, no lib): columns days × rows time, colored blocks; click empty slot → quick book dialog (search patient inline); drag not required v1.
2. Today panel: upcoming list with Confirm/Check-in/No-show buttons; check-in jumps to queue.
3. Keys `clinic.appt*` (~20).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Slots match roster exactly; booked/leave slots absent; crossing DST-less zone math verified by fixture.
- [ ] AC-2 Double-booking same doctor+time rejected translated even under race (unique constraint test).
- [ ] AC-3 Check-in creates linked visit appearing on queue board with source=appointment.
- [ ] AC-4 Reminder cron marks reminder_sent_at once; re-run same night doesn't duplicate (idempotency test).
- [ ] AC-5 No-show rate appears on dashboard for selected month matching manual count.
