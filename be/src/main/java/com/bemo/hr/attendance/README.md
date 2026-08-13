# Attendance capability / الحضور والبصمة

**EN:** Owns immutable biometric imports, identity matching, punches, and attendance calculation inputs.

**EN:** Live biometric devices are tenant-owned connections. They support immediate or scheduled IP/HTTP JSON synchronization, durable success/failure state, checksum and punch-level duplicate prevention, employee identity matching, and audit evidence.

**EN:** File imports accept CSV, XLS/XLSX, and ZKTeco-style Access MDB/ACCDB backups. Access parsing is pure Java and maps `USERINFO.Badgenumber` to `CHECKINOUT.CHECKTIME`, so it works in Linux containers without Microsoft Access drivers. The UI offers a parser-compatible CSV template.

**AR:** يقبل الاستيراد ملفات CSV وXLS/XLSX ونسخ Access بصيغة MDB/ACCDB الشائعة في أجهزة ZKTeco، مع ربط رقم البطاقة في `USERINFO` ببصمات `CHECKINOUT`. ويوفر التطبيق قالب CSV متوافقاً مع المستورد.

**AR:** مسؤولة عن استيراد البصمة غير القابل للتعديل ومطابقة الهوية والبصمات ومدخلات حساب الحضور.
