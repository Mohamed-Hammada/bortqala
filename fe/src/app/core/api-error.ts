import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from './auth/auth.models';
import { I18nService } from './i18n.service';

const KNOWN_MESSAGES: Record<string, { 'ar-EG': string; 'en-US': string }> = {
  'Select a non-empty biometric file.': {
    'ar-EG': 'يرجى اختيار ملف بصمة غير فارغ.',
    'en-US': 'Please select a non-empty biometric file.',
  },
  'Device name is required.': {
    'ar-EG': 'اسم الجهاز مطلوب.',
    'en-US': 'Device name is required.',
  },
  'Importer name is required.': {
    'ar-EG': 'اسم مستورد البيانات مطلوب.',
    'en-US': 'Importer name is required.',
  },
  'Could not read the uploaded file.': {
    'ar-EG': 'تعذر قراءة الملف المرفوع.',
    'en-US': 'Could not read the uploaded file.',
  },
  'Supported biometric files are CSV, XLSX, and XLS.': {
    'ar-EG': 'صيغ ملفات البصمة المدعومة هي CSV و XLSX و XLS.',
    'en-US': 'Supported biometric files are CSV, XLSX, and XLS.',
  },
  'Could not read the biometric file.': {
    'ar-EG': 'تعذر قراءة ملف البصمة.',
    'en-US': 'Could not read the biometric file.',
  },
  'The biometric file is empty.': {
    'ar-EG': 'ملف البصمة فارغ.',
    'en-US': 'The biometric file is empty.',
  },
  'The biometric sheet is empty.': {
    'ar-EG': 'ورقة ملف البصمة فارغة.',
    'en-US': 'The biometric sheet is empty.',
  },
  'Category code already exists.': {
    'ar-EG': 'رمز الفئة مستخدم بالفعل.',
    'en-US': 'Category code already exists.',
  },
  'Deactivate or move active employees before deactivating this category.': {
    'ar-EG': 'يرجى تعطيل أو نقل الموظفين النشطين قبل تعطيل هذه الفئة.',
    'en-US': 'Deactivate or move active employees before deactivating this category.',
  },
  'Work days cannot contain an empty value.': {
    'ar-EG': 'أيام العمل لا يمكن أن تحتوي على قيمة فارغة.',
    'en-US': 'Work days cannot contain an empty value.',
  },
  'Schedule end date cannot be before its start date.': {
    'ar-EG': 'تاريخ نهاية الجدول لا يمكن أن يكون قبل تاريخ بدايته.',
    'en-US': 'Schedule end date cannot be before its start date.',
  },
  'Schedule effective date ranges cannot overlap.': {
    'ar-EG': 'نطاقات التواريخ الفعالة للجدول لا يمكن أن تتداخل.',
    'en-US': 'Schedule effective date ranges cannot overlap.',
  },
  'Employee active-to date cannot be before active-from date.': {
    'ar-EG': 'تاريخ نهاية فاعلية الموظف لا يمكن أن يكون قبل تاريخ بدايتها.',
    'en-US': 'Employee active-to date cannot be before active-from date.',
  },
  'A unique biometric device ID is required for active employees in biometric categories.': {
    'ar-EG': 'رقم المستخدم بالجهاز (Biometric ID) مطلوب وفريد للموظفين النشطين في الفئات ذات البصمة.',
    'en-US': 'A unique biometric device ID is required for active employees in biometric categories.',
  },
  'Device user id is already mapped to another employee.': {
    'ar-EG': 'رقم الجهاز مستخدم بالفعل لموظف آخر.',
    'en-US': 'Device user id is already mapped to another employee.',
  },
  'Employee code already exists.': {
    'ar-EG': 'كود الموظف مستخدم بالفعل.',
    'en-US': 'Employee code already exists.',
  },
  'This record changed since it was loaded. Refresh and try again.': {
    'ar-EG': 'تم تعديل السجل بواسطة مستخدم آخر. يرجى تحديث الصفحة والمحاولة مرة أخرى.',
    'en-US': 'This record changed since it was loaded. Refresh and try again.',
  },
  'Item code already exists.': {
    'ar-EG': 'كود الصنف مستخدم بالفعل.',
    'en-US': 'Item code already exists.',
  },
  'This item changed. Refresh and retry.': {
    'ar-EG': 'تم تعديل هذا الصنف. يرجى التحديث والمحاولة مرة أخرى.',
    'en-US': 'This item changed. Refresh and retry.',
  },
  'Quantity and amount cannot both be zero.': {
    'ar-EG': 'الكمية والمبلغ لا يمكن أن يكونا صفرين معاً.',
    'en-US': 'Quantity and amount cannot both be zero.',
  },
  'Quantity must be a positive number.': {
    'ar-EG': 'الكمية يجب أن تكون رقماً موجباً.',
    'en-US': 'Quantity must be a positive number.',
  },
  'Loss percentage must be between 0 and 100.': {
    'ar-EG': 'نسبة الهالك يجب أن تكون بين 0 و 100.',
    'en-US': 'Loss percentage must be between 0 and 100.',
  },
  'An inventory item is required for quantity movement.': {
    'ar-EG': 'صنف المخزون مطلوب لحركة الكمية.',
    'en-US': 'An inventory item is required for quantity movement.',
  },
  'A business party is required for a financial movement.': {
    'ar-EG': 'الجهة (الطرف التجاري) مطلوبة للحركة المالية.',
    'en-US': 'A business party is required for a financial movement.',
  },
  'Advance amount cannot be zero.': {
    'ar-EG': 'مبلغ السلفة لا يمكن أن يكون صفراً.',
    'en-US': 'Advance amount cannot be zero.',
  },
  'This employee category does not allow advances.': {
    'ar-EG': 'فئة الموظف لا تسمح بالصرف المسبق للسلف.',
    'en-US': 'This employee category does not allow advances.',
  },
  'This business party changed since it was loaded. Refresh and try again.': {
    'ar-EG': 'تم تعديل بيانات الجهة. يرجى التحديث والمحاولة.',
    'en-US': 'This business party changed since it was loaded. Refresh and try again.',
  },
  'Business party code already exists.': {
    'ar-EG': 'كود الجهة مستخدم بالفعل.',
    'en-US': 'Business party code already exists.',
  },
  'Year is outside the supported range.': {
    'ar-EG': 'السنة خارج نطاق السنوات المدعومة.',
    'en-US': 'Year is outside the supported range.',
  },
  'A report for this pay cycle already overlaps the selected period.': {
    'ar-EG': 'يوجد تقرير مستخرج لهذه الدورة ويتداخل مع الفترة المختارة.',
    'en-US': 'A report for this pay cycle already overlaps the selected period.',
  },
  'No attendance categories use this pay cycle.': {
    'ar-EG': 'لا توجد فئات حضور تستخدم دورة الدفع هذه.',
    'en-US': 'No attendance categories use this pay cycle.',
  },
  'This row does not require an HR decision.': {
    'ar-EG': 'هذا السطر لا يتطلب قراراً من الموارد البشرية.',
    'en-US': 'This row does not require an HR decision.',
  },
  'Choose CONFIRMED or REJECTED.': {
    'ar-EG': 'اختر مؤكد (CONFIRMED) أو مرفوض (REJECTED).',
    'en-US': 'Choose CONFIRMED or REJECTED.',
  },
  'Select at least one role.': {
    'ar-EG': 'اختر دوراً واحداً على الأقل.',
    'en-US': 'Select at least one role.',
  },
  'Password is required for a new user.': {
    'ar-EG': 'كلمة المرور مطلوبة للمستخدم الجديد.',
    'en-US': 'Password is required for a new user.',
  },
  'Username already exists.': {
    'ar-EG': 'اسم المستخدم مستخدم بالفعل.',
    'en-US': 'Username already exists.',
  },
  'User not found.': {
    'ar-EG': 'المستخدم غير موجود.',
    'en-US': 'User not found.',
  },
  'Application not found.': {
    'ar-EG': 'التطبيق غير موجود.',
    'en-US': 'Application not found.',
  },
  'The username or password is incorrect.': {
    'ar-EG': 'اسم المستخدم أو كلمة المرور غير صحيحة.',
    'en-US': 'The username or password is incorrect.',
  },
  'The operation conflicts with existing data.': {
    'ar-EG': 'العملية تتعارض مع البيانات الحالية.',
    'en-US': 'The operation conflicts with existing data.',
  },
  'One or more fields are invalid.': {
    'ar-EG': 'واحد أو أكثر من الحقول غير صالحة.',
    'en-US': 'One or more fields are invalid.',
  },
  'must not be blank': {
    'ar-EG': 'هذا الحقل مطلوب ولا يمكن أن يكون فارغاً',
    'en-US': 'must not be blank',
  },
};

function translateRawMsg(msg: string, i18n?: Pick<I18nService, 't'>): string {
  if (!msg) return msg;
  const lang = ((i18n as any)?.lang?.() ?? 'ar-EG') as 'ar-EG' | 'en-US';
  const entry = KNOWN_MESSAGES[msg.trim()];
  if (entry) {
    return entry[lang] ?? entry['ar-EG'];
  }
  return msg;
}

export function apiErrorMessage(error: unknown, i18n?: Pick<I18nService, 't'>): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ApiProblem | null;
    if (problem?.errors) {
      const msgs = Object.values(problem.errors).map((m) => translateRawMsg(m, i18n));
      return msgs.join(' — ');
    }
    if (problem?.detail) return translateRawMsg(problem.detail, i18n);
    if (error.status === 0) return i18n?.t('api.connectionError') ?? 'Unable to reach the server.';
    if (error.status === 401) return i18n?.t('api.unauthorized') ?? 'Authentication failed.';
  }
  return i18n?.t('api.unexpected') ?? 'An unexpected error occurred.';
}
