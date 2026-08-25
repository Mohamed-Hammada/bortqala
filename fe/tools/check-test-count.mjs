#!/usr/bin/env node
/**
 * CI regression gate: fail if the frontend test counts drop below baseline.
 *
 * Parses the Vitest summary lines emitted by `npx ng test --watch=false`:
 *   " Test Files  26 passed (26)"  and  "      Tests  136 passed (136)"
 *
 * Usage (from fe/): npx ng test --watch=false | node tools/check-test-count.mjs
 */
import { createInterface } from 'node:readline';
import { stdin, stdout, exit } from 'node:process';

const MIN_TESTS = 493;
const MIN_FILES = 101;

const ANSI = /\u001B\[[0-?]*[ -/]*[@-~]/g;

let tests = null;
let files = null;
let failuresSeen = false;

const rl = createInterface({ input: stdin, crlfDelay: Infinity });

rl.on('line', (raw) => {
  const line = raw.replace(ANSI, '');
  const testMatch = line.match(/Tests\s+(?:\d+\s+failed\s*\|\s*)?(\d+)\s+passed/);
  if (testMatch) {
    tests = Number(testMatch[1]);
  }
  const fileMatch = line.match(/Test Files\s+(?:\d+\s+failed\s*\|\s*)?(\d+)\s+passed/);
  if (fileMatch) {
    files = Number(fileMatch[1]);
  }
  if (/Tests\s+\d+\s+failed/.test(line)) {
    failuresSeen = true;
  }
});

rl.on('close', () => {
  console.log(
    `frontend tests: ${tests ?? 'unknown'} (min ${MIN_TESTS}) | files: ${files ?? 'unknown'} (min ${MIN_FILES}) | failed: ${failuresSeen}`,
  );

  let ok = true;
  if (tests === null || tests < MIN_TESTS) {
    console.error(`FAIL: test count ${tests} below baseline ${MIN_TESTS}`);
    ok = false;
  }
  if (files === null || files < MIN_FILES) {
    console.error(`FAIL: file count ${files} below baseline ${MIN_FILES}`);
    ok = false;
  }
  if (failuresSeen) {
    console.error('FAIL: test failures present');
    ok = false;
  }
  exit(ok ? 0 : 1);
});
