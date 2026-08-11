# BEMO ERP UI/UX Phase 3

## العربية

هذه المرحلة تعالج المشاكل التي ظهرت بعد تطبيق أساس نظام التصميم:

- Login RTL: هوية Bemo في بداية اتجاه اللغة، ومبدّل اللغة في الطرف المقابل.
- جميع حقول Login متساوية بصرياً حتى مع Chrome autofill.
- إظهار/إخفاء كلمة المرور أصبح مكوّناً بصرياً واحداً في Login وUsers بدون Emoji.
- صفحة Users تعرض دليل الأدوار ووصف كل دور بدون إعادة جدار الـ 19 بطاقة.
- شرح الدور المختار يظهر مباشرة تحت اختيار الدور في Add/Edit User.
- دليل الأدوار عند فتحه يأخذ عرض الشاشة ولا يترك مساحة فارغة كبيرة بجانبه.
- Tabs موحّدة للميزانيات والضرائب/العملات والمشتريات.
- نوافذ Budgets وTax/Currency انتقلت إلى `app-modal-dialog` ولذلك ESC يعمل.
- حقول هذه النوافذ تستخدم grid ثابت وحالات checkbox منظمة.
- Procurement dark theme يعتمد semantic tokens بدلاً من أسطح Light ثابتة.
- تقرير الحضور يبدأ افتراضياً بتاريخ اليوم لحقلي From/To. تغيير From يعيد To لنفس اليوم، وبعدها يمكن للمستخدم توسيع الفترة.
- تم التحقق من backend فعلياً: نفس اليوم مسموح بالفعل؛ backend يرفض فقط end < start أو أكثر من 366 يوم أو قواعد العمل الأخرى.

## English

This phase fixes the inconsistencies visible after the design-system foundation:

- Correct RTL login header ordering.
- Equal visual treatment for autofilled and non-autofilled login inputs.
- One password visibility pattern across Login and Users.
- User-first `/users` while retaining clear role descriptions.
- Full-width role directory with no large empty side column.
- Shared product tabs in Budgets, Tax/Currency and Procurement.
- Budgets and Tax/Currency migrated from legacy drawers to `app-modal-dialog`, restoring ESC behavior.
- Organized modal field grids and compact toggle rows.
- Procurement dark-theme surfaces now use semantic product tokens.
- Attendance report date selection starts as today → today and keeps End >= Start.
- Backend was inspected: same-day report ranges are already valid; no speculative backend rule was changed.
