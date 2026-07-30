import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, resolve } from 'node:path';

const sourceRoot = resolve('src/app');
const translationsRoot = resolve('../be/src/main/resources/db/changelog/data');
const required = new Set();
const walk = (directory) => {
  for (const name of readdirSync(directory)) {
    const path = join(directory, name);
    if (statSync(path).isDirectory()) walk(path);
    else if (/\.(ts|html)$/.test(name)) {
      const source = readFileSync(path, 'utf8');
      for (const match of source.matchAll(/(?:i18n\.t|\.translate)\(\s*['"]([^'"]+)['"]/g)) required.add(match[1]);
    }
  }
};
walk(sourceRoot);

const byLocale = new Map([['ar-EG', new Set()], ['en-US', new Set()]]);
const filesUnder = (directory, extension) => {
  const files = [];
  for (const name of readdirSync(directory)) {
    const path = join(directory, name);
    if (statSync(path).isDirectory()) files.push(...filesUnder(path, extension));
    else if (name.endsWith(extension)) files.push(path);
  }
  return files;
};
for (const path of filesUnder(translationsRoot, '.csv')) {
  const lines = readFileSync(path, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/).slice(1);
  for (const line of lines) {
    const columns = line.split(';');
    if (columns.length >= 4 && byLocale.has(columns[2])) byLocale.get(columns[2]).add(columns[1]);
  }
}
const changelogRoot = resolve('../be/src/main/resources/db/changelog');
for (const path of filesUnder(changelogRoot, '.yaml')) {
  const source = readFileSync(path, 'utf8');
  for (const match of source.matchAll(/translation_key,\s*value:\s*['"]?([\w.-]+)['"]?[\s\S]{0,350}?locale,\s*value:\s*['"]?([\w-]+)['"]?/g)) {
    if (byLocale.has(match[2])) byLocale.get(match[2]).add(match[1]);
  }
  for (const match of source.matchAll(/VALUES\s*\(\s*'[^']+'\s*,\s*'([\w.-]+)'\s*,\s*'(ar-EG|en-US)'/g)) {
    byLocale.get(match[2]).add(match[1]);
  }
}
const missing = [...required].sort().flatMap((key) => [...byLocale].filter(([, keys]) => !keys.has(key)).map(([locale]) => `${locale}: ${key}`));
if (missing.length) {
  console.error(`Missing database translations (${missing.length}):\n${missing.join('\n')}`);
  process.exit(1);
}
console.log(`i18n check passed: ${required.size} literal keys exist in ar-EG and en-US.`);
