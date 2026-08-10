# Trial and demo control / التحكم في التجربة والعرض

**EN:** `TrialDemoService` owns the tenant commercial lifecycle. Trial dates are stored on the existing tenant row; an expired trial remains readable while the central MVC interceptor blocks business writes with `TRIAL_EXPIRED_READ_ONLY`. Paid conversion changes only the commercial state, so the tenant ID and all ERP records remain intact.

Demo templates are immutable versioned definitions. Reset is permitted only for an explicitly marked demo tenant, is operation-idempotent, audits template version and actor, and deletes only `demo_sample_records` owned by the current tenant. It never deletes employees, attendance, journals, inventory, parties, or other ERP aggregates.

**AR:** تملك خدمة `TrialDemoService` دورة الحالة التجارية للمستأجر. تُحفظ تواريخ التجربة على سجل المستأجر نفسه؛ وبعد الانتهاء تظل البيانات قابلة للقراءة بينما يمنع المعترض المركزي كتابات الأعمال بالكود `TRIAL_EXPIRED_READ_ONLY`. التحويل إلى مدفوع يغيّر الحالة التجارية فقط، لذلك تبقى هوية المستأجر وكل سجلات ERP دون تغيير.

قوالب العرض تعريفات ثابتة ذات إصدار. لا تُسمح إعادة الضبط إلا لمستأجر محدد صراحةً كعرض تجريبي، وهي آمنة عند تكرار `operationId` وتسجل إصدار القالب والمنفذ. الحذف محصور في `demo_sample_records` التابعة للمستأجر الحالي، ولا يمس الموظفين أو الحضور أو القيود أو المخزون أو الأطراف أو أي تجميع أعمال آخر.
