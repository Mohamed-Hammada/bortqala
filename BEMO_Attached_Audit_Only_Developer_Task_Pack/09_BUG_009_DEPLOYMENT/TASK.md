# BUG-009 — Dev-server/lazy-loaded chunk deployment risk

Priority: **HIGH**

Previous deployment failed to load a lazy Angular chunk and exposed development-style @fs filesystem references.

Acceptance:
- [x] Production build is served.
- [x] No local filesystem references are exposed.
- [x] Lazy routes load after hard reload.
- [x] Deep links work.
- [x] Blank-screen failure is eliminated.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.