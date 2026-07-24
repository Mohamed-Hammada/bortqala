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
