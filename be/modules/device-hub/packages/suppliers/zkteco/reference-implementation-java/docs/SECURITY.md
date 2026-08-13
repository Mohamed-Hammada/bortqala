# Security

- Put gateway and devices on a private management VLAN/VPN; do not expose port 4370 directly to the Internet.
- Prefer HTTPS for ADMS/PUSH where the device firmware supports it, and place older HTTP-only devices behind a site-to-site tunnel/reverse proxy boundary.
- Store Comm Keys/API tokens in secrets, not device profile CSV or Git.
- Redact fingerprints, faces, palm templates, passwords and real attendance fixtures from logs.
- Disable raw/admin endpoints from untrusted networks.
- Keep Windows vendor bridge local to the site and firewall it to the gateway only.
- Treat biometric templates as sensitive personal data and apply applicable Egyptian privacy/data-protection requirements plus your organization policy.
