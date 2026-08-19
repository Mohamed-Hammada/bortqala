import { readFileSync, readdirSync, statSync } from 'node:fs';
import { resolve } from 'node:path';

const sourceRoot = resolve('src/app');

const htmlFiles = [];
const tsFiles = [];

function walk(directory) {
  for (const name of readdirSync(directory)) {
    const path = `${directory}/${name}`;
    if (statSync(path).isDirectory()) {
      walk(path);
      continue;
    }
    if (name.endsWith('.html')) {
      htmlFiles.push(path);
      continue;
    }
    if (
      name.endsWith('.ts') &&
      !name.endsWith('.spec.ts') &&
      !name.endsWith('.d.ts')
    ) {
      tsFiles.push(path);
    }
  }
}
walk(sourceRoot);

const LETTER = /[A-Za-z\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]/;
const DISPLAY_LITERAL = /[\s\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]/;
const DIRECTIVE = /@(?:if|else\s+if|else|for|switch|case|default|empty|placeholder)\b[^{}\n]*\{?/;
const LET = /@let\b[^;\n]*;/;
const PUNCT_ONLY = /^[\s,.;:!?()[\]{}\-–—_·•|/\\\"'‘’“”«»<>+=*#&%$@~`^°]+$/;
const PAREN_TECH = /^\([A-Za-z0-9\s./%+\-]+\)$/;
const TIME_LITERAL = /^\d{1,2}:\d{2}(:\d{2})?\s*[AP]M$/;
const RATE_LITERAL = /^\d+\s*=\s*[A-Z]{3}$/;

const ALLOWED_EXACT = new Set([
  'BEMO',
  'Bemo ERP',
  'ERP Platform',
  'Business Operations Platform',
  'JSON API',
  'JSON API ⓘ',
  'SHA-256',
  'CSV • XLS • XLSX ()',
  'Ctrl K',
  'Ctrl + K',
  'Enter',
  'Excel',
  'ج.م',
  '&nbsp;',
  'v',
  'yyyy-MM-dd HH:mm',
  'yyyy-MM-dd HH:mm:ss',
  'SUPER_ADMIN',
  'ADMIN',
  'WORKFORCE_MANAGER',
  'WORKFORCE_FINANCE',
  'FINANCE_MANAGER',
  'PROCUREMENT_MANAGER',
  'PAYROLL_MANAGER',
  'HR_MANAGER',
]);

function findTagEnd(source, start) {
  let quote = null;
  for (let j = start + 1; j < source.length; j += 1) {
    const ch = source[j];
    if (quote) {
      if (ch === quote) quote = null;
    } else if (ch === '"' || ch === "'") {
      quote = ch;
    } else if (ch === '>') {
      return j;
    }
  }
  return source.length - 1;
}

function skipBlock(source, i) {
  const open = source.indexOf('<', i);
  if (open === -1) return i;
  const tagEnd = findTagEnd(source, open);
  const tag = source.slice(open + 1, tagEnd).trim().split(/[\s/]/)[0];
  const closing = new RegExp(`<\\/${tag}[\\s>]`);
  const rest = source.slice(tagEnd + 1);
  const match = rest.match(closing);
  return match ? tagEnd + 1 + match.index + match[0].length : i;
}

function stripTags(source) {
  let out = '';
  let i = 0;
  const n = source.length;

  while (i < n) {
    const c = source[i];

    if (source.startsWith('<!--', i)) {
      const end = source.indexOf('-->', i);
      i = end === -1 ? n : end + 3;
      continue;
    }

    if (c === '<') {
      const end = findTagEnd(source, i);
      const tag = source.slice(i + 1, end).trim();
      if (/^(kbd|svg|script|style)(\s|$)/.test(tag)) {
        const skipped = skipBlock(source, i);
        i = skipped === i ? end + 1 : skipped;
        continue;
      }
      i = end + 1;
      continue;
    }

    if (source.startsWith('{{', i)) {
      let depth = 0;
      i += 2;
      while (i < n) {
        if (source[i] === '{') depth += 1;
        else if (source[i] === '}') {
          if (depth === 0) {
            if (source[i + 1] === '}') {
              i += 2;
              break;
            }
            i += 1;
            continue;
          }
          depth -= 1;
        }
        i += 1;
      }
      continue;
    }

    if (c === '@') {
      const match = /^@(if|else\s+if|else|for|switch|case|default|empty|placeholder|let|defer)\b/.exec(
        source.slice(i, i + 24),
      );
      if (match) {
        const rest = source.slice(i);
        const brace = rest.indexOf('{');
        const semi = rest.indexOf(';');
        const stop = match[1] === 'let' || match[1] === 'defer' ? semi : brace;
        i = stop === -1 ? i + match[0].length : i + stop + 1;
        continue;
      }
    }

    out += c;
    i += 1;
  }

  return out;
}

let failures = 0;

function report(path, kind, value, offset = null) {
  failures += 1;
  const suffix = offset === null ? '' : ` @${offset}`;
  console.log(`${path}: [${kind}]${suffix} ${value}`);
}

function stripInterpolationLiterals(expr) {
  let out = '';
  let i = 0;
  const n = expr.length;

  while (i < n) {
    if (expr.startsWith('i18n.t(', i)) {
      let depth = 0;
      i += 7;
      while (i < n) {
        if (expr[i] === '(') depth += 1;
        else if (expr[i] === ')') {
          if (depth === 0) {
            i += 1;
            break;
          }
          depth -= 1;
        }
        i += 1;
      }

      while (i < n) {
        const rest = expr.slice(i);
        const match = /^\s*\|\|\s*'(?:[^'\\]|\\.)*'/.exec(rest);
        if (!match) break;
        i += match[0].length;
      }
      continue;
    }

    out += expr[i];
    i += 1;
  }

  return out;
}

function scanInterpolations(source, path) {
  const re = /\{\{([\s\S]*?)\}\}/g;
  let match;

  while ((match = re.exec(source)) !== null) {
    const expr = match[1];
    const literals = expr.match(/'([^']+)'|"([^"]+)"/g) ?? [];

    for (const literal of literals) {
      const value = literal.slice(1, -1);
      if (!value || !LETTER.test(value)) continue;
      if (!DISPLAY_LITERAL.test(value)) continue;
      if (ALLOWED_EXACT.has(value)) continue;

      const cleaned = stripInterpolationLiterals(expr);
      if (cleaned.includes(literal)) {
        report(path, 'interpolation literal', value, match.index);
      }
    }
  }
}

function scanHtml(path) {
  const source = readFileSync(path, 'utf8');
  const text = stripTags(source);
  const lines = text.split(/\r?\n/);

  for (const raw of lines) {
    let line = raw;

    if (/^\s*}\s*$/.test(line)) continue;

    line = line.replace(/^\s*}/, '');
    line = line.replace(LET, '');
    line = line.replace(DIRECTIVE, '');
    line = line.replace(/[*@]/g, ' ').replace(/\s+/g, ' ').trim();

    if (!line) continue;
    if (!LETTER.test(line)) continue;
    if (PUNCT_ONLY.test(line)) continue;
    if (PAREN_TECH.test(line)) continue;
    if (TIME_LITERAL.test(line)) continue;
    if (RATE_LITERAL.test(line)) continue;
    if (ALLOWED_EXACT.has(line)) continue;

    const stripped = line
      .replace(/^[^A-Za-z\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]+/, '')
      .replace(/[^A-Za-z\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]+$/, '');

    if (
      stripped !== line &&
      (
        PUNCT_ONLY.test(stripped) ||
        ALLOWED_EXACT.has(stripped) ||
        !LETTER.test(stripped)
      )
    ) {
      continue;
    }

    report(path, 'HTML text', line);
  }

  scanInterpolations(source, path);
}

function skipWhitespace(source, index) {
  let i = index;
  while (i < source.length && /\s/.test(source[i])) i += 1;
  return i;
}

function parseStringLiteral(text) {
  const value = text.trim();
  if (value.length < 2) return null;

  const quote = value[0];
  if (!["'", '"', '`'].includes(quote)) return null;
  if (value[value.length - 1] !== quote) return null;

  // Template literals containing interpolation are dynamic and are not
  // automatically classified as hardcoded UI here.
  if (quote === '`' && value.includes('${')) return null;

  return value.slice(1, -1);
}

function readCallArguments(source, openParenIndex) {
  const args = [];
  let current = '';
  let paren = 0;
  let bracket = 0;
  let brace = 0;
  let quote = null;
  let escaped = false;

  for (let i = openParenIndex + 1; i < source.length; i += 1) {
    const ch = source[i];

    if (quote) {
      current += ch;
      if (escaped) {
        escaped = false;
      } else if (ch === '\\') {
        escaped = true;
      } else if (ch === quote) {
        quote = null;
      }
      continue;
    }

    if (ch === "'" || ch === '"' || ch === '`') {
      quote = ch;
      current += ch;
      continue;
    }

    if (ch === '(') {
      paren += 1;
      current += ch;
      continue;
    }
    if (ch === ')') {
      if (paren === 0 && bracket === 0 && brace === 0) {
        args.push(current.trim());
        return args;
      }
      paren -= 1;
      current += ch;
      continue;
    }
    if (ch === '[') {
      bracket += 1;
      current += ch;
      continue;
    }
    if (ch === ']') {
      bracket -= 1;
      current += ch;
      continue;
    }
    if (ch === '{') {
      brace += 1;
      current += ch;
      continue;
    }
    if (ch === '}') {
      brace -= 1;
      current += ch;
      continue;
    }

    if (ch === ',' && paren === 0 && bracket === 0 && brace === 0) {
      args.push(current.trim());
      current = '';
      continue;
    }

    current += ch;
  }

  return null;
}

