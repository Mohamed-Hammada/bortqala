# Attendance import infrastructure / بنية استيراد الحضور

**EN:** Spreadsheet imports accept Excel serial dates whether or not the cell has date formatting, numeric `yyyyMMdd`, ISO dates, and common day/month/year separators. Dates are combined with attendance times in the configured company zone, preserving the correct Cairo work day.

**EN:** `HttpBiometricDeviceClient` accepts a JSON array or `{ "punches": [...] }`, maps common user/time aliases, supports ISO timestamps or epoch milliseconds, and sends a `since` cursor after the first successful sync.

**AR:** يقبل استيراد الجداول تاريخ Excel الرقمي سواء كانت الخلية منسقة كتاريخ أم لا، وصيغة `yyyyMMdd` الرقمية، وتاريخ ISO، وفواصل اليوم/الشهر/السنة الشائعة. ويُدمج التاريخ مع وقت الحضور وفق المنطقة الزمنية المضبوطة للشركة للحفاظ على يوم العمل الصحيح في القاهرة.
