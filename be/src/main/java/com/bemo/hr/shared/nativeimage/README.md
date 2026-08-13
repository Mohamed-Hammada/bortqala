# Native-image hints / تلميحات native-image

## English

`LiquibaseRuntimeHints` registers Liquibase classes whose YAML changes, preconditions, and extensions are applied reflectively at runtime. This package is used only while Spring AOT builds the desktop native executable; the web/JVM runtime behavior is unchanged.

## العربية

يسجّل `LiquibaseRuntimeHints` فئات Liquibase التي تُطبّق تغييرات YAML وشروطها وامتداداتها بالانعكاس وقت التشغيل. تُستخدم هذه الحزمة فقط أثناء بناء ملف سطح المكتب الأصلي بواسطة Spring AOT، ولا تغيّر سلوك تطبيق الويب/JVM.
