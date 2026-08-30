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

    def test_find_max_numeric_id_quoted_fields(self):
        # QUOTE_ALL CSVs write the id quoted; scanner must still read the sequence
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('id;translation_key;locale;text_value\n')
            f.write('"v355-001-en";"foo";"en-US";"Foo"\n')
            f.write('"v356-009-ar";"bar";"ar-EG";"بار"\n')
            path = f.name
        try:
            self.assertEqual(find_max_numeric_id(path), 9)
        finally:
            os.unlink(path)

    def test_find_max_numeric_id_skips_header_and_blank_lines(self):
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('id;translation_key;locale;text_value\n')
            f.write('\n')
            f.write('v357-004-en;foo;en-US;Foo\n')
            path = f.name
        try:
            self.assertEqual(find_max_numeric_id(path), 4)
        finally:
            os.unlink(path)

    def test_continues_sequence_after_real_changeset(self):
        # Proof against a real repo changeset: next emitted id follows the max.
        here = os.path.dirname(os.path.abspath(__file__))
        real_csv = os.path.join(
            here, '..', 'src', 'main', 'resources', 'db', 'changelog',
            'data', 'insert', 'files', '20260829_v413_manpower_client_billing_translations.csv')
        if not os.path.exists(real_csv):
            self.skipTest('V413 CSV not present in this checkout')
        max_existing = find_max_numeric_id(real_csv)
        generated = generate_csv({'k': {'ar': 'أ', 'en': 'A'}}, version=414, start_seq=max_existing + 1)
        first_row = list(csv.reader(io.StringIO(generated), delimiter=';', quotechar='"'))[1]
        self.assertEqual(first_row[0], f'v414-{max_existing + 1:03d}-en')
        # And the continuation id does not collide with any existing id in the file
        self.assertNotIn(first_row[0], scan_existing_ids(real_csv))

    def test_generated_output_has_zero_pk_collisions(self):
        data = {f'key.{i}': {'ar': 'أ', 'en': 'A'} for i in range(50)}
        generated = generate_csv(data, version=414, start_seq=1)
        rows = list(csv.reader(io.StringIO(generated), delimiter=';', quotechar='"'))[1:]
        ids = [r[0] for r in rows]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(len(rows), 100)  # 50 keys × 2 locales


if __name__ == '__main__':
    unittest.main()
