# Architecture

```text
ERP / HR / Payroll / Access application
                  |
          normalized REST API
                  |
+--------------------------------------------------+
| ZKTeco Universal Gateway                        |
| device registry | profile routing | audit       |
| event normalization | idempotency | command bus |
+--------------------------------------------------+
  |          |             |             |
PULL      ADMS/AC PUSH   ZKBio APIs   Windows bridge
TCP/UDP      HTTP          REST       COM / DLL / USB
  |          |             |             |
legacy      iClock,      Time/CV     panels, old SDK,
terminals   F18, face    platforms    ZKFinger scanners
```

Routing rule: identify the *integration route first*, then select a model/firmware parser. Model names are hints, not protocol guarantees.
