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
