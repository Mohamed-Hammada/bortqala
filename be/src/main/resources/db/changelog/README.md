# Liquibase changelog / سجل تغييرات قاعدة البيانات

**EN:** `db.changelog-master.yaml` is the only schema entry point. Never edit a changeset already applied to a shared database; add a new versioned YAML file with explicit tenant predicates for tenant-owned data. V4 canonicalizes user locales, adds the 30-day translation, and seeds editable DEMO reference categories idempotently.

**AR:** ملف `db.changelog-master.yaml` هو نقطة الدخول الوحيدة لبنية قاعدة البيانات. لا تعدّل changeset طُبّق على قاعدة مشتركة؛ أضف ملف YAML جديدًا مرقمًا، واستخدم شرط `app_id` صريحًا لبيانات الشركات. الإصدار V4 يوحّد صيغة لغة المستخدم، ويضيف ترجمة دورة 30 يوم، ويضيف فئات DEMO المرجعية القابلة للتعديل بدون تكرار.

V5 adds the Arabic and English database translation for the session-expired login notification.

يضيف V5 ترجمة إشعار انتهاء الجلسة بالعربية والإنجليزية إلى قاعدة البيانات.

V6 adds the per-application session timeout and its Arabic/English settings translations. V7 adds the translated invalid-credentials message used separately from session expiry.

يضيف V6 مدة جلسة قابلة للتحكم لكل تطبيق وترجمات إعداداتها بالعربية والإنجليزية. ويضيف V7 رسالة بيانات الدخول غير الصحيحة المترجمة، مستقلة عن رسالة انتهاء الجلسة.
