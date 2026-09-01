# TASK 06 — Dashboard

Priority: P1
Status: ☑ Verified

## Required
1. Above-the-fold hierarchy is KPI → alerts/actions → primary business health.
2. Period/year change does not fire redundant requests.
3. Enter/change behavior is deterministic.
4. Loading state does not flash stale data as current data.
5. Error state identifies the failed section and provides retry.
6. Empty state explains why data is absent.
7. Attendance chart has a textual/accessibility fallback.
8. Chart tooltip is supplemental, not the only source of data.
9. KPI values include labels, units and period context.
10. Numbers are localized correctly in Arabic/English.
11. Dashboard cards remain readable at 1366×768.
12. At mobile width, cards stack logically without horizontal page overflow.
13. Customization controls do not cover dashboard content.
14. Reduced-motion preference is respected.
15. Screen-reader order follows visual business priority.

## Acceptance
Run with populated, empty, slow, failed and partial API responses.
