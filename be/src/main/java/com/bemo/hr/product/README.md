# Product Operations — Trials, Onboarding, Packs, Risk & Support (تشغيل المنتج)

**EN:** SaaS product-operation domain for running Bemo as a sellable product. Sub-modules: `trial/` tenant trial lifecycle and conversion · `onboarding/` guided first-run readiness checks consumed by the admin setup-readiness screen · `pack/` sellable feature packs mapped to entitlement flags · `subscription/` commercial subscription state · `risk/` tenant risk scoring (usage/health signals) · `support/` support-case intake feeding the frontend support page · `analytics/` product-level usage metrics surfaced in admin product-insights.

**AR:** نطاق تشغيل المنتج لتسيير «بيمو» كمنتج مبيعات. الوحدات الفرعية: دورة الإصدار التجريبي والتحويل، وفحوص جاهزية التهيئة الأولى التي تستهلكها شاشة الجاهزية للإدارة، وحزم الميزات القابلة للبيع المرتبطة بأعلام التمكين، وحالة الاشتراك التجاري، وتقييم مخاطر المستأجر من إشارات الاستخدام والصحة، واستقبال طلبات الدعم، ومؤشرات الاستخدام على مستوى المنتج في لوحة الإدارة.

- Consumers: `fe/src/app/features/admin/product-insights` and `features/admin/setup-readiness`; support page under `features/support`.
- Rule: entitlement/feature-flag evaluation stays authoritative here — vertical setup (`tenant` package) writes flags through these packs rather than ad-hoc updates.
