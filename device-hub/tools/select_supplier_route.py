#!/usr/bin/env python3
import argparse, json, re
from pathlib import Path

p=argparse.ArgumentParser(); p.add_argument('supplier'); p.add_argument('model'); p.add_argument('--firmware',default=''); p.add_argument('--platform-version',default='')
a=p.parse_args()
root=Path(__file__).resolve().parents[1]
path=root/'packages'/'suppliers'/a.supplier/'profiles'/'device_routes.json'
rules=json.loads(path.read_text())
text=' '.join([a.model,a.firmware,a.platform_version])
unknown=None
for r in rules:
    if r['model_pattern']=='UNKNOWN': unknown=r; continue
    if re.search(r['model_pattern'], text, re.I): print(json.dumps(r,indent=2)); raise SystemExit
print(json.dumps(unknown or {},indent=2))
