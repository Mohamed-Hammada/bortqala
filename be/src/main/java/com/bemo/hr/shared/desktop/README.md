# Desktop hosting / استضافة سطح المكتب

**EN:** The Gradle resource pipeline builds Angular into Spring Boot static resources. The `desktop` profile binds only to localhost on port `0` and prints `BEMO_BACKEND_PORT=<port>` after startup so Tauri can navigate to the available port. Angular routes are forwarded to `index.html`; API and actuator routes remain separate.

**AR:** يبني Gradle واجهة Angular وينسخها داخل موارد Spring Boot. ملف التشغيل `desktop` يربط بالخادم المحلي فقط ويطلب منفذًا متاحًا تلقائيًا، ثم يطبع `BEMO_BACKEND_PORT=<port>` حتى تفتح Tauri العنوان الصحيح. تُحوّل مسارات Angular إلى `index.html` مع بقاء API وActuator منفصلين.
