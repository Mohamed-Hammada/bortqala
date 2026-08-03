import re

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_loop = '''            Set<String> workerCodesToFetch = new java.util.HashSet<>();
            Map<String, Integer> headerIndexes = new HashMap<>();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String val = text(headerRow, i, formatter);
                    if (val != null && !val.isBlank()) {
                        headerIndexes.put(val.strip(), i);
                    }
                }
            }
            Integer workerCodeIndex = headerIndexes.get(mapping.get("workerCode"));

            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String rawCode = text(row, workerCodeIndex, formatter);
                if (rawCode != null && !rawCode.isBlank()) {
                    workerCodesToFetch.add(rawCode.strip().toUpperCase(Locale.ROOT));
                }
            }'''

new_loop = '''            Set<String> workerCodesToFetch = new java.util.HashSet<>();
            Integer workerCodeIndex = indexes.get(mapping.get("workerCode"));
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String rawCode = text(row, workerCodeIndex, formatter);
                if (rawCode != null && !rawCode.isBlank()) {
                    workerCodesToFetch.add(rawCode.strip().toUpperCase(Locale.ROOT));
                }
            }'''

content = content.replace(old_loop, new_loop)

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'w', encoding='utf-8') as f:
    f.write(content)
