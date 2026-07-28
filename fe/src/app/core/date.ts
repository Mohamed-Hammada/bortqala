const COMPANY_TIME_ZONE = 'Africa/Cairo';

export function dateInputToEpoch(value: string): number {
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) throw new Error('Invalid date input.');
  return Date.UTC(year, month - 1, day);
}

export function epochToDateInput(value: number): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: COMPANY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date(value));
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((item) => item.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

export function formatDate(value: number): string {
  return new Intl.DateTimeFormat('ar-EG', {
    timeZone: COMPANY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value));
}

export function formatDateTime(value: number): string {
  return new Intl.DateTimeFormat('ar-EG', {
    timeZone: COMPANY_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatTime(value: number): string {
  return new Intl.DateTimeFormat('ar-EG', {
    timeZone: COMPANY_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatDateReadable(value: number | string | null | undefined, locale: string = 'ar-EG'): string {
  if (value === null || value === undefined || value === '') return '—';
  let date: Date;
  if (typeof value === 'number') {
    date = new Date(value);
  } else if (typeof value === 'string') {
    const num = Number(value);
    if (!isNaN(num) && num > 100000000000) {
      date = new Date(num);
    } else {
      date = new Date(value);
    }
  } else {
    return '—';
  }

  if (isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat(locale.startsWith('en') ? 'en-US' : 'ar-EG', {
    timeZone: COMPANY_TIME_ZONE,
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date);
}
