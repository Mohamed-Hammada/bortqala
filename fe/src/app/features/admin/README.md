# Admin — Product Insights & Setup Readiness (الإدارة — رؤى المنتج وجاهزية التهيئة)

**EN:** Platform-admin surfaces for running Bemo as a product: `product-insights.page.ts` renders product-level usage/analytics from the backend `product/analytics` module (tenants, active usage, feature adoption), and `setup-readiness.page.ts` renders onboarding readiness checks (vertical chosen, categories seeded, devices integrated, opening balances) from the `product/onboarding` module with per-check remediation links. Both are admin-gated routes.

**AR:** شاشتان لإدارة المنصة لتشغيل «بيمو» كمنتج: صفحة رؤى المنتج تعرض مؤشرات الاستخدام على مستوى المنتج من وحدة تحليلات المنتج (المستأجرون، الاستخدام النشط، تبنّي الميزات)، وصفحة جاهزية التهيئة تعرض فحوصات الإعداد الأول (تحديد القطاع، تهيئة الفئات، ربط الأجهزة، الأرصدة الافتتاحية) مع روابط معالجة لكل فحص. كلتا الشاشتين مقيدتان بدور الإدارة.

- Backend counterpart: `com.bemo.hr.product` (analytics/onboarding).
- Rule: these screens never compute product metrics client-side — endpoints only.
