# BEMO ERP Product UI/UX Standard

## الهدف

هذه الوثيقة تحول الواجهة من مجموعة شاشات لها CSS منفصل إلى **منتج واحد له هوية واحدة**.

القاعدة الأساسية:

> Simple by default. Advanced on demand.

لا نضيف خيارات أو معلومات تقنية في المسار الطبيعي للمستخدم إلا إذا كانت مطلوبة لإنجاز المهمة.

## الهوية البصرية

- الواجهة التشغيلية: هادئة، واضحة، كثيفة بالمعلومات بدون ازدحام.
- الألوان الأساسية: Ink / Surface / Gold الحالية، مع استخدام Gold كـ accent وليس كخلفية لكل شيء.
- لا يتم إنشاء لون جديد داخل صفحة Feature إذا كان له معنى موجود في tokens.
- Light / Dark / System يجب أن تستخدم نفس semantic tokens.
- الخطوط الحالية El Messiri للعناوين وTajawal للنصوص تبقى الأساس.

## نظام المسافات

استخدم فقط:

- `--space-1`: 4px
- `--space-2`: 8px
- `--space-3`: 12px
- `--space-4`: 16px
- `--space-5`: 20px
- `--space-6`: 24px
- `--space-8`: 32px

لا تضف `margin-left/right` عشوائي للأزرار. ضع الأزرار داخل Action Group واستخدم `gap`.

## الأزرار

- Primary: الإجراء الرئيسي فقط، غالباً واحد في المنطقة.
- Secondary: الإجراءات المساعدة.
- Ghost: الإجراءات الخفيفة.
- Danger: حذف/إلغاء خطير فقط.
- Icon button: إجراءات معروفة وقصيرة مع `aria-label` وTooltip.

كل الأزرار تستخدم ارتفاعاً موحداً ومسافة موحدة بين النص والأيقونة.

## النماذج

المسار الطبيعي يجب أن يعرض فقط الحقول التي يحتاجها المستخدم فعلاً.

### Add User

الواجهة الافتراضية:

1. الاسم
2. اسم المستخدم
3. كلمة المرور
4. الفئة (اختياري)
5. حالة الحساب
6. الدور الرئيسي
7. حفظ / إلغاء

داخل **Advanced** فقط:

- أدوار إضافية
- صلاحية عرض الراتب
- تخصيص لوحة المتابعة
- صلاحيات القوائم الدقيقة
- Effective Access / diff / warnings

كلمة المرور واسم المستخدم التقني يعرضان LTR حتى داخل التطبيق العربي.

## Dialogs and errors

- كل dialog مركزي وله Header / scrollable Body / Footer.
- Toast layer دائماً أعلى من modal layer.
- خطأ الحفظ داخل dialog يظهر داخل نفس dialog ويظهر Toast كذلك.
- لا نعرض رسالة خطأ خلف backdrop.

## Sidebar

- زر collapse/expand يوضح الحالة وليس Hamburger ثابت في الحالتين.
- فتح/غلق جميع مجموعات القائمة: أيقونات فقط + Tooltip + aria-label.
- لا نستخدم Emoji كأيقونات تحكم أساسية عندما يوجد `app-icon`.

## Dashboard

- ترتيب Widgets يجب أن يطابق ما يراه المستخدم.
- زر Up غير فعال لأول عنصر، وDown غير فعال لآخر عنصر.
- أزرار التحريك Icon-only.
- Categories وRecent Imports يجب أن يشاركا فعلياً في ترتيب الصفحة بشكل مستقل.

## RTL / LTR

- استخدم `margin-inline-*`, `padding-inline-*`, `inset-inline-*`.
- Password / username / codes / IDs يمكن أن تكون `direction:ltr`.
- لا تثبت `left/right` إلا عندما يكون المعنى فعلاً فيزيائياً وليس اتجاهياً.

## طبقات Z-index

- Content
- Sticky
- Sidebar
- Overlay
- Modal
- Toast
- Tooltip

القيم موجودة في `_bemo-product-system.scss`. لا يتم اختراع z-index جديد داخل Feature.

## قاعدة التطوير

قبل إضافة SCSS إلى صفحة جديدة، اسأل:

1. هل هذا Token؟
2. هل هذا Component مشترك؟
3. هل هذا Pattern مستخدم في أكثر من شاشة؟
4. أم أنه فعلاً خاص بهذه الصفحة؟

إذا كان 1-3، لا يوضع داخل feature stylesheet.
