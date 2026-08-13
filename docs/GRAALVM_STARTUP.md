# GraalVM startup / التشغيل باستخدام GraalVM

## English

- Set `GRAALVM_HOME` to a GraalVM JDK 21 or newer.
- Run `start-backend-graalvm.bat` on Windows or `start-backend-graalvm.sh` on Unix-like systems. Existing `SPRING_PROFILES_ACTIVE`, database, bootstrap, and secret environment variables are preserved; the profile defaults to `dev` only when unset.
- Run `desktop/build-with-graalvm.bat` to package the desktop app with GraalVM's JVM and `jlink` runtime.

These scripts run the standard Spring Boot jar on the GraalVM JVM. They intentionally do not claim to create a GraalVM native image; native-image support needs a separate Spring AOT compatibility effort and dependency audit.

## العربية

- حدد `GRAALVM_HOME` على GraalVM JDK 21 أو أحدث.
- شغّل `start-backend-graalvm.bat` على Windows أو `start-backend-graalvm.sh` على الأنظمة الشبيهة بـ Unix. تحافظ الأدوات على متغيرات ملف التشغيل وقاعدة البيانات والأسرار، وتستخدم `dev` فقط عند عدم تحديد ملف.
- شغّل `desktop/build-with-graalvm.bat` لتجميع تطبيق سطح المكتب باستخدام JVM و`jlink` من GraalVM.

تشغّل هذه الأدوات ملف Spring Boot العادي على GraalVM JVM، ولا تبني native image. يتطلب native image عملًا منفصلًا لمراجعة Spring AOT والاعتماديات.
