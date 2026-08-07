# Build Tooling — Bemo ERP

Operational notes for building both applications. This file is the E-4 record
(Gradle flakiness on `/mnt/d`, node version pinning). Keep it up to date when
the toolchains change.

## Node (frontend)

- `.nvmrc` at `fe/.nvmrc` pins Node **24** (`24`), matching the `engines` field
  in `fe/package.json` (`">=22.0.0 <25"`).
- CI enforces the range with a `Node version guard` step before install.
- Angular 22 + Vitest require Node >= 22 and < 25; running under Node 21 or 25
  is unsupported.
- Local dev uses `/home/bemo/.nvm/versions/node/v24.18.1/bin` (nvm-managed).
- `npm test` == `ng test` (default watch); CI runs `ng test --watch=false`.
  Direct `npx vitest` fails with `TestBed.initTestEnvironment()` errors — always
  go through the Angular builder.

## Gradle (backend)

- Native Gradle on the WSL `/mnt/d` mount can be flaky (Windows-filesystem
  latency, symlink/gradle-cache issues). When `./gradlew` misbehaves, use the
  rsync mirror at `/tmp/opencode/be-build` (native ext4):
  ```sh
  rsync -a --delete /mnt/d/hamada-bemo-01/be/src/ /tmp/opencode/be-build/src/
  rsync -a /mnt/d/hamada-bemo-01/be/build.gradle /mnt/d/hamada-bemo-01/be/gradle.properties /tmp/opencode/be-build/
  cd /tmp/opencode/be-build && ./gradlew test -PskipDockerTests
  ```
  The mirror is **not a git repo**; copy edited backend files back to
  `/mnt/d/hamada-bemo-01/be/` after verification.
- Local environment has no Docker daemon, so Testcontainers suites
  (`@PostgresIntegrationTest`) cannot run: use `-PskipDockerTests` locally.
  CI provides Docker and runs the full suite.
- `options.release = 17` keeps main/test sources Java-17 compatible; avoid
  Java 21+ collection APIs (`getFirst()`/`getLast()`).

## CI

`.github/workflows/ci.yml`:
- **backend**: temurin 21 + gradle cache → `./gradlew clean test check` →
  `python3 tools/check-test-count.py` (regression gate, baseline 268/56).
- **frontend**: node 24 → node-version guard → `npm ci` → `check:i18n` →
  `check:hardcoded` → `ng test --watch=false` piped through
  `tools/check-test-count.mjs` (baseline 150/28) → `npm run build`.
- **compose**: renders dev + prod compose, asserts private ports unexposed,
  builds the backend image, smoke-tests the container JVM.

### Known CI blocker (RELEASE-HEAD-001)

All branch workflow runs conclude `failure` ~2–4 s after start with `steps: 0`
— the GitHub Actions account/billing runner lock, not a code failure. No commit
can go green until the repo owner resolves the account billing/runner
availability. Full root cause: `docs/BEMO_ERP_PENDING_BUSINESS_TECHNICAL_ROADMAP.md`
A-2 and `docs/TEST_EVIDENCE.md`.
