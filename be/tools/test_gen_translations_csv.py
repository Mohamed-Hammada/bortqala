#!/usr/bin/env python3
"""Tests for gen-translations-csv.py"""
import csv
import io
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))
from gen_translations_csv import find_max_numeric_id, generate_csv, scan_existing_ids


class TestGenTranslationsCsv(unittest.TestCase):

    def test_find_max_numeric_id_empty_file(self):
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('id;translation_key;locale;text_value\n')
            path = f.name
        try:
            self.assertEqual(find_max_numeric_id(path), 0)
        finally:
            os.unlink(path)

    def test_find_max_numeric_id_nonexistent_file(self):
        self.assertEqual(find_max_numeric_id('/nonexistent/path.csv'), 0)

    def test_find_max_numeric_id_with_rows(self):
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('v355-001-en;foo;en-US;Foo\n')
            f.write('v355-012-ar;bar;ar-EG;Bar\n')
            f.write('v356-003-en;baz;en-US;Baz\n')
            path = f.name
        try:
            self.assertEqual(find_max_numeric_id(path), 12)
        finally:
            os.unlink(path)

    def test_generate_csv_basic(self):
        data = {'foo.bar': {'ar': 'بار', 'en': 'Foo'}}
        csv_content = generate_csv(data, version=358, start_seq=1)
        reader = csv.reader(io.StringIO(csv_content), delimiter=';', quotechar='"')
        rows = list(reader)
        self.assertEqual(len(rows), 3)  # header + 2 rows
        self.assertEqual(rows[1][0], 'v358-001-en')
        self.assertEqual(rows[1][1], 'foo.bar')
        self.assertEqual(rows[1][2], 'en-US')
        self.assertEqual(rows[1][3], 'Foo')
        self.assertEqual(rows[2][0], 'v358-001-ar')
        self.assertEqual(rows[2][2], 'ar-EG')
        self.assertEqual(rows[2][3], 'بار')

    def test_generate_csv_sequential_ids(self):
        data = {'a': {'ar': 'أ', 'en': 'A'}, 'b': {'ar': 'ب', 'en': 'B'}}
        csv_content = generate_csv(data, version=358, start_seq=5)
        reader = csv.reader(io.StringIO(csv_content), delimiter=';', quotechar='"')
        rows = list(reader)
        self.assertEqual(rows[1][0], 'v358-005-en')
        self.assertEqual(rows[2][0], 'v358-005-ar')
        self.assertEqual(rows[3][0], 'v358-006-en')
        self.assertEqual(rows[4][0], 'v358-006-ar')

    def test_scan_existing_ids(self):
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('v355-001-en;foo;en-US;Foo\n')
            f.write('v355-001-ar;foo;ar-EG;بار\n')
            path = f.name
        try:
            ids = scan_existing_ids(path)
            self.assertEqual(ids, {'v355-001-en', 'v355-001-ar'})
        finally:
            os.unlink(path)


if __name__ == '__main__':
    unittest.main()
