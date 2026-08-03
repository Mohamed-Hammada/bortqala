import re

with open('be/src/test/resources/db/changelog/releases/test-h2.changelog-master.yaml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('PRIMARY KEY (app_id, feature_key)', 'PRIMARY KEY (app_id, feature_key),\n                CONSTRAINT fk_tenant_features_app FOREIGN KEY (app_id) REFERENCES apps (id) ON DELETE CASCADE ON UPDATE NO ACTION')

with open('be/src/test/resources/db/changelog/releases/test-h2.changelog-master.yaml', 'w', encoding='utf-8') as f:
    f.write(content)