function scanLiteralArgumentCalls(source, path, callName, argumentIndex, kind) {
  let cursor = 0;
  const needle = `${callName}(`;

  while (cursor < source.length) {
    const index = source.indexOf(needle, cursor);
    if (index < 0) break;

    const openParen = index + callName.length;
    const args = readCallArguments(source, openParen);

    if (args && args.length > argumentIndex) {
      const literal = parseStringLiteral(args[argumentIndex]);
      if (
        literal &&
        LETTER.test(literal) &&
        !ALLOWED_EXACT.has(literal)
      ) {
        report(path, kind, literal, index);
      }
    }

    cursor = index + needle.length;
  }
}

function scanInlineTemplates(source, path) {
  const normalizedPath = path.replace(/\\/g, '/');
  // v246 closes the inline-template blind spot for Workforce, where the mixed-language defects were found.
  if (!normalizedPath.includes('/features/workforce/')) return;
  const re = /\btemplate\s*:\s*`([\s\S]*?)`\s*,/g;
  let match;
  while ((match = re.exec(source)) !== null) {
    const template = match[1];
    const text = stripTags(template);
    for (const raw of text.split(/\r?\n/)) {
      let line = raw.replace(/^\s*}/, '').replace(LET, '').replace(DIRECTIVE, '').replace(/[*@]/g, ' ').replace(/\s+/g, ' ').trim();
      if (!line || !LETTER.test(line) || PUNCT_ONLY.test(line) || PAREN_TECH.test(line) || TIME_LITERAL.test(line) || RATE_LITERAL.test(line) || ALLOWED_EXACT.has(line)) continue;
      report(`${path}#inline-template`, 'HTML text', line, match.index);
    }
    scanInterpolations(template, `${path}#inline-template`);
    const attrRe = /(?<!\[)(placeholder|title|aria-label|appTooltip)\s*=\s*(["'])([^"']*[A-Za-z\u0600-\u06FF][^"']*)\2/g;
    let attr;
    while ((attr = attrRe.exec(template)) !== null) {
      if (!ALLOWED_EXACT.has(attr[3])) report(`${path}#inline-template`, `literal ${attr[1]}`, attr[3], match.index + attr.index);
    }
  }
}

function scanTypeScript(path) {
  const source = readFileSync(path, 'utf8');
  scanInlineTemplates(source, path);

  // Direct user-visible notification strings must always use i18n.t(...).
  for (const method of ['success', 'error', 'warning', 'info']) {
    scanLiteralArgumentCalls(
      source,
      path,
      `this.notification.${method}`,
      0,
      `TypeScript notification.${method}`,
    );
    scanLiteralArgumentCalls(
      source,
      path,
      `notification.${method}`,
      0,
      `TypeScript notification.${method}`,
    );
    if (path.replace(/\\/g, '/').includes('/features/workforce/')) {
      scanLiteralArgumentCalls(source, path, `this.notificationService.${method}`, 0, `TypeScript notificationService.${method}`);
      scanLiteralArgumentCalls(source, path, `notificationService.${method}`, 0, `TypeScript notificationService.${method}`);
    }
  }

  // Browser blocking dialogs are not allowed to contain hardcoded UI text.
  scanLiteralArgumentCalls(source, path, 'window.alert', 0, 'window.alert');
  scanLiteralArgumentCalls(source, path, 'window.confirm', 0, 'window.confirm');

  // Translation fallbacks hide missing DB translation keys and can mask
  // incomplete Arabic/English coverage. A visible fallback must therefore
  // be moved to the translation store.
  scanLiteralArgumentCalls(source, path, 'this.i18n.t', 2, 'i18n fallback');
  scanLiteralArgumentCalls(source, path, 'i18n.t', 2, 'i18n fallback');
}

for (const path of htmlFiles) {
  scanHtml(path);
}

for (const path of tsFiles) {
  scanTypeScript(path);
}

if (failures) {
  console.error(
    `Hardcoded UI text found: ${failures} candidate(s). ` +
    'Move visible text to i18n.t() and the translation store.',
  );
  process.exit(1);
}

console.log(
  `Hardcoded-UI check passed: ${htmlFiles.length} HTML templates and ` +
  `${tsFiles.length} TypeScript source files scanned.`,
);
