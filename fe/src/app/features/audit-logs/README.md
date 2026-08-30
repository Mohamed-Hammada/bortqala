# Audit Logs (سجلات التدقيق)

**EN:** Searchable audit trail of user actions: who changed what, when, from where — backed by the audit service recording actor, entity, operation, correlation id and JSON payload. Page renders server-paged results with filters (user, entity, date range) and a dedicated error state (`audit.loadErrorTitle/Hint` + retry) that REPLACES the table — never shows empty-state and error simultaneously (FINAL-002 pattern).

**AR:** سجل تدقيق قابل للبحث لكل إجراءات المستخدمين: من غيّر ماذا ومتى ومن أين — مستند إلى خدمة التدقيق التي تسجل الفاعل والكيان والعملية ومعرف الارتباط والحمولة. تعرض الشاشة نتائج مقسمة صفحةً بصفحة مع مرشحات (مستخدم، كيان، فترة)، وحالة خطأ مخصصة مع زر إعادة المحاولة تحل محل الجدول ولا تظهر معها حالة «لا يوجد» أبداً.

- Backend: audit recording primitives + break-glass trails (V324–V325).
- Spec: `FINAL-002` regression test asserts `.load-error` replaces the table on failure.
