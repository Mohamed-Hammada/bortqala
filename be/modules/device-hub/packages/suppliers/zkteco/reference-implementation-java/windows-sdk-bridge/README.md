# Windows Vendor SDK Bridge

This process isolates Windows-only ZKTeco vendor runtimes from the Java gateway.

Implemented integration surfaces:

- **zkemkeeper / CZKEM COM**: connect, probe serial/firmware/platform, enumerate users, enumerate attendance.
- **plcommpro.dll**: connect, `GetDeviceData`, `SetDeviceData`, `ControlDevice` for access panels / PULL SDK installations.
- **libzkfp.dll**: ZKFinger SDK initialization and scanner discovery; extend locally for capture/template functions matching your licensed SDK build.

Vendor DLL/OCX/COM files are intentionally **not** redistributed. Install them on the Windows bridge host and ensure the process architecture matches the vendor runtime (x86 vs x64).
