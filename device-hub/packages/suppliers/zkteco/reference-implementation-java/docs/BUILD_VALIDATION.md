# Build validation performed for this ZIP

Validated in the generation environment on 2026-08-08:

- OpenJDK 21.0.11 present.
- `core`, `adapter-zk-pull`, `adapter-adms-push`, and `adapter-http-platforms` compiled together with `javac`.
- `ZkProtocolSelfTest` passed (PULL packet encode/decode, TCP wrapper, ZK time codec).
- `AdmsParserSelfTest` passed.
- `tools/zk_probe.py` passed Python bytecode compilation and failure-path smoke testing.
- All Maven POM XML files parsed successfully.
- Device profile CSV contains 22 routing profiles.

Not executed in this environment:

- Full Maven/Spring Boot build (`mvn` is not installed here).
- .NET bridge build (`dotnet` is not installed here).
- Hardware-in-the-loop testing against physical ZKTeco terminals/controllers/scanners.
- Licensed ZKBio API contract tests, because vendor credentials/manuals are deployment-specific.

The hardware and proprietary-runtime items are intentionally handled by the conformance process in `docs/CONFORMANCE.md`.
