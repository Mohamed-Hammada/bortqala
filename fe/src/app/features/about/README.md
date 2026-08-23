# About / System Info (حول النظام)

**EN:** System about screen: shows application version/build metadata from `SystemAboutController`, current tenant/license status (offline licensing state from platform deployment), database-backed system-health probes summary, and support contact hints. Read-only for all signed-in users; admin-only sections gated by role checks.

**AR:** شاشة «حول النظام»: تعرض بيانات الإصدار والبناء من نقطة النظام، وحالة المستأجر والترخيص الحالي من محرك الترخيص دون اتصال، وملخص فحوصات صحة النظام المخزنة، وقنوات الدعم. للقراءة فقط لكل المستخدمين، والأقسام الإدارية مقيدة بالأدوار.

- Data sources: `/api/v1/system/about` + licensing status endpoints.
- Copy via i18n DB keys; no hardcoded strings (E-1 scanner gate).
