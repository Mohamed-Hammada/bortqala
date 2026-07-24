# HR attendance platform — executable business scope

## Confirmed scope

- Configure attendance categories instead of hard-coding job titles. Examples include 8-hour administrators, 10-hour accountants, 12-hour security staff, and daily workers settled every half month.
- Each category chooses `BIOMETRIC`, `MANUAL`, or `HYBRID` attendance. Daily/manual workers are never marked absent merely because they have no device punch; HR records the worked/deduct/leave decision and optional worked minutes.
- A category has default daily minutes, while each effective schedule rule may override those minutes. This keeps dynamic hours understandable without a general-purpose rules engine.
- Configure effective-dated schedules such as summer starting at 08:00 and winter starting at 09:00, per category.
- Import biometric CSV/XLS/XLSX files, retain every source punch unchanged, map stable device user ids to employees, and surface unmatched identities for review.
- Calculate expected and worked minutes for each employee/day, including first/last punch, lateness, early leave, overtime, status, rule evidence, and warnings.
- A configured category may treat one punch as presence, while the result remains a review warning.
- No punch requires an HR decision: deduct, normal day, approved leave, or another configured reason.
- When all active employees in a category are absent, propose a holiday. A user must confirm or reject it, and confirmation is persisted.
- Reports expose a full-month period for monthly categories and two periods (1-15 and 16-month-end) for half-monthly categories. Each report calculates only categories with its matching pay cycle, cannot be approved with blocking exceptions, and becomes an immutable snapshot when approved.
- Export the current table/report or the full approved data set to Excel.
- Authenticate with username/password and JWT. A user can hold multiple roles: admin, HR manager, HR reviewer, and viewer.
- Require an SaaS application code at login. Every user and operational row belongs to one application and must never be visible from another application's JWT.
- Let the user select any report range up to 366 days and the target pay cycle. Prevent overlapping reports for the same pay cycle; keep full-month and half-month presets as shortcuts.
- Persist each user's light/dark/system theme, comfortable/compact table density, and Arabic/English locale.
- Store platform translation keys and both `ar-EG`/`en-US` values in the database, expose read-only bundles to the UI, and switch RTL/LTR immediately.
- Send all work dates and audit timestamps between frontend/backend as epoch milliseconds; keep schedule clock time as `HH:mm`.

## Role matrix

| Capability | ADMIN | HR_MANAGER | HR_REVIEWER | VIEWER |
|---|---:|---:|---:|---:|
| Manage users and roles | ✓ |  |  |  |
| Configure categories, schedules, employees | ✓ | ✓ |  |  |
| Import biometric files and map identities | ✓ | ✓ | ✓ |  |
| Create reports and resolve exceptions | ✓ | ✓ | ✓ |  |
| Approve or reopen reports | ✓ | ✓ |  |  |
| Read dashboards/reports and export permitted data | ✓ | ✓ | ✓ | ✓ |

Authorization is enforced by the backend. Angular uses the same JWT roles for navigation and action visibility, but UI hiding is not treated as security.

## Audit and tracing

- Angular sends a new `X-Correlation-Id` per API request and a stable, random `X-Device-Id` per browser installation.
- Spring generates `X-Server-Correlation-Id` for every request and returns both correlation ids to the caller.
- Structured Logstash JSON includes correlation ids, authenticated user id/name, roles, remote IP, device id, HTTP method/path/status, duration, and bounded user-agent.
- Passwords, JWT values, request/response bodies, and query strings are not logged.

## Photo notes reviewed on 2026-07-24

The handwritten photos in `C:\Users\wolfn\Downloads\notes` reinforce flexible category and daily-table requirements. Legible candidate groups include cleaning, administration/accounting, drivers/raw-material receiving, operations/washing, export, sorting, and freezer/small-fruit work. The page also shows device/person codes and day-by-day attendance/leave columns.

Some compensation wording and numeric values are not legible enough to encode safely. They remain configuration/discovery items and must be confirmed before payroll formulas are added. The attendance system will preserve the necessary facts without inventing those formulas.

## Explicitly deferred integrations

- Vendor-specific biometric-device SDK or live network synchronization; the MVP uses file imports behind a replaceable reader boundary.
- Payment execution and bank integration. The system produces payroll-ready attendance evidence only.
- Enterprise SSO. Local username/password JWT authentication is implemented first.
