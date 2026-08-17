# Payroll integrity / سلامة الرواتب

The authoritative salary lifecycle is `DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID`. Generic status mutation is forbidden. Calculation freezes the source snapshot, transition commands advance one step, payment requires `POSTED`, and payment reversal requires `PAID`.

Salary mutations lock the row and payment/reversal requests include the expected entity version. Creator, payer, and reverser are stored separately in `created_by`, `paid_by`, and `reversed_by`; reversal time and reason are preserved independently from the original note.

دورة الراتب المعتمدة هي `مسودة → محسوب → مراجع → معتمد → مرحّل → مصروف`. لا يسمح بتغيير الحالة بشكل عام أو بتجاوز أي خطوة. تؤدي عملية الحساب إلى تجميد لقطة المصدر، ولا يتم الصرف إلا من الحالة المرحلة، ولا يتم العكس إلا بعد الصرف.

تقفل عمليات التعديل صف الراتب، وترسل طلبات الصرف والعكس رقم الإصدار المتوقع. تُحفظ هوية المنشئ والقائم بالصرف والقائم بالعكس في حقول منفصلة، كما يُحفظ وقت العكس وسببه دون تغيير الملاحظة أو هوية المنشئ.
