import glob
import csv
import yaml
import re
import os
import sys

sys.stdout.reconfigure(encoding='utf-8')

changelog_dir = os.path.normpath('be/src/main/resources/db/changelog')
csv_files = sorted(glob.glob(os.path.join(changelog_dir, '**', '*.csv'), recursive=True))
yaml_files = sorted(glob.glob(os.path.join(changelog_dir, '**', '*.yaml'), recursive=True))

ignore_csv_files = {
    '20260821_v333_sec_permissions.csv',
    '20260826_v356_expense_permissions.csv',
    '20260826_v351_user_role_templates.csv'
}

KEY_COLUMNS = ('translation_key', 'key')
VALUE_COLUMNS = ('text_value', 'text', 'value')

keys_dict = {} # key -> {'ar-EG': text, 'en-US': text}

# 1. Read all translation CSVs
for path in csv_files:
    fname = os.path.basename(path)
    if fname in ignore_csv_files:
        continue
    with open(path, 'r', encoding='utf-8-sig', newline='') as f:
        reader = csv.DictReader(f, delimiter=';')
        headers = set(reader.fieldnames or [])
        key_col = next((k for k in KEY_COLUMNS if k in headers), None)
        val_col = next((v for v in VALUE_COLUMNS if v in headers), None)
        if not key_col or 'locale' not in headers or not val_col:
            continue
        for row in reader:
            k = (row.get(key_col) or '').strip()
            loc = (row.get('locale') or '').strip()
            val = (row.get(val_col) or '').strip()
            if not k or not loc or not val:
                continue
            if loc not in ('ar-EG', 'en-US'):
                continue
            if k not in keys_dict:
                keys_dict[k] = {}
            # Conflict resolution: later file overrides earlier file
            # Special case fixes:
            if k == 'DEVICE_NOT_FOUND' and fname.startswith('20260830_v443'):
                keys_dict[k][loc] = val
            elif k == 'settings.tabSecurity' and fname.startswith('20260830_v443'):
                keys_dict[k][loc] = val
            else:
                keys_dict[k][loc] = val

# 2. Extract from YAML files
for path in yaml_files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
        for match in re.finditer(r'VALUES\s*\(\s*[\x27\"][^\x27\"]+[\x27\"]\s*,\s*[\x27\"]([\w.-]+)[\x27\"]\s*,\s*[\x27\"](ar-EG|en-US)[\x27\"]\s*,\s*[\x27\"]([^\x27\"]+)[\x27\"]', content):
            k, loc, val = match.group(1), match.group(2), match.group(3)
            if k not in keys_dict:
                keys_dict[k] = {}
            if loc not in keys_dict[k]:
                keys_dict[k][loc] = val
        for match in re.finditer(r'SELECT\s+[\x27\"][^\x27\"]+[\x27\"]\s*,\s*[\x27\"]([\w.-]+)[\x27\"]\s*,\s*[\x27\"](ar-EG|en-US)[\x27\"]\s*,\s*[\x27\"]([^\x27\"]+)[\x27\"]', content):
            k, loc, val = match.group(1), match.group(2), match.group(3)
            if k not in keys_dict:
                keys_dict[k] = {}
            if loc not in keys_dict[k]:
                keys_dict[k][loc] = val

print(f"Total collected translation keys: {len(keys_dict)}")
missing = {k: v for k, v in keys_dict.items() if 'ar-EG' not in v or 'en-US' not in v}
if missing:
    print(f"ERROR: {len(missing)} keys missing a locale:")
    for k, v in list(missing.items())[:10]:
        print(f"  {k}: {v}")
    sys.exit(1)

# Write master translations.csv
target_csv = os.path.normpath('be/src/main/resources/db/changelog/data/insert/files/translations.csv')
print(f"Writing {len(keys_dict) * 2} rows to {target_csv}...")

with open(target_csv, 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f, delimiter=';', quotechar='"', quoting=csv.QUOTE_MINIMAL)
    writer.writerow(['id', 'translation_key', 'locale', 'text_value'])
    seq = 1
    for k in sorted(keys_dict.keys()):
        for loc in ('ar-EG', 'en-US'):
            suffix = 'ar' if loc == 'ar-EG' else 'en'
            row_id = f't-{seq:06d}-{suffix}'
            val = keys_dict[k][loc]
            writer.writerow([row_id, k, loc, val])
        seq += 1

print("Master translations.csv written successfully!")
