#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const root = process.argv[2] ?? path.resolve('dist');
if (!fs.existsSync(root)) {
  console.error(`Build directory not found: ${root}`);
  process.exit(2);
}

const files = [];
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    const stat = fs.statSync(full);
    if (stat.isDirectory()) walk(full); else files.push(full);
  }
}
walk(root);

const maps = files.filter((file) => file.endsWith('.map'));
const js = files.filter((file) => file.endsWith('.js'));
const unhashed = js.filter((file) => {
  const name = path.basename(file);
  return !/^main-[A-Z0-9]+\.js$/i.test(name)
      && !/^chunk-[A-Z0-9]+\.js$/i.test(name)
      && !/^polyfills-[A-Z0-9]+\.js$/i.test(name);
});

let devMarkers = [];
for (const file of js) {
  const content = fs.readFileSync(file, 'utf8');
  if (content.includes('@fs/') || content.includes('@angular/build:dev-server')) devMarkers.push(file);
}

if (maps.length || devMarkers.length) {
  console.error('Production build verification failed.');
  if (maps.length) console.error('Source maps found:', maps);
  if (devMarkers.length) console.error('Development-server markers found:', devMarkers);
  process.exit(1);
}

console.log(`Production bundle check passed: ${js.length} JS bundles, no source maps, no dev-server markers.`);
if (unhashed.length) console.warn('Review JS names that do not match the expected hashed bundle pattern:', unhashed);
