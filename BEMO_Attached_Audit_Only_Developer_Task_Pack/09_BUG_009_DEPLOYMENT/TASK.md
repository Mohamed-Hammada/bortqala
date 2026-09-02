# BUG-009 — Dev-server/lazy-loaded chunk deployment risk

Priority: **HIGH**

Previous deployment failed to load a lazy Angular chunk and exposed development-style @fs filesystem references.

Acceptance:
- [ ] Production build is served.
- [ ] No local filesystem references are exposed.
- [ ] Lazy routes load after hard reload.
- [ ] Deep links work.
- [ ] Blank-screen failure is eliminated.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.