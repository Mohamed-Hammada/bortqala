# Core — Navigation Model (نموذج التنقل)

**EN:** Single source of truth for the sidebar navigation contract (`app-navigation.ts`): the typed `NavItem` list with menu ids, workspace group keys, label/description translation keys, routes, and permission requirements consumed by `app-shell`. Specs here guard the navigation contract (`app-navigation.spec.ts`, `app-navigation-settings-refactor.spec.ts`) so menu registration changes cannot silently break role visibility.

**AR:** المرجع الموحد لعقد القائمة الجانبية: قائمة عناصر التنقل بأنواعها بمعرفات القوائم ومفاتيح مجموعات مساحات العمل ومفاتيح الترجمة والمسارات ومتطلبات الصلاحية، ويستهلكها هيكل التطبيق. الاختبارات هنا تحرس العقد حتى لا تتغير تسجيلات القوائم بصمت وتعطل ظهور الأدوار.

- Adding a page: follow AGENTS.md 4-part protocol; update this list + shell `visible()` + auth gate together.
- Keys referenced must exist in i18n DB (both locales) — enforced by `check:i18n`.
