package com.bemo.hr.shared.dataexchange;

import java.util.List;
import java.util.Map;

public record SpreadsheetTemplateDefinition(
        String key,
        String module,
        String title,
        String workspaceRoute,
        String description,
        boolean commitSupported,
        List<SheetDefinition> sheets) {

    public enum ColumnType {
        TEXT,
        INTEGER,
        DECIMAL,
        DATE,
        DATETIME,
        BOOLEAN
    }

    public record SheetDefinition(
            String name,
            List<ColumnDefinition> columns,
            List<Map<String, Object>> sampleRows) {
    }

    public record ColumnDefinition(
            String key,
            String header,
            boolean required,
            ColumnType type,
            boolean uniqueWithinFile,
            List<String> allowedValues,
            String example,
            String description) {

        public ColumnDefinition {
            allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
        }
    }
}
