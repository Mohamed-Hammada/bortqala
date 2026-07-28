import { describe, it, expect } from 'vitest';
import { dateInputToEpoch, epochToDateInput, formatDate } from './date';

describe('epoch-millisecond date contract', () => {
  it('round-trips an HTML date through the configured company time zone', () => {
    const epoch = dateInputToEpoch('2026-07-24');

    expect(typeof epoch).toBe('number');
    expect(epochToDateInput(epoch)).toBe('2026-07-24');
    expect(formatDate(epoch)).toContain('٢٠٢٦');
  });
});
