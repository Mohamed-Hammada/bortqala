# Request observability / تتبع الطلبات

**EN:** Generates an independent server correlation id for every request, preserves a safe client correlation id, records device/user/IP/roles/status/duration in MDC, and emits Logstash JSON. Tokens, passwords, query strings, and bodies are never logged.

**AR:** ينشئ correlation id مستقلًا من الخادم لكل طلب ويحفظ رقم العميل الآمن ويسجل الجهاز والمستخدم وIP والأدوار والحالة والمدة داخل MDC بصيغة Logstash JSON، دون تسجيل token أو كلمة مرور أو query أو body.
