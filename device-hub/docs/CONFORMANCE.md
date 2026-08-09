# Hardware/Firmware conformance process

For every production model + firmware:

1. Record vendor, exact model, serial prefix, firmware, hardware revision, region and enabled license.
2. Probe the preferred integration route.
3. Verify device identity and clock.
4. Read users without modifying data.
5. Generate a test punch/access event and verify exact timestamps and person mapping.
6. Test incremental event retrieval and deduplication.
7. If allowed, add/update/delete one disposable test person.
8. If access control is enabled, test door unlock/lock on a non-critical test door.
9. Reboot the integration service and verify cursor recovery/no duplicate punches.
10. Mark the matrix row `VERIFIED` with date and notes.

Never mark a whole marketing family as verified based on one firmware branch.
