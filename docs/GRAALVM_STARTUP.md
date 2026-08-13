# GraalVM startup / التشغيل باستخدام GraalVM

## English

- Set `GRAALVM_HOME` to a GraalVM JDK 21 or newer.
- Run `start-backend-graalvm.bat` on Windows or `start-backend-graalvm.sh` on Unix-like systems. Existing `SPRING_PROFILES_ACTIVE`, database, bootstrap, and secret environment variables are preserved; the profile defaults to `dev` only when unset.
- Run `desktop/build-with-graalvm.bat` to package the desktop app with GraalVM's JVM and `jlink` runtime.
- Run `build-desktop-native.bat` from the repository root to produce a real Spring AOT native executable and its Tauri/NSIS installer. It auto-detects the supplied GraalVM 25.0.4 archive under Downloads and requires Visual Studio Build Tools with the C++ workload.

The startup and `build-with-graalvm.bat` scripts run the standard Spring Boot jar on the GraalVM JVM. `build-desktop-native.bat` is the separate native-image path: its client package contains `bemo-erp.exe` and native DLL sidecars, but no Java runtime or backend JAR.

The scripts also override `org.gradle.java.home`, so a user-level Gradle setting cannot silently move the build daemon back to another JDK.

## العربية

- حدد `GRAALVM_HOME` على GraalVM JDK 21 أو أحدث.
- شغّل `start-backend-graalvm.bat` على Windows أو `start-backend-graalvm.sh` على الأنظمة الشبيهة بـ Unix. تحافظ الأدوات على متغيرات ملف التشغيل وقاعدة البيانات والأسرار، وتستخدم `dev` فقط عند عدم تحديد ملف.
- شغّل `desktop/build-with-graalvm.bat` لتجميع تطبيق سطح المكتب باستخدام JVM و`jlink` من GraalVM.
- شغّل `build-desktop-native.bat` من جذر المشروع لبناء ملف Spring AOT أصلي ومثبت Tauri/NSIS. تكتشف الأداة ملف GraalVM 25.0.4 الموجود في Downloads تلقائيًا، وتتطلب Visual Studio Build Tools مع أدوات C++.

تشغّل أدوات البدء و`build-with-graalvm.bat` ملف Spring Boot العادي على JVM. أما `build-desktop-native.bat` فهو مسار native-image الفعلي: تحتوي حزمة العميل على `bemo-erp.exe` وملفات DLL الأصلية المطلوبة، من دون Java runtime أو backend JAR.

تتجاوز الأدوات أيضًا إعداد `org.gradle.java.home` الخاص بالمستخدم، لكي لا يعود Gradle بصمت إلى JDK آخر.
