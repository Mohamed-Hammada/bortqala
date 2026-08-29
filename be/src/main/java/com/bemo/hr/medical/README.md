# Medical Clinic Module (com.bemo.hr.medical)

## English Overview
The Medical Clinic module provides clinical operations, patient record management, consultation queue workflows, electronic prescriptions (e-Rx), and doctor commission statements for outpatient healthcare clinics and medical practices.

### Key Capabilities
- **Patient Master Index (PMI)**: Patient demographics, auto-generated MRN sequence, 14-digit Egyptian National ID parsing (extracting birth date, gender, and governorate), blood group, allergies, and duplicate detection by phone or national ID.
- **Consultation Queue**: Daily token generation per doctor, state transitions (`WAITING` → `IN_ROOM` → `DONE` / `CANCELLED`), copay calculations (fee charged vs insurance covered), and real-time waiting line board.
- **e-Prescriptions (e-Rx)**: Structured medication lines (drug name, dose, frequency, duration, instructions) and print layout with medical headers.
- **Doctor Commissions**: Monthly commission calculations based on completed consultations and configurable revenue-sharing rates.

---

## نظرة عامة بالعربية
يوفر موديول العيادات الطبية إدارة العمليات الإكلينيكية وسجلات المرضى، وطابور الكشف، والروشتات الإلكترونية، وحساب عمولات الأطباء الشهرية للعيادات والمراكز الطبية.

### القدرات الرئيسية
- **سجل المرضى الرئيسي (PMI)**: بيانات المرضى، الترقيم التلقائي للملف الطبي (MRN)، تحليل الرقم القومي المصري (14 رقماً) واستخراج تاريخ الميلاد والنوع والمحافظة، فصائل الدم، الحساسية، وكشف التكرار برقم الهاتف أو الرقم القومي.
- **طابور انتظار العيادة**: إصدار أرقام الأدوار اليومية لكل طبيب، إدارة حالات الكشف (`في الانتظار` ← `في غرفة الكشف` ← `مكتمل` / `ملغي`)، حساب نسب التحمل والتأمين، ولوحة العرض اللحظية.
- **الروشتة الإلكترونية (e-Rx)**: تسجيل الأدوية والجرعات والتكرار والمدة والتعليمات، مع نموذج طباعة الروشتة المعتمد.
- **عمولات الأطباء**: كشف العمولات الشهري بناءً على عدد الكشوفات المكتملة والإيراد المحقق ونسبة المشاركة.
