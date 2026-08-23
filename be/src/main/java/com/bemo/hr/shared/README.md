# Shared Primitives (المشتركات)

**EN:** Cross-cutting infrastructure owned centrally so features stay thin. Sub-packages:

- `api/` — `ApiExceptionHandler`: RFC-9457 problem details; resolves `BusinessRuleException`/`NotFoundException` codes through DB translation tables (ar-EG/en-US) with constructor-message fallback. Never hard-code Arabic in services.
- `dataexchange/` — generic import/export exchange endpoints (`DataExchangeController`) for interoperability.
- `dto/` — shared API DTO primitives (paging, envelopes).
- `idempotency/` — `operationId` replay protection used by settlements, imports, payments: a repeated operation id executes exactly once.
- `job/` — background job primitives (scheduling wrappers, run evidence).
- `numbering/` — unified document numbering (`FIN-002`, V118): per-tenant, gap-tolerant sequences with year scoping for journals/invoices/payments.
- `sampletemplate/` — downloadable sample templates endpoint powering structured-import screens.
- `shortcut/` — permission-aware user screen shortcuts (`UserShortcutProfile`, `UserScreenShortcut`, `/api/v1/auth/preferences/shortcuts`, UX-SHORTCUTS-001, V114–V115).

**AR:** بنية تحتية مشتركة تُدار مركزياً لتبقى الميزات خفيفة. الحزم الفرعية: معالج الاستثناءات بترجمة أكواد الأعمال من قاعدة البيانات بالعربية والإنجليزية، ونقاط تبادل البيانات، وأساسيات كائنات النقل، وحماية التكرار عبر `operationId` للتسويات والاستيرادات والمدفوعات، وأساسيات المهام الخلفية، والترقيم الموحد للمستندات، وقوالب الاستيراد الجاهزة، واختصارات الشاشات الواعية بالصلاحيات لكل مستخدم.

- Rule: anything two features need goes here AFTER the second consumer exists — no speculative abstractions.
