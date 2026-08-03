import re

with open('be/src/main/resources/db/changelog/schema/create/20260803_v87_tenant_features.yaml', 'r', encoding='utf-8') as f:
    content = f.read()

content += '''        - addForeignKeyConstraint:
            constraintName: fk_tenant_features_app
            baseTableName: tenant_features
            baseColumnNames: app_id
            referencedTableName: apps
            referencedColumnNames: id
            onDelete: CASCADE
            onUpdate: NO ACTION
      rollback:
        - dropForeignKeyConstraint:
            baseTableName: tenant_features
            constraintName: fk_tenant_features_app
        - dropTable:
            tableName: tenant_features
'''

with open('be/src/main/resources/db/changelog/schema/create/20260803_v87_tenant_features.yaml', 'w', encoding='utf-8') as f:
    f.write(content)
