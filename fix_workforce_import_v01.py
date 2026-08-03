import re

with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_loop = '''            Set<String> workerCodesToFetch = new java.util.HashSet<>();
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String rawCode = text(row, (mapping.get("workerCode") == null ? null : Integer.parseInt(mapping.get("workerCode"))), formatter);
                if (rawCode != null && !rawCode.isBlank()) {
                    workerCodesToFetch.add(rawCode.strip().toUpperCase(Locale.ROOT));
                }
            }'''

new_loop = '''            Set<String> workerCodesToFetch = new java.util.HashSet<>();
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

content = content.replace(old_loop, new_loop)

# Fix where mapping strings are directly passed to text() down below too
content = content.replace('text(row, (mapping.get("workerName") == null ? null : Integer.parseInt(mapping.get("workerName"))), formatter)', 'text(row, headerIndexes.get(mapping.get("workerName")), formatter)')
content = content.replace('text(row, (mapping.get("workDate") == null ? null : Integer.parseInt(mapping.get("workDate"))), formatter)', 'text(row, headerIndexes.get(mapping.get("workDate")), formatter)')
content = content.replace('text(row, (mapping.get("attendanceValue") == null ? null : Integer.parseInt(mapping.get("attendanceValue"))), formatter)', 'text(row, headerIndexes.get(mapping.get("attendanceValue")), formatter)')

# Also replace raw strings mapping if applicable (might be a bit more robust manually looking at the java file)
with open('be/src/main/java/com/bemo/hr/workforce/WorkforceExcelImportService.java', 'w', encoding='utf-8') as f:
    f.write(content)
