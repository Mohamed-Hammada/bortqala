# Fiscal periods / الفترات المالية

**EN:** The page loads a selected fiscal year without creating data as a side effect. An empty API response (and legacy `204`/`404` empty responses) renders the guided empty state and the explicit “create year” action; only genuine server failures render an error.

**AR:** تعرض الصفحة السنة المالية المختارة دون إنشاء بيانات تلقائياً. وتُعرض حالة فارغة إرشادية مع زر إنشاء فترات السنة عند رجوع قائمة فارغة (وكذلك استجابات `204` أو `404` القديمة)، بينما تُعرض رسالة الخطأ فقط عند وجود فشل حقيقي في الخادم.

## 2026-08-10 — Header control alignment

**EN:** Fiscal year and Create Year controls are bottom-aligned with equal product control height; the extra hard-coded lightning prefix was removed.

**AR:** تمت محاذاة حقل السنة المالية وزر إنشاء الفترات على خط واحد وبنفس ارتفاع عناصر التحكم، مع إزالة رمز البرق المضاف يدوياً لتجنب التكرار.
