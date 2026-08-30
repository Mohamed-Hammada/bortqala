# Verticals Engine (محرك القطاعات)

**EN:** Business-vertical setup engine (`tenant` sibling consumed via `TenantSetupService`). During onboarding or admin reconfiguration the tenant picks its core vertical (`GENERAL`, `MEDICAL`, `CIVIL`, `RETAIL`, `MANUFACTURING`, `SERVICES`); the engine writes active module feature flags and provisions default policy groups with fine-grained permissions per vertical (e.g., MEDICAL → Clinic Administrator / Medical Receptionist). Vertical inference from active flags powers round-trips.

**AR:** محرك إعداد قطاع النشاط: أثناء التهيئة أو إعادة الضبط يختار المستأجر قطاعه (عام، طبي، مقاولات، تجزئة، تصنيع، خدمات)؛ فيكتب المحرك أعلام تمكين الوحدات النشطة ويهيّئ مجموعات صلاحيات افتراضية دقيقة لكل قطاع (مثال: الطبي → مسؤول العيادة / موظف استقبال)، مع استنتاج القطاع من الأعلام المفعلة.

- Key files: `domain/BusinessVertical.java`, `application/TenantSetupService.java` (`configureTenantVertical`, feature maps, `DefaultGroupSpec` catalogs), `api/TenantSetupController` (`/api/v1/tenant/vertical-setup`).
- Frontend: Settings → Business vertical setup (`fe/src/app/features/settings/business-vertical-setup`) and standalone `features/verticals`.
- Roadmap: full role-template catalogs per vertical are WP-10 in the implementation guide; medical/hospital packs §14–§15.
