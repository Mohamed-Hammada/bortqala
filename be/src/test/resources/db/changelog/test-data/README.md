# Test-only Liquibase data

All Liquibase fixtures used only by automated tests belong in this folder and must be included only by `test.changelog-master.yaml`.

Never reference this folder from the production `db.changelog-master.yaml`.

The test master creates the isolated `TEST` tenant before loading sample categories, organization data, currencies, and legacy QA scenarios. Mandatory translations and platform roles remain in the production changelog and are therefore also exercised by tests.

يُنشئ سجل الاختبار شركة `TEST` المعزولة قبل تحميل الفئات وبيانات المؤسسة والعملات وسيناريوهات QA التجريبية. تبقى الترجمات وأدوار النظام الإلزامية في سجل الإنتاج، ولذلك تختبرها الاختبارات أيضاً.
