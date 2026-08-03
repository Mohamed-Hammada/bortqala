import re

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'r', encoding='utf-8') as f:
    content = f.read()

# I see it uses `indexes.get(mapping.get("..."))` instead of `headerIndexes` let me check the file properly
