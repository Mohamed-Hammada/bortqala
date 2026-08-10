# Business parties / جهات التعامل

**EN:** Tenant-scoped CRUD for suppliers, processing customers, export customers, farms, and future custom party types. Parties are deliberately separate from HR employees. Codes are unique per SaaS application; mutable records retain `created_at` and `updated_at` and use optimistic locking.

**AR:** إدارة جهات التعامل لكل شركة، مثل الموردين وعملاء التشغيل وعملاء التصدير والمزارع، مع السماح بأنواع جديدة مستقبلًا. جهات التعامل منفصلة عن موظفي الموارد البشرية، والكود فريد داخل التطبيق، مع تواريخ الإنشاء والتعديل وقفل تفاؤلي للتحديث.

## Supplier onboarding / تأهيل الموردين

**EN:** Suppliers use the controlled `REQUESTED → UNDER_REVIEW → APPROVED → ACTIVE` lifecycle, with suspension and blacklist controls. The module stores real compliance files (5 MB allowlisted formats), verifies mandatory-document validity, detects duplicate tax IDs and IBANs, supports the shared approval workflow, and exposes an auditable Supplier 360 view. Procurement accepts only active suppliers and payments additionally require a verified primary bank account.

**AR:** يمر المورد بدورة مضبوطة `REQUESTED → UNDER_REVIEW → APPROVED → ACTIVE` مع إمكان التعليق والإدراج في القائمة السوداء. تحفظ الوحدة ملفات الامتثال الفعلية (حتى 5 ميجابايت وبأنواع مسموحة)، وتتحقق من صلاحية المستندات الإلزامية، وتكشف تكرار الرقم الضريبي وIBAN، وتتكامل مع مسار الاعتماد المشترك، وتعرض ملف مورد شامل قابلًا للتدقيق. لا تقبل المشتريات إلا المورد النشط، كما تتطلب المدفوعات حسابًا بنكيًا أساسيًا متحققًا منه.
