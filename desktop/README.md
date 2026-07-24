# Bemo HR desktop / تطبيق Bemo HR لسطح المكتب

## English

The Tauri 2 launcher packages the Angular-in-Spring Boot jar, a `jlink` Java 26 runtime, and an official PostgreSQL Windows binary distribution. On first launch it silently initializes an app-local PostgreSQL cluster, selects free database and HTTP ports, starts Spring Boot with the `desktop` profile, reads `BEMO_BACKEND_PORT` from stdout, and navigates the webview to that localhost URL. Closing the window stops both owned processes.

Set `JAVA_HOME` and `BEMO_POSTGRES_DISTRIBUTION_DIR`, then run `npm install` and `npm run build`. The PostgreSQL path must point to a redistributable official Windows binary distribution containing `bin`, `lib`, and `share`; the preparation script deliberately refuses an unknown or incomplete source. Sign the generated NSIS installer before external distribution.

## العربية

تجمع Tauri 2 ملف Spring Boot الذي يحتوي واجهة Angular، وبيئة Java 26 مصغرة، وتوزيعة PostgreSQL الرسمية لويندوز. عند أول تشغيل تُنشئ قاعدة محلية بصمت، وتختار منافذ متاحة لقاعدة البيانات والخادم، وتشغّل Spring Boot بملف `desktop`، ثم تقرأ المنفذ الفعلي من stdout وتفتح الواجهة عليه. عند إغلاق النافذة تتوقف العمليات التي شغّلها التطبيق.

حدد `JAVA_HOME` و`BEMO_POSTGRES_DISTRIBUTION_DIR` ثم شغّل `npm install` و`npm run build`. يجب أن يشير مسار PostgreSQL إلى توزيعة ويندوز رسمية قابلة لإعادة التوزيع وتحتوي `bin` و`lib` و`share`. يجب توقيع مثبت NSIS قبل التوزيع الخارجي.
