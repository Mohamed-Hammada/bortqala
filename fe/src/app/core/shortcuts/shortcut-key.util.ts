export function displayShortcutKey(code: string): string {
  if (!code) return '';
  if (code.startsWith('Key')) return code.substring(3);
  if (code.startsWith('Digit')) return code.substring(5);
  return code;
}
