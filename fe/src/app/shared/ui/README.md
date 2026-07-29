# Shared UI / واجهة المستخدم المشتركة

**EN:** Reusable presentational controls used by multiple features. `ModalDialogComponent` supports both mounted state through `[isOpen]` and structurally-created dialogs that exist only while open. It emits both `close` and the compatibility alias `closeModal`; all rendered dialogs are centered, modal, keyboard-dismissible application dialogs.

**AR:** عناصر عرض مشتركة تستخدمها عدة شاشات. يدعم `ModalDialogComponent` التحكم المستمر عبر `[isOpen]` وكذلك إنشاء المكوّن فقط أثناء الفتح، ويصدر الحدثين `close` و`closeModal` للتوافق. تظهر كل النوافذ في وسط الصفحة كنوافذ تطبيق ويمكن إغلاقها بلوحة المفاتيح.

**EN:** Dialogs focus the first available control on open, preserve native Tab order, and close with Escape. Forms inside the authenticated shell submit from ordinary inputs with Enter while text areas and selects retain their native keyboard behavior.

**AR:** تضع الحوارات التركيز على أول حقل متاح عند الفتح، وتحافظ على ترتيب Tab الطبيعي، وتغلق بزر Escape. كما تُرسل النماذج داخل التطبيق بزر Enter من حقول الإدخال العادية مع احتفاظ مربعات النص والقوائم بسلوك لوحة المفاتيح الطبيعي.

## Application tooltip / تلميح التطبيق

**EN:** `AppTooltipDirective` renders a styled accessible overlay on hover and keyboard focus, removes it on blur, click, scroll, resize, or Escape, and keeps `aria-describedby` synchronized.

**AR:** يعرض `AppTooltipDirective` تلميحاً مرئياً منسقاً عند المرور أو تركيز لوحة المفاتيح، ويغلقه عند فقد التركيز أو النقر أو التمرير أو تغيير الحجم أو Escape، مع ربطه بعنصر التحكم عبر `aria-describedby`.
