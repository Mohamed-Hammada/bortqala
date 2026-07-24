export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

export function timestampedExcelFileName(arabicName: string, englishName: string, locale: string): string {
  const now = new Date();
  const two = (value: number) => String(value).padStart(2, '0');
  const timestamp = `${now.getFullYear()}${two(now.getMonth() + 1)}${two(now.getDate())}-${two(now.getHours())}${two(now.getMinutes())}`;
  const featureName = locale.toLowerCase().startsWith('ar') ? arabicName : englishName;
  return `${featureName}-${timestamp}.xlsx`;
}
