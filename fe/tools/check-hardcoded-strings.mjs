import { readFileSync, readdirSync, statSync } from 'node:fs';
import { resolve } from 'node:path';

const sourceRoot = resolve('src/app');

const files = [];
const walk = (directory) => {
  for (const name of readdirSync(directory)) {
    const path = `${directory}/${name}`;
    if (statSync(path).isDirectory()) walk(path);
    else if (name.endsWith('.html')) files.push(path);
  }
};
walk(sourceRoot);

const LETTER = /[A-Za-z\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]/;
const DISPLAY_LITERAL = /[\s\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]/;
const DIRECTIVE = /@(?:if|else\s+if|else|for|switch|case|default|empty|placeholder)\b[^{}\n]*\{?/;
const LET = /@let\b[^;\n]*;/;
const PUNCT_ONLY = /^[\s,.;:!?()\[\]{}\-–—_·•|/\\"'‘’“”«»<>+=*#&%$@~`^°]+$/;
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
]);

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
            if (source[i + 1] === '}') { i += 2; break; }
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
      const m = /^@(if|else\s+if|else|for|switch|case|default|empty|placeholder|let|defer)\b/.exec(source.slice(i, i + 20));
      if (m) {
        const rest = source.slice(i);
        const brace = rest.indexOf('{');
        const semi = rest.indexOf(';');
        const stop = m[1] === 'let' || m[1] === 'defer' ? semi : brace;
        i = stop === -1 ? i + m[0].length : i + stop + 1;
        continue;
      }
    }
    out += c;
    i += 1;
  }
  return out;
}

let failures = 0;

function stripInterpolationLiterals(expr) {
  let out = '';
  let i = 0;
  const n = expr.length;
  while (i < n) {
    if (expr.startsWith("i18n.t(", i)) {
      let depth = 0;
      i += 7;
      while (i < n) {
        if (expr[i] === '(') depth += 1;
        else if (expr[i] === ')') {
          if (depth === 0) { i += 1; break; }
          depth -= 1;
        }
        i += 1;
      }
      while (i < n) {
        const rest = expr.slice(i);
        const m = /^\s*\|\|\s*'(?:[^'\\]|\\.)*'/.exec(rest);
        if (!m) break;
        i += m[0].length;
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
        failures += 1;
        console.log(`${path}: [interpolation literal] ${value}`);
      }
    }
  }
}

for (const path of files) scan(path);
for (const path of files) scanInterpolations(readFileSync(path, 'utf8'), path);

function scan(path) {
  const source = readFileSync(path, 'utf8');
  const text = stripTags(source);
  const lines = text.split(/\r?\n/);
  for (const raw of lines) {
    let line = raw;
    if (/^\s*\}\s*$/.test(line)) continue;
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
    if (stripped !== line && (PUNCT_ONLY.test(stripped) || ALLOWED_EXACT.has(stripped) || !LETTER.test(stripped))) continue;
    failures += 1;
    console.log(`${path}: ${line}`);
  }
}

if (failures) {
  console.error(`Hardcoded UI text found: ${failures} candidate(s). Wrap text in i18n.t() with a translation key.`);
  process.exit(1);
}
console.log(`Hardcoded-UI check passed: ${files.length} templates scanned, no bare text nodes found.`);
