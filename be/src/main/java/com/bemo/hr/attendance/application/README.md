# Attendance application / تطبيق الحضور

**EN:** Import orchestration and the replaceable biometric-file reader port. Checksum-based imports are idempotent.

**EN:** `BiometricDeviceSyncService` persists each live sync result, prevents duplicate batches and punches, records audit evidence, and keeps scheduler failures isolated per tenant and device.

**AR:** تنسيق الاستيراد وحد قارئ ملفات البصمة القابل للاستبدال. إعادة نفس الملف آمنة عبر checksum.
