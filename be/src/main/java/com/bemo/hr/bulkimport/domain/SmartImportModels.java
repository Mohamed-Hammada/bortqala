package com.bemo.hr.bulkimport.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SmartImportModels {
    private SmartImportModels() {}

    public enum ColumnType { STRING, DATE, DECIMAL, INTEGER, BOOLEAN, ENUM }

    public record Column(
            String key,
            String headerEn,
            String headerAr,
            ColumnType type,
            boolean required,
            List<String> allowedValues,
            List<String> aliases) {
        public Column {
            allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    public record Sheet(
            String key,
            String titleEn,
            String titleAr,
            List<Column> columns) {
        public Sheet { columns = List.copyOf(columns); }
    }

    public record Workflow(
            String key,
            String titleEn,
            String titleAr,
            String route,
            String templateFileName,
            String priority,
            boolean domainCommitAvailable,
            List<Sheet> sheets) {
        public Workflow { sheets = List.copyOf(sheets); }
    }

    public record CellError(
            int rowNumber,
            String sheet,
            String column,
            String messageEn,
            String messageAr) {}

    public record PreviewRow(
            int rowNumber,
            String sheet,
            Map<String, String> values,
            List<CellError> errors) {
        public PreviewRow {
            values = Map.copyOf(values);
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }

    public record Preview(
            UUID previewId,
            Workflow workflow,
            String fileName,
            int totalRows,
            int validRows,
            int errorRows,
            List<PreviewRow> rows,
            List<CellError> errors) {
        public Preview {
            rows = List.copyOf(rows);
            errors = List.copyOf(errors);
        }
    }

    public record EditedRow(int rowNumber, String sheet, Map<String, String> values) {
        public EditedRow { values = Map.copyOf(values); }
    }

    public record CommitRequest(UUID previewId, boolean skipInvalid, List<EditedRow> rows) {
        public CommitRequest { rows = rows == null ? List.of() : List.copyOf(rows); }
    }

    public record CommitResult(
            UUID batchId,
            String workflow,
            String status,
            boolean persisted,
            int committedRows,
            int rejectedRows,
            String messageEn,
            String messageAr,
            List<CellError> errors) {
        public CommitResult { errors = errors == null ? List.of() : List.copyOf(errors); }
    }

    public record HandlerOutcome(
            boolean persisted,
            int committedRows,
            int rejectedRows,
            String messageEn,
            String messageAr,
            List<CellError> errors) {
        public HandlerOutcome { errors = errors == null ? List.of() : List.copyOf(errors); }
    }
}
