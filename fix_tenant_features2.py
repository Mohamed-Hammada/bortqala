import re

with open('be/src/main/resources/db/changelog/releases/next.changelog-master.yaml', 'r', encoding='utf-8') as f:
    content = f.read()

content += '''  - include:
      file: db/changelog/schema/create/20260803_v87_tenant_features.yaml
'''

with open('be/src/main/resources/db/changelog/releases/next.changelog-master.yaml', 'w', encoding='utf-8') as f:
    f.write(content)
