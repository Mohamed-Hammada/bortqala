import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import { escapeCsvCell } from './download';

describe('escapeCsvCell', () => {
  it('prefixes formula injection prefixes with an apostrophe', () => {
    expect(escapeCsvCell('=SUM(A1:A9)')).toBe(`'=SUM(A1:A9)`);
    expect(escapeCsvCell('+1+1')).toBe(`'+1+1`);
    expect(escapeCsvCell('-1')).toBe(`'-1`);
    expect(escapeCsvCell('@cmd')).toBe(`'@cmd`);
  });

  it('still escapes embedded double quotes', () => {
    expect(escapeCsvCell('say "hi"')).toBe(`say ""hi""`);
    expect(escapeCsvCell('=a"b')).toBe(`'=a""b`);
  });

  it('leaves safe values unchanged', () => {
    expect(escapeCsvCell('EMP-001')).toBe('EMP-001');
    expect(escapeCsvCell('أحمد علي')).toBe('أحمد علي');
    expect(escapeCsvCell('')).toBe('');
    expect(escapeCsvCell('2026-07-31')).toBe('2026-07-31');
    expect(escapeCsvCell('(123)')).toBe('(123)');
  });
});
