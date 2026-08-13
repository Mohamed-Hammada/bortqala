# Vendor Artifacts

This project intentionally does not include ZKTeco DLLs, COM components, SDK archives, firmware, license files, or manuals restricted by membership/login.

Recommended deployment layout:

```text
vendor-sdk/
  zkteco/
    standalone-sdk/
    pull-sdk/
    zkfinger-sdk/
    licenses/
```

The whole `vendor-sdk/` directory is ignored by Git.

Before distributing an image or installer containing vendor artifacts:

1. Read the applicable ZKTeco SDK/API license.
2. Confirm whether redistribution is permitted.
3. Pin checksums and version metadata.
4. Scan binaries.
5. Document 32-bit/64-bit and operating-system requirements.
6. Keep a clean-room boundary between original open-source code and proprietary binaries.
