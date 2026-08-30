# TASK 07 — Device Signing
## Goal
Implement real cryptographic device-bound signing.
## Lifecycle
Register Device → Generate Key Pair → Register Public Key → Challenge → Device Signs → Server Verifies → Sensitive Operation.
## Acceptance Criteria
- [ ] Private key never reaches server.
- [ ] Valid signature succeeds.
- [ ] Invalid signature fails.
- [ ] Modified payload fails.
- [ ] Expired challenge fails.
- [ ] Replay fails.
- [ ] Wrong device/user/tenant fails.
- [ ] Revoked device fails.
- [ ] Device lifecycle audited.
- [ ] Cryptographic + API integration tests exist.
