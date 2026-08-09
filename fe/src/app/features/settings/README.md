# Settings / إعدادات المستخدم

**EN:** Authenticated self-service preferences for light/dark/system theme, comfortable/compact table density, and locale. Values are persisted by the backend per SaaS application and user.

**EN:** Administrators also control the application session timeout from 5 to 10,080 minutes. The saved value is used for newly issued JWT sessions.

**AR:** يمكن لمدير النظام تحديد مدة الجلسة من 5 إلى 10080 دقيقة، وتُستخدم القيمة المحفوظة عند إصدار جلسات JWT الجديدة.

**AR:** تفضيلات ذاتية للمستخدم المسجل تشمل المظهر الفاتح/الداكن/النظام وكثافة الجداول واللغة، وتحفظ في الخادم لكل تطبيق SaaS ومستخدم.

**EN:** Sidebar favorites and recently used visibility, history, favorites, and the configurable recent-item limit are saved with the authenticated user's backend preferences and apply immediately in the shell.

**AR:** تُحفظ إعدادات إظهار المفضلة والمستخدمة حديثاً وسجلها والحد الأقصى للعناصر ضمن تفضيلات المستخدم في الخادم وتُطبق فوراً في القائمة.

**EN:** Administrators can choose automatic locked sequential numbering or unique manual numbering for purchase orders and goods receipts.

**AR:** يستطيع المدير اختيار الترقيم التلقائي المتسلسل والمقفل أو الترقيم اليدوي غير المكرر لأوامر الشراء وأذون الاستلام.

**EN:** The Shortcuts tab is available to every authenticated user and documents general keyboard actions plus direct menu-navigation chords. Actual navigation remains permission-aware.

**EN:** The session settings tab exposes the Admin dashboard-customization policy only to Super Admin. Regular admins retain the stored value but cannot change it.

**AR:** تعرض تبويبة إعدادات الجلسة سياسة تخصيص لوحات المديرين للمدير الشامل فقط، بينما يحتفظ المدير العادي بالقيمة الحالية ولا يستطيع تغييرها.

**AR:** تبويب الاختصارات متاح لكل مستخدم مسجل، ويعرض اختصارات العمليات العامة والانتقال المباشر للقوائم، مع استمرار تطبيق صلاحيات المستخدم على التنقل الفعلي.

## Translation management / إدارة الترجمات

**EN:** Super Admin sees the Arabic tab “إدارة الترجمات”. The view selects a language and either the platform default scope or one application. Save updates that scope; an application override takes priority, while “Restore default” deletes only the override and immediately exposes the platform text. Search covers keys and effective text, and new keys can be added from the same view.

**AR:** يظهر للمدير الشامل فقط تبويب «إدارة الترجمات». يختار المدير اللغة ثم «الترجمة الافتراضية لكل التطبيقات» أو تطبيقًا محددًا. الحفظ يحدّث النطاق المختار، والنص الخاص بالتطبيق له الأولوية. زر «العودة للافتراضي» يحذف تخصيص التطبيق فقط ليظهر النص العام فورًا. يمكن البحث بالمفتاح أو النص وإضافة مفتاح جديد من الشاشة نفسها.
# Attendance anomaly threshold / نسبة شذوذ الحضور

**EN:** Tenant settings expose the percentage used by the backend to create daily biometric outage anomalies.

**AR:** تعرض إعدادات الشركة النسبة التي يستخدمها الخادم لإنشاء حالات شذوذ انقطاع البصمة اليومية.

## Saved-state guard / حارس التغييرات

**EN:** Successful user-preference and application-setting saves mark their reactive forms pristine. Navigation prompts only appear for changes made after the last successful save.

**AR:** بعد نجاح حفظ تفضيلات المستخدم أو إعدادات الشركة تُعلّم النماذج كبيانات محفوظة، لذلك لا يظهر تحذير المغادرة إلا للتغييرات التي تمت بعد آخر حفظ ناجح.
