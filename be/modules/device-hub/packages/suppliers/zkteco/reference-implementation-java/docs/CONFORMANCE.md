# Model + firmware conformance

A device becomes VERIFIED only after these tests pass on the exact model/firmware:

1. Read identity: serial, model/platform, firmware.
2. Read/set device time and verify round-trip.
3. Read users without corruption; verify non-ASCII names if supported.
4. Read attendance/access events and compare with device UI for at least 20 records.
5. Real-time/PUSH test if advertised; verify retry/idempotency on disconnect.
6. User create/update/delete using a sacrificial test user.
7. Template/card operations only on a test identity and only when SDK licensing permits.
8. Door output command only on a bench controller, never on a live production door during automated tests.
9. Reboot/reconnect and confirm no duplicate event ingestion.
10. Record transport, firmware, record format, timezone and known limitations in the compatibility matrix.

Never call an entire marketing family VERIFIED based on one firmware build.
