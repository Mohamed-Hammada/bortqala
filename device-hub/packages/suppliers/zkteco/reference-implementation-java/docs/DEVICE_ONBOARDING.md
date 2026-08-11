# Device Onboarding and Conformance

A device is onboarded by exact tuple:

```text
model + device name + platform + firmware + algorithm versions + protocol + region
```

## Required evidence

- product label and exact model
- serial number redacted in public fixtures
- firmware version
- platform value
- fingerprint/face/palm algorithm versions when relevant
- enabled communication mode
- configured port and communication password behavior
- official SDK/software compatibility listed for the model
- sample user, attendance, operation, and realtime payloads
- timezone and daylight-saving behavior

## Minimum read-only conformance suite

1. Connect or receive heartbeat.
2. Read stable identity twice.
3. Read empty and non-empty user datasets.
4. Read attendance without deleting records.
5. Repeat reads and prove idempotency.
6. Test non-ASCII names.
7. Test clock skew and timezone conversion.
8. Test network interruption and reconnect.
9. Test maximum supported identifier length.
10. Verify no biometric template is logged.

## Write-operation gates

Write operations remain disabled until:

- a complete backup/restore path is proven
- device capacity is checked before writes
- rollback behavior is documented
- duplicate user/card/PIN handling is tested
- admin-lockout scenarios are tested
- destructive commands require an explicit feature flag

## Fixture layout

```text
fixtures/<protocol>/<model>/<firmware>/
  identity.json
  capabilities.json
  raw/
  expected/
  notes.md
```

Never commit real biometric templates, passwords, API tokens, or personally identifiable production data.
