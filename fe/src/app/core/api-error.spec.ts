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

    expect(message).toContain('Employee code already exists.');
    expect(message).toContain('must not be blank');
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

    expect(apiErrorMessage(error)).toBe('Employee code already exists.');
  });

  it('translates the backend error code from the i18n bundle when present', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        code: 'PASSWORD_REUSE',
        message: 'The new password must differ from the current password.',
        localizedMessage: 'The new password must differ from the current password.',
        status: 400,
      },
    });
    const i18n = {
      t: (key: string) =>
        key === 'PASSWORD_REUSE' ? 'كلمة المرور الجديدة يجب أن تختلف عن كلمة المرور الحالية.' : key,
    };

    expect(apiErrorMessage(error, i18n)).toBe('كلمة المرور الجديدة يجب أن تختلف عن كلمة المرور الحالية.');
  });

  it('prefers the backend localized message when the code is not a bundle key', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { code: 'UNKNOWN_CODE', localizedMessage: 'رسالة مترجمة.', status: 400 },
    });
    const i18n = { t: (key: string) => key };

    expect(apiErrorMessage(error, i18n)).toBe('رسالة مترجمة.');
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
  it('extracts the message and honours the fallback', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'BUSINESS_CONFLICT', message: 'Employee code already exists.', status: 409 },
    });

    expect(apiErrorDetail(error, 'تعذر التنفيذ.')).toBe('Employee code already exists.');
  });

  it('uses the fallback when no server body is present', () => {
    expect(apiErrorDetail(new HttpErrorResponse({ status: 500 }), 'تعذر التنفيذ.')).toBe('تعذر التنفيذ.');
  });

  it('handles plain error objects (legacy subscribers)', () => {
    const plain = { error: { detail: 'Duplicate key' }, message: 'x' };

    expect(apiErrorDetail(plain, 'fallback')).toBe('Duplicate key');
  });
});
