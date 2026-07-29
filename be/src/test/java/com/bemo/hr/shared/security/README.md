# SaaS tenant isolation tests / اختبارات عزل الشركات

**EN:** Persists a row for one app and proves Hibernate `@TenantId` hides it from another app in a separate transaction.

**AR:** يحفظ سجلًا لشركة ويتأكد أن Hibernate `@TenantId` يخفيه عن شركة أخرى في transaction منفصلة.

**EN:** `UserPreferenceTests` also verifies synchronized navigation visibility, ordered favorite/recent IDs, validation filtering, and the configured recent-item limit.

**AR:** تتحقق اختبارات `UserPreferenceTests` أيضاً من مزامنة إظهار أقسام التنقل وترتيب معرفات المفضلة والمستخدمة حديثاً وتنقية القيم والحد الأقصى المحدد.
