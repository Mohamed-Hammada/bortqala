# Users / المستخدمون والأدوار

**EN:** Admin-only user management with BCrypt-backed passwords and any combination of platform roles.

**AR:** إدارة مستخدمين للمدير فقط، بكلمات مرور BCrypt وإمكانية جمع أكثر من دور لكل مستخدم.

**EN:** Category assignment is loaded live from active employee and worker categories, persisted by the API, and returned for reliable editing. Menu options include organization and audit-log permissions with translated labels.

**EN:** User permissions include a per-account dashboard customization switch. Existing and newly created users default to enabled unless an admin turns it off.

**AR:** تتضمن صلاحيات المستخدم مفتاحاً مستقلاً لتخصيص لوحة المتابعة، ويكون مفعلاً افتراضياً للمستخدمين الحاليين والجدد ما لم يوقفه المدير.

**AR:** يُحمّل تعيين الفئة مباشرة من فئات الموظفين والعمال النشطة ويُحفظ في الخادم ويُعاد عند التعديل. وتشمل خيارات القوائم صلاحيات المؤسسة وسجل التدقيق بتسميات مترجمة.

**EN:** The create/edit user dialog is now simple by default: identity/account fields first, then human-readable role cards without technical role codes, sensitivity tags, guided-role duplication, or page-search noise. For newly created users, allowed menus follow the selected backend role catalog automatically until an administrator explicitly changes a menu. Existing users keep their saved menu configuration when edited.

**AR:** أصبحت نافذة إنشاء وتعديل المستخدم بسيطة بشكل افتراضي: بيانات الحساب أولاً، ثم بطاقات أدوار مفهومة بدون أكواد تقنية أو مستويات حساسية أو تكرار وضع الإرشاد أو البحث بالصفحات. عند إنشاء مستخدم جديد تتبع القوائم المسموحة الأدوار المختارة تلقائياً من كتالوج الصلاحيات في الخادم إلى أن يغيّر المدير صلاحية قائمة يدوياً. أما المستخدمون الحاليون فتظل إعدادات القوائم المحفوظة لديهم كما هي عند التعديل.

**EN:** Menu-by-menu permissions, effective-access preview, role/menu mismatch indicators, sensitive-access warnings, and access-change acknowledgment remain available inside a collapsed advanced-access section. Backend access validation still runs before save, so the UX is simpler without weakening authorization controls.

**AR:** تظل صلاحيات القوائم التفصيلية ومعاينة الصلاحيات الفعلية وتنبيهات تعارض الدور مع القائمة وتحذيرات الصلاحيات الحساسة وسبب الإقرار بالتغيير متاحة داخل قسم صلاحيات متقدمة قابل للفتح. ويستمر التحقق من الصلاحيات في الخادم قبل الحفظ، لذلك تم تبسيط تجربة الاستخدام بدون إضعاف ضوابط الأمان.

## 2026-08-10 user-first UI polish

**EN:** The `/users` page is now user-first rather than role-card-first. The permanent 19-role card wall is replaced with four compact operational metrics, a user search box, the user table, and an optional collapsed role directory. Role technical codes no longer dominate the screen.

**AR:** أصبحت شاشة `/users` موجهة لإدارة المستخدمين أولاً بدلاً من عرض جدار كبير من بطاقات الأدوار. تم استبدال بطاقات الأدوار الدائمة بملخص صغير، وبحث المستخدمين، وجدول المستخدمين، ودليل أدوار اختياري قابل للفتح.

**EN:** The Add/Edit dialog now uses the correct `--modal-large-max-width` variable because the component uses `size="large"`. The primary role moved into the main account grid beside category, account state became a compact row, and Advanced access uses simple dividers instead of nested raised cards.

**AR:** تستخدم نافذة الإضافة والتعديل الآن متغير العرض الصحيح `--modal-large-max-width` لأن النافذة من النوع `large`. تم نقل الدور الأساسي إلى شبكة بيانات الحساب بجوار الفئة، وتبسيط حالة الحساب، وتقليل البطاقات المتداخلة داخل الصلاحيات المتقدمة.
