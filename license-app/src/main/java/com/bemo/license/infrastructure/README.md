# License persistence / تخزين التراخيص

## English

Repositories store only SHA-256 license-key hashes and public device identity. Seat-changing activation uses a pessimistic write lock on `LicenseKey` so concurrent desktop activations cannot exceed the configured limit. The server signing private key remains in environment-backed secret storage.

## العربية

تحفظ المستودعات بصمة SHA-256 لمفتاح الترخيص وهوية الجهاز العامة فقط. يستخدم تخصيص مقعد التفعيل قفل كتابة على `LicenseKey` لمنع تجاوز الحد عند تفعيل أجهزة متزامنة. يبقى مفتاح توقيع الخادم الخاص في مخزن أسرار يوفره التشغيل.
