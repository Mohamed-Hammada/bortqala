# Database i18n / الترجمة من قاعدة البيانات

**EN:** Public read-only translation bundles for `ar-EG` and `en-US`. A row with `app_id = NULL` is the platform default. A row with the same key/locale and an application id overrides only that application; missing overrides fall back to the default. Authenticated reads derive the application from the JWT tenant context, never from a client-controlled query parameter.

**AR:** حزم ترجمة للواجهة بالعربية والإنجليزية. الصف ذو `app_id = NULL` هو النص الافتراضي للمنصة، ويمكن إضافة صف بالمفتاح واللغة نفسيهما مع رقم تطبيق ليحل محله داخل ذلك التطبيق فقط. عند غياب النص الخاص بالتطبيق يعود النظام تلقائيًا إلى النص الافتراضي. يستخرج الخادم التطبيق من JWT ولا يقبل من العميل تحديد نطاق حزمة القراءة.

**EN:** Super Admins manage defaults and application overrides through `/api/v1/i18n/admin`. Saving in the default scope updates the NULL-app row. “Restore default” deletes only the selected application override. Every upsert/delete is written to the audit log.

**AR:** يدير المدير الشامل النصوص الافتراضية والترجمات الخاصة بالتطبيقات عبر `/api/v1/i18n/admin`. الحفظ في النطاق الافتراضي يعدّل صف التطبيق الفارغ، وزر «العودة للافتراضي» يحذف التخصيص الخاص بالتطبيق فقط. تسجَّل كل عملية إضافة أو تعديل أو حذف في سجل التدقيق.

**EN:** Translation bundles are cached with Spring Cache/Caffeine by locale and tenant. Any admin save or restore evicts the bundle cache. New Liquibase translation CSVs created after V228 omit the technical `id`; the database generates UUIDs. Use `python tools/add-translation.py` to add both locales and `python tools/check-translation-catalog.py` to reject duplicate ids, duplicate key/locale pairs, malformed CSV, or missing bilingual rows before Liquibase runs.

**AR:** تُخزّن حزم الترجمة مؤقتًا باستخدام Spring Cache/Caffeine حسب اللغة والمستأجر، ويُفرّغ الكاش بعد أي حفظ أو استعادة إدارية. ملفات CSV الجديدة بعد V228 لا تحتوي على `id`، بل تولّد قاعدة البيانات UUID تلقائيًا. استخدم `python tools/add-translation.py` لإضافة اللغتين، ثم `python tools/check-translation-catalog.py` لكشف التكرار ونقص أحد النصين قبل Liquibase.
