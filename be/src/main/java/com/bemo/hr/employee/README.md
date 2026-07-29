# Employee capability / الموظفون والفئات

**EN:** Owns attendance categories, effective schedules, employees, employment type, device identity assignment, pay-cycle configuration, and category advance eligibility. Employee codes use the category prefix; a user suffix is normalized, while a blank code is generated from a pessimistically locked per-category sequence and checked for uniqueness.

**AR:** مسؤولة عن فئات الحضور والجداول الفعّالة والموظفين ونوع التعيين وربط هوية جهاز البصمة ودورة الاستحقاق وأهلية السُلف. يبدأ كود الموظف بكود الفئة؛ يُنسّق الجزء الذي يدخله المستخدم، أو يُولّد تلقائيًا من sequence مقفول لكل فئة مع التحقق من عدم التكرار.

**EN:** Sample attendance categories and schedules are demo fixtures, not production bootstrap data. They are created only when the `dev` or `demo` profile is active and `hr.bootstrap.demo-data=true`, or by the test-only Liquibase master.

**AR:** فئات الحضور والجداول النموذجية بيانات تجريبية وليست تهيئة إنتاجية. لا تُنشأ إلا مع ملف التعريف `dev` أو `demo` وتفعيل `hr.bootstrap.demo-data=true`، أو من خلال سجل Liquibase الاختباري فقط.
