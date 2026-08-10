# Subscription and plan control plane / إدارة الاشتراكات والخطط

**EN:** `subscription_plans` is the data-driven commercial catalog. Plan definitions contain canonical entitlement keys and explicit limits. `tenant_subscriptions` stores the one current tenant lifecycle, while `subscription_changes` is immutable, tenant-scoped, operation-idempotent evidence of every assignment, renewal, downgrade, or cancellation.

Changing a plan locks the tenant, validates dates and optimistic version, applies dependencies before dependents and removes dependents before dependencies. A downgrade changes entitlement rows only: historical ERP data is never deleted. Cancellation disables commercial features, and inactive subscriptions cannot add users. The `users` limit is enforced in the real account-creation transaction. Every control-plane endpoint is explicitly Super Admin-only and every mutation is audited.

**AR:** يمثل جدول `subscription_plans` كتالوج الخطط التجاري المعتمد على البيانات، ويحتوي مفاتيح الاستحقاقات القياسية والحدود الصريحة. يحتفظ `tenant_subscriptions` بحالة الاشتراك الحالية للمستأجر، بينما يسجل `subscription_changes` دليلاً ثابتاً ومعزولاً وآمناً عند تكرار العملية لكل تعيين أو تجديد أو تخفيض أو إلغاء.

يقفل تغيير الخطة المستأجر ويتحقق من التواريخ والإصدار، ثم يفعّل الاعتماديات قبل توابعها ويعطّل التوابع قبل اعتمادياتها. لا يحذف تخفيض الخطة أي بيانات ERP تاريخية، بل يغير الاستحقاقات فقط. يؤدي الإلغاء إلى تعطيل المزايا التجارية، ولا يسمح الاشتراك غير النشط بإضافة مستخدمين. يطبق حد `users` داخل معاملة إنشاء الحساب الحقيقية. جميع نقاط تحكم الخطط محمية صراحةً بدور Super Admin وكل تغيير مسجل في التدقيق.
