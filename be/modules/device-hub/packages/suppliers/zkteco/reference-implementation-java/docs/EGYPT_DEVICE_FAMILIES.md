# Egypt / North Africa coverage seed

The repository ships model-family presets for deployments commonly encountered through ZKTeco Egypt/North Africa channels: K-series, LX50, MB20/MB360, IN-series, iClock, F18/F-series, uFace, and newer SpeedFace/SenseFace/ProFace families.

Important examples:

- **K40** — TCP/IP and USB-host attendance terminal; probe classic PULL first.
- **iClock700 / iClock680** — official Egypt pages describe ADMS and former SDK compatibility; try ADMS/TA PUSH when Cloud Server is configured, otherwise PULL/SDK.
- **F18** — official Egypt page explicitly lists AC PUSH, TA PUSH, and Standalone SDK as firmware/software alternatives. This is exactly why model name alone is insufficient.
- **MB20** — local North Africa listing identifies compatibility with older ZKTime/ZKAccess generations, so keep legacy SDK/PULL support enabled.
- **uFace 402/602/802** — older multi-biometric family; profile uses auto detection because deployed firmware varies.

`profiles/device-profiles.csv` is a *routing seed*, not a certification claim. Use real serial/model/firmware samples to promote a row to VERIFIED.
