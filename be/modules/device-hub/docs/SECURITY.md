# Security rules

- Place devices on an isolated VLAN.
- Do not expose device management ports directly to the Internet.
- Prefer TLS-enabled platform APIs and vendor-supported secure modes.
- Store secrets in a vault/environment variables, not in Git.
- Use read-only credentials for event ingestion when possible.
- Treat biometric templates as highly sensitive data; avoid centralizing templates unless required.
- Log administrative commands and door actions with actor, target and result.
- Rate-limit webhook and raw passthrough endpoints.
- Disable `/v1/vendors/{vendor}/raw` in production unless needed for diagnostics.
