# Tenant Business Vertical Setup & Module Provisioning (تهيئة القطاع التجاري والموديولات)

**EN:** Dynamic company business vertical setup engine. During onboarding or administrative reconfiguration, tenant administrators can select their company's core vertical (`GENERAL`, `MEDICAL`, `CIVIL`, `RETAIL`, `MANUFACTURING`, `SERVICES`). The backend automatically configures active module feature flags and provisions standard custom policy groups with fine-grained permissions.

**AR:** محرك تهيئة وتخصيص قطاع النشاط للشركات والمؤسسات. يتيح للمدير اختيار قطاع العمل الأساسي للمنشأة (متعدد الأنشطة، مراكز طبية، مقاولات وهندسة مدنية، تجارة وتجزئة، تصنيع وإنتاج، خدمات مهنية)، ويقوم الخادم تلقائياً بتفعيل الموديولات المتوافقة وإنشاء مجموعات الصلاحيات الافتراضية المناسبة لنشاط المنشأة.

## Key Components
- `domain/BusinessVertical.java`: Supported business verticals enum.
- `application/TenantSetupService.java`: Business vertical orchestration, feature activation mapping, and policy group provisioning.
- `api/TenantSetupController.java`: Endpoints at `/api/v1/tenant/vertical-setup`.
