# Action Center 2.0 / مركز الإجراءات 2.0

**EN:** Business notifications remain tenant-owned and recipient-specific. Advanced cards add an exception key, localized impact/reason/recommendation, optional monetary impact, internal action route, and target roles. Ranking is deterministic in the backend: base severity + unread boost + a decisive role-match boost. The frontend only renders that order. External action links are rejected by validation and normalized away in the domain.

The original notification fields and endpoints remain compatible. Mark-read authorization still verifies the signed-in recipient, and every send is audited.

**AR:** تظل إشعارات الأعمال مملوكة للمستأجر وموجهة إلى مستخدم محدد. تضيف البطاقة المتقدمة مفتاح الاستثناء والأثر والسبب والتوصية باللغتين وأثرًا ماليًا اختياريًا ومسار إجراء داخليًا والأدوار المستهدفة. الترتيب حتمي في Backend: شدة أساسية ثم تعزيز غير المقروء ثم تعزيز حاسم لتطابق الدور، وتعرض الواجهة هذا الترتيب دون إعادة حسابه. تُرفض روابط الإجراءات الخارجية في التحقق ويزيلها نموذج المجال أيضًا.

تظل حقول ونقاط النهاية القديمة متوافقة، ويستمر التحقق من أن القارئ هو المستلم المسجل مع تدقيق كل عملية إرسال.
