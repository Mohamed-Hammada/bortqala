# Bemo ERP desktop / تطبيق Bemo ERP لسطح المكتب

## English

The Tauri 2 launcher packages the Angular-in-Spring Boot jar, a `jlink` Java 21+ runtime, and an official PostgreSQL Windows binary distribution. On first launch it silently initializes an app-local PostgreSQL cluster, selects free database and HTTP ports, starts Spring Boot with the `desktop` profile, reads `BEMO_BACKEND_PORT` from stdout, and navigates the webview to that localhost URL. With licence enforcement enabled, PostgreSQL and Spring Boot do not start until the device has a valid licence. Closing the window stops both owned processes.

Set `JAVA_HOME` to JDK 21 or newer, then run `npm install` and `npm run build`. To package with GraalVM, set `GRAALVM_HOME` and run `build-with-graalvm.bat`; this uses GraalVM as the JVM/jlink runtime and does not imply a Spring native-image build. `BEMO_POSTGRES_DISTRIBUTION_DIR` is optional: when omitted, the preparation script downloads EDB's official PostgreSQL 18.4 Windows binary archive into a user-local packaging cache. Sign the generated NSIS installer before external distribution.

The first launch creates random PostgreSQL username/password, JWT secret, and bootstrap admin password in the app-local data directory. The login page obtains only the generated app/user/password through Tauri's localhost-restricted command and prefills it; database and JWT secrets never enter Angular. The NSIS pre-uninstall hook invokes the headless deactivation command. If the online service cannot be reached, uninstall continues with an explicit warning so the operator can release that device from Settings or the license service.

Production licensing requires `BEMO_LICENSE_URL`, `BEMO_LICENSE_PUBLIC_KEY`, and `BEMO_LICENSE_ENFORCED=true` while building the Tauri executable. Build-time values are pinned and cannot be weakened by a client-side environment variable; runtime values are accepted only when that setting was not compiled in for development. The licence service is called when the client enters the key on first activation. Later launches validate the stored server signature, installation id, Windows `MachineGuid`-based device fingerprint, expiry, and clock bounds locally. The normal web application never invokes this licence flow; web access is controlled by enabling or disabling accounts.

## العربية

تجمع Tauri 2 ملف Spring Boot الذي يحتوي واجهة Angular، وبيئة Java 21 أو أحدث مصغرة، وتوزيعة PostgreSQL الرسمية لويندوز. عند أول تشغيل تُنشئ قاعدة محلية بصمت، وتختار منافذ متاحة لقاعدة البيانات والخادم، وتشغّل Spring Boot بملف `desktop`، ثم تقرأ المنفذ الفعلي من stdout وتفتح الواجهة عليه. عند تفعيل إلزام الترخيص لا تبدأ PostgreSQL وSpring Boot قبل قبول ترخيص صالح. عند إغلاق النافذة تتوقف العمليات التي شغّلها التطبيق.

حدد `JAVA_HOME` على JDK 21 أو أحدث ثم شغّل `npm install` و`npm run build`. للتجميع باستخدام GraalVM، حدد `GRAALVM_HOME` ثم شغّل `build-with-graalvm.bat`؛ هذا يستخدم JVM و`jlink` الخاصين بـ GraalVM ولا يبني native image. مسار `BEMO_POSTGRES_DISTRIBUTION_DIR` اختياري؛ عند غيابه تُنزّل أداة التحضير توزيعة PostgreSQL 18.4 الرسمية إلى cache محلي. يجب توقيع مثبت NSIS قبل التوزيع الخارجي.

ينشئ أول تشغيل اسم مستخدم وكلمة مرور PostgreSQL وسر JWT وكلمة مرور المدير عشوائيًا. تحصل شاشة الدخول على بيانات المدير فقط من أمر Tauri مقيد بعنوان localhost، ولا تصل أسرار قاعدة البيانات أو JWT إلى Angular. يحاول مثبت الإزالة تحرير الترخيص تلقائيًا؛ وإذا تعذر الاتصال يعرض تحذيرًا واضحًا لإتمام التحرير من الإعدادات أو خدمة التراخيص.

يتطلب الإنتاج ضبط رابط خدمة الترخيص والمفتاح العام وتفعيل `BEMO_LICENSE_ENFORCED=true` عند بناء ملف Tauri. تبقى قيم البناء مثبتة ولا يمكن للعميل إضعافها بمتغير بيئة. تُستدعى خدمة الترخيص عندما يُدخل العميل المفتاح لأول تفعيل. بعد ذلك يتحقق التطبيق محليًا من توقيع الخادم ومعرف التثبيت وبصمة الجهاز المبنية على Windows `MachineGuid` والانتهاء وحدود الوقت. تطبيق الويب العادي لا يستدعي هذا المسار نهائيًا؛ ويتم التحكم في دخوله بتفعيل الحسابات أو تعطيلها.
