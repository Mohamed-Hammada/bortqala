# Supplier and contractor risk scores / درجات مخاطر الموردين والمقاولين

**EN:** Scores are deterministic and explainable. Supplier weights cover lifecycle (30), mandatory current/verified documents (25), verified bank (20), profile completeness (15), and declared risk (10). Contractor weights cover lifecycle (25), active-worker coverage (25), paid-settlement performance (25), settlement issue quality (15), and profile completeness (10). Every component returns score/max/status and a remediation route.

Risk thresholds are tenant-owned, versioned, strictly descending, and audited. Refresh locks the tenant, persists immutable per-subject snapshots, and is operation-idempotent. Repository reads and snapshots retain Hibernate tenant isolation.

**AR:** الدرجات حتمية وقابلة للتفسير. أوزان المورد: دورة الحياة 30، المستندات الإلزامية السارية والموثقة 25، البنك الموثق 20، اكتمال الملف 15، والمخاطر المعلنة 10. أوزان المقاول: دورة الحياة 25، تغطية العمال النشطين 25، أداء التسويات المدفوعة 25، جودة مشكلات التسوية 15، واكتمال الملف 10. يعيد كل مكوّن الدرجة والحد والحالة ومسار المعالجة.

حدود المخاطر مملوكة للمستأجر وذات إصدار وتنازلية بصرامة ومدققة. يقفل التحديث المستأجر ويحفظ لقطة ثابتة لكل شريك ويكون آمنًا عند إعادة `operationId`، مع استمرار عزل Hibernate لكل القراءات واللقطات.
