# Security entitlements / استحقاقات الوحدات

**EN:** `EntitlementCatalog` is the only source for feature defaults, module ownership, dependencies and protected API prefixes. `tenant_features` stores tenant overrides without deleting business data. Super Admin changes require optimistic version and reason; missing dependencies and active dependents are rejected and every change is audited.

**AR:** يمثل `EntitlementCatalog` المصدر الوحيد للقيم الافتراضية وملكية الوحدات والتبعيات ومسارات API المحمية. يخزن `tenant_features` استثناءات كل شركة دون حذف بيانات الأعمال. تتطلب تغييرات Super Admin النسخة وسبباً، وتُرفض التبعيات الناقصة أو التابعة النشطة، ويُدقق كل تغيير.
