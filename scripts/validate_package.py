#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
import json, sys
import yaml

ROOT=Path(__file__).resolve().parents[1]
HUB=ROOT/'device-hub'
errors=[]

for file in ROOT.rglob('*.yaml'):
    try: yaml.safe_load(file.read_text(encoding='utf-8'))
    except Exception as exc: errors.append(f'YAML {file.relative_to(ROOT)}: {exc}')
for file in ROOT.rglob('*.json'):
    try: json.loads(file.read_text(encoding='utf-8'))
    except Exception as exc: errors.append(f'JSON {file.relative_to(ROOT)}: {exc}')

for supplier_dir in sorted((HUB/'packages/suppliers').iterdir()):
    if not supplier_dir.is_dir(): continue
    supplier=supplier_dir.name
    integrations={p.name for p in (supplier_dir/'integrations').iterdir() if p.is_dir()}
    adapters={p.stem.replace('_','-') for p in (supplier_dir/'src'/f'supplier_{supplier}'/'adapters').glob('*.py') if p.stem not in {'base','__init__'}}
    for route in integrations:
        if not (supplier_dir/'integrations'/route/'README.md').exists():
            errors.append(f'{supplier}/{route}: missing integration README')
        if route not in adapters:
            errors.append(f'{supplier}/{route}: missing adapter module')
    catalog=json.loads((supplier_dir/'profiles/integration_versions.json').read_text(encoding='utf-8'))
    for row in catalog:
        route=row.get('route','')
        if route not in integrations: errors.append(f'{supplier}/{route}: version catalog route has no integration directory')
        if not row.get('official_documentation'): errors.append(f'{supplier}/{route}: no official documentation URL')

if errors:
    print('\n'.join(errors)); sys.exit(1)
print('OK: YAML/JSON valid; every supplier route has adapter, README and documentation metadata.')
