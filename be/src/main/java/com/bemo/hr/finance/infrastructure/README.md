# Finance infrastructure / بنية المالية

**EN:** `FrankfurterExchangeRateClient` uses Spring `RestClient` over the JDK HTTP client with bounded connect/read timeouts. It first obtains the provider's supported currency catalogue and then requests only the relevant latest rates. The base URL can be overridden for tests/self-hosting with `hr.exchange-rate.frankfurter-base-url`.

**AR:** يستخدم `FrankfurterExchangeRateClient` عميل `RestClient` مع مهلات اتصال وقراءة محددة. يتم أولاً جلب قائمة العملات المدعومة من المصدر ثم طلب الأسعار المطلوبة فقط. يمكن تغيير رابط المصدر للاختبارات أو الاستضافة الذاتية عبر `hr.exchange-rate.frankfurter-base-url`.
