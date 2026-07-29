import { shouldRenderAttendanceMatrix } from './manual-attendance.component';

describe('manual attendance matrix visibility', () => {
  it('keeps the editable matrix visible after calculation rules load', () => {
    expect(shouldRenderAttendanceMatrix(false, null, 2)).toBe(true);
  });

  it('hides the matrix only while loading, on errors, or without workers', () => {
    expect(shouldRenderAttendanceMatrix(true, null, 2)).toBe(false);
    expect(shouldRenderAttendanceMatrix(false, 'تعذر التحميل', 2)).toBe(false);
    expect(shouldRenderAttendanceMatrix(false, null, 0)).toBe(false);
  });
});
