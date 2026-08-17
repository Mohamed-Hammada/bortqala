# Payroll / الرواتب

The payroll page follows one server-authoritative lifecycle:

`DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID`

Calculation freezes the payroll input snapshot. Each status action advances exactly one step, disbursement is available only for `POSTED` rows, and reversal is available only for `PAID` rows. Mutation requests carry the row version so stale browser state cannot overwrite a concurrent change.

تتبع صفحة الرواتب دورة واحدة معتمدة من الخادم:

`مسودة → محسوب → مراجع → معتمد → مرحّل → مصروف`

تؤدي خطوة الحساب إلى تجميد لقطة مدخلات الرواتب، ويتقدم كل إجراء حالة خطوة واحدة فقط. لا يتاح الصرف إلا للصفوف المرحلة، ولا يتاح العكس إلا للصفوف المصروفة، وترسل طلبات التعديل رقم إصدار الصف لمنع الكتابة فوق تغيير متزامن.
