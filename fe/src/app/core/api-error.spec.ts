import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage, apiErrorDetail } from './api-error';

describe('apiErrorMessage', () => {
  it('joins field errors from the standard error shape', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        code: 'VALIDATION_FAILED',
        message: 'One or more fields are invalid.',
        status: 400,
        fieldErrors: [
          { field: 'employeeCode', code: 'INVALID_VALUE', message: 'Employee code already exists.' },
          { field: 'name', code: 'INVALID_VALUE', message: 'must not be blank' },
        ],
      },
    });

    const message = apiErrorMessage(error);

    expect(message).toContain('كود الموظف مستخدم بالفعل.');
    expect(message).toContain('هذا الحقل مطلوب ولا يمكن أن يكون فارغاً');
  });

  it('prefers localizedMessage over message', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        code: 'BUSINESS_CONFLICT',
        message: 'Employee code already exists.',
        localizedMessage: 'كود الموظف مستخدم بالفعل.',
        status: 409,
      },
    });

    expect(apiErrorMessage(error)).toBe('كود الموظف مستخدم بالفعل.');
  });

  it('falls back to the English message when no localized message is present', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'BUSINESS_CONFLICT', message: 'Employee code already exists.', status: 409 },
    });

    expect(apiErrorMessage(error)).toBe('كود الموظف مستخدم بالفعل.');
  });

  it('reports connection failures for status 0', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(apiErrorMessage(error)).toBe('Unable to reach the server.');
  });

  it('falls back to a generic message for unexpected errors', () => {
    expect(apiErrorMessage(new Error('boom'))).toBe('An unexpected error occurred.');
  });
});

describe('apiErrorDetail', () => {
  it('extracts the localized message and honours the fallback', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'BUSINESS_CONFLICT', message: 'Employee code already exists.', status: 409 },
    });

    expect(apiErrorDetail(error, 'تعذر التنفيذ.')).toBe('كود الموظف مستخدم بالفعل.');
  });

  it('uses the fallback when no server body is present', () => {
    expect(apiErrorDetail(new HttpErrorResponse({ status: 500 }), 'تعذر التنفيذ.')).toBe('تعذر التنفيذ.');
  });

  it('handles plain error objects (legacy subscribers)', () => {
    const plain = { error: { detail: 'Duplicate key' }, message: 'x' };

    expect(apiErrorDetail(plain, 'fallback')).toBe('Duplicate key');
  });
});
