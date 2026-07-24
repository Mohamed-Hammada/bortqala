# Backend setup scripts / أدوات إعداد الخادم

**EN:** `CreatePostgresDatabase.java` idempotently creates one validated, lower-case UTF-8 PostgreSQL database. It never drops or replaces a database and receives credentials only as command arguments.

**AR:** تنشئ أداة `CreatePostgresDatabase.java` قاعدة PostgreSQL واحدة بترميز UTF-8 بعد التحقق الصارم من الاسم. الأداة لا تحذف ولا تستبدل أي قاعدة، وتأخذ بيانات الدخول من arguments فقط.
