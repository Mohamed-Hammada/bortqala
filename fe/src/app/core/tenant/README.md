# Core — Tenant Context (سياق المستأجر)

**EN:** Frontend tenant awareness: holds the active SaaS app identity (app code/id from the session), exposes it to services that need explicit tenant context beyond the JWT-scoped defaults, and reacts to account switches so cached tenant-scoped state (menus, preferences, shortcuts) is invalidated. Mirrors backend `TenantContext` semantics — one source of truth on the auth session.

**AR:** وعي الواجهة بالمستأجر: تحمل هوية التطبيق النشط (كود/معرف من الجلسة)، وتوفرها للخدمات التي تحتاج سياقاً صريحاً خارج افتراضيات التوكن، وتتفاعل مع تبديل الحسابات بإبطال الحالات المخزنة الخاصة بالمستأجر (القوائم، التفضيلات، الاختصارات). تقابل دلالات `TenantContext` في الخادم ومصدر الحقيقة هو جلسة المصادقة.

- Rule: features never read tenant id directly from storage — always through this service.
