package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class SmartImportValidator {

    public ValidationResult validate(Workflow workflow, List<PreviewRow> rawRows) {
        var normalizedRows = new ArrayList<PreviewRow>();
        var allErrors = new ArrayList<CellError>();
        for (var row : rawRows) {
            var sheet = workflow.sheets().stream().filter(s -> s.key().equals(row.sheet())).findFirst().orElse(null);
            if (sheet == null) continue;
            var errors = new ArrayList<CellError>();
            for (var column : sheet.columns()) validateCell(row, column, errors);
            normalizedRows.add(new PreviewRow(row.rowNumber(), row.sheet(), row.values(), errors));
            allErrors.addAll(errors);
        }

        var crossErrors = validateCrossRow(workflow, normalizedRows);
        if (!crossErrors.isEmpty()) {
            allErrors.addAll(crossErrors);
            var byRow = new HashMap<String, List<CellError>>();
            for (var error : crossErrors)
                byRow.computeIfAbsent(error.sheet() + "#" + error.rowNumber(), k -> new ArrayList<>()).add(error);
            var rebuilt = new ArrayList<PreviewRow>();
            for (var row : normalizedRows) {
                var errors = new ArrayList<>(row.errors());
                errors.addAll(byRow.getOrDefault(row.sheet() + "#" + row.rowNumber(), List.of()));
                rebuilt.add(new PreviewRow(row.rowNumber(), row.sheet(), row.values(), errors));
            }
            normalizedRows = rebuilt;
        }
        return new ValidationResult(normalizedRows, allErrors);
    }

    private void validateCell(PreviewRow row, Column column, List<CellError> errors) {
        var value = clean(row.values().get(column.key()));
        if (column.required() && value.isBlank()) {
            errors.add(error(row, column.key(), "Required value is missing.", "Required value is missing."));
            return;
        }
        if (value.isBlank()) return;
        try {
            switch (column.type()) {
                case DATE -> LocalDate.parse(value);
                case DECIMAL -> new BigDecimal(value.replace(",", ""));
                case INTEGER -> Integer.parseInt(value);
                case BOOLEAN -> {
                    var normalized = value.toUpperCase(Locale.ROOT);
                    if (!Set.of("TRUE", "FALSE", "YES", "NO", "1", "0").contains(normalized)) {
                        throw new IllegalArgumentException("boolean");
                    }
                }
                case ENUM -> {
                    boolean valid = column.allowedValues().stream().anyMatch(allowed -> allowed.equalsIgnoreCase(value));
                    if (!valid) throw new IllegalArgumentException("enum");
                }
                default -> {
                }
            }
        } catch (NumberFormatException ex) {
            errors.add(error(row, column.key(), "Expected a valid number.", "Expected a valid number."));
        } catch (DateTimeParseException ex) {
            errors.add(error(row, column.key(), "Expected date format YYYY-MM-DD.", "Expected date format YYYY-MM-DD."));
        } catch (IllegalArgumentException ex) {
            if (column.type() == ColumnType.BOOLEAN) {
                errors.add(error(row, column.key(), "Expected TRUE or FALSE.", "Expected TRUE or FALSE."));
            } else if (column.type() == ColumnType.ENUM) {
                errors.add(error(row, column.key(), "Allowed values: " + String.join(", ", column.allowedValues()),
                        "Allowed values: " + String.join(", ", column.allowedValues())));
            }
        }
    }

    private List<CellError> validateCrossRow(Workflow workflow, List<PreviewRow> rows) {
        return switch (workflow.key()) {
            case "employees" -> validateEmployees(rows);
            case "accounts" -> validateAccounts(rows);
            case "journal-entries" -> validateJournals(rows);
            case "bank-statements" -> validateBankStatement(rows);
            case "budgets" -> validateBudgets(rows);
            case "parties" ->
                    validateDuplicates(rows, "parties", List.of("PartyCode", "TaxRegistrationNumber", "Mobile"));
            case "items" -> validateItems(rows);
            case "stock-count" -> validateStockCount(rows);
            case "bom-routing" -> validateBom(rows);
            case "shift-roster" -> validateShiftRoster(rows);
            default -> List.of();
        };
    }

    private List<CellError> validateEmployees(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        errors.addAll(validateDuplicates(rows, "personal", List.of("EmployeeCode", "NationalId", "PersonalEmail")));
        var personalCodes = new HashSet<String>();
        for (var row : rows)
            if (row.sheet().equals("personal")) {
                var code = clean(row.values().get("EmployeeCode"));
                if (!code.isBlank()) personalCodes.add(code.toLowerCase(Locale.ROOT));
                var nationalId = clean(row.values().get("NationalId"));
                if (!nationalId.isBlank() && nationalId.chars().allMatch(Character::isDigit) && nationalId.length() != 14) {
                    errors.add(error(row, "NationalId", "Egyptian National ID must contain 14 digits (passport values may be alphanumeric).",
                            "Egyptian National ID must contain 14 digits (passport values may be alphanumeric)."));
                }
                var hasName = !clean(row.values().get("FirstNameAr")).isBlank()
                        || !clean(row.values().get("FirstNameEn")).isBlank();
                if (!hasName)
                    errors.add(error(row, "FirstNameAr", "At least an Arabic or English first name is required.", "At least an Arabic or English first name is required."));
            }
        for (var row : rows)
            if (!row.sheet().equals("personal")) {
                var code = clean(row.values().get("EmployeeCode"));
                if (!code.isBlank() && !personalCodes.contains(code.toLowerCase(Locale.ROOT))) {
                    errors.add(error(row, "EmployeeCode", "EmployeeCode must exist in the Personal sheet.", "EmployeeCode must exist in the Personal sheet."));
                }
            }
        return errors;
    }

    private List<CellError> validateAccounts(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        errors.addAll(validateDuplicates(rows, "accounts", List.of("AccountCode")));
        var seen = new HashSet<String>();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (var row : rows)
            if (row.sheet().equals("accounts")) {
                var code = clean(row.values().get("AccountCode"));
                var parent = clean(row.values().get("ParentAccountCode"));
                if (!parent.isBlank() && !seen.contains(parent.toLowerCase(Locale.ROOT))) {
                    errors.add(error(row, "ParentAccountCode", "Parent account must exist earlier in the file.", "Parent account must exist earlier in the file."));
                }
                if (!code.isBlank()) seen.add(code.toLowerCase(Locale.ROOT));
                var d = decimal(row.values().get("DebitOpeningBalance"));
                var c = decimal(row.values().get("CreditOpeningBalance"));
                debit = debit.add(d);
                credit = credit.add(c);
                boolean posting = bool(row.values().get("IsPostingAllowed"));
                if (!posting && (d.signum() != 0 || c.signum() != 0)) {
                    errors.add(error(row, "DebitOpeningBalance", "Opening balances are allowed only on posting/leaf accounts.", "Opening balances are allowed only on posting/leaf accounts."));
                }
            }
        if (debit.compareTo(credit) != 0 && !rows.isEmpty()) {
            var row = rows.stream().filter(r -> r.sheet().equals("accounts")).findFirst().orElse(rows.get(0));
            errors.add(error(row, "DebitOpeningBalance", "Opening debits must equal opening credits. Debit=" + debit + ", Credit=" + credit,
                    "Opening debits must equal opening credits. Debit=" + debit + ", Credit=" + credit));
        }
        return errors;
    }

    private List<CellError> validateJournals(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        var balances = new LinkedHashMap<String, BigDecimal[]>();
        var first = new HashMap<String, PreviewRow>();
        for (var row : rows)
            if (row.sheet().equals("journals")) {
                String ref = clean(row.values().get("JournalBatchRef"));
                if (ref.isBlank()) continue;
                var balance = balances.computeIfAbsent(ref, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                balance[0] = balance[0].add(decimal(row.values().get("DebitAmount")));
                balance[1] = balance[1].add(decimal(row.values().get("CreditAmount")));
                first.putIfAbsent(ref, row);
                if (decimal(row.values().get("DebitAmount")).signum() != 0 && decimal(row.values().get("CreditAmount")).signum() != 0) {
                    errors.add(error(row, "DebitAmount", "A journal line cannot contain both debit and credit amounts.", "A journal line cannot contain both debit and credit amounts."));
                }
            }
        balances.forEach((ref, balance) -> {
            if (balance[0].compareTo(balance[1]) != 0) {
                errors.add(error(first.get(ref), "DebitAmount", "Journal batch " + ref + " is not balanced.", "Journal batch " + ref + " is not balanced."));
            }
        });
        return errors;
    }

    private List<CellError> validateBankStatement(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        errors.addAll(validateDuplicates(rows, "statement", List.of("BankTransactionRef")));
        BigDecimal previousBalance = null;
        for (var row : rows)
            if (row.sheet().equals("statement")) {
                var debit = decimal(row.values().get("DebitAmount"));
                var credit = decimal(row.values().get("CreditAmount"));
                var current = optionalDecimal(row.values().get("BalanceAfter"));
                if (debit.signum() != 0 && credit.signum() != 0) {
                    errors.add(error(row, "DebitAmount", "A bank transaction cannot be both debit and credit.", "A bank transaction cannot be both debit and credit."));
                }
                if (previousBalance != null && current != null) {
                    var expected = previousBalance.add(credit).subtract(debit);
                    if (expected.compareTo(current) != 0) {
                        errors.add(error(row, "BalanceAfter", "Running balance is inconsistent; expected " + expected + ".", "Running balance is inconsistent; expected " + expected + "."));
                    }
                }
                if (current != null) previousBalance = current;
            }
        return errors;
    }

    private List<CellError> validateBudgets(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        for (var row : rows)
            if (row.sheet().equals("budget")) {
                BigDecimal sum = BigDecimal.ZERO;
                for (String month : List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")) {
                    sum = sum.add(decimal(row.values().get(month + "_Amount")));
                }
                var total = optionalDecimal(row.values().get("TotalAnnualBudget"));
                if (total != null && total.compareTo(sum) != 0) {
                    errors.add(error(row, "TotalAnnualBudget", "Annual total must equal the sum of monthly allocations (" + sum + ").",
                            "Annual total must equal the sum of monthly allocations (" + sum + ")."));
                }
            }
        return errors;
    }

    private List<CellError> validateItems(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        errors.addAll(validateDuplicates(rows, "items", List.of("ItemCode", "Barcode")));
        for (var row : rows)
            if (row.sheet().equals("items")) {
                var opening = decimal(row.values().get("OpeningQuantity"));
                if (opening.signum() > 0) {
                    if (clean(row.values().get("OpeningWarehouseCode")).isBlank())
                        errors.add(error(row, "OpeningWarehouseCode", "Warehouse is required when opening quantity is greater than zero.", "Warehouse is required when opening quantity is greater than zero."));
                    if (optionalDecimal(row.values().get("StandardCost")) == null)
                        errors.add(error(row, "StandardCost", "Standard cost is required when opening quantity is greater than zero.", "Standard cost is required when opening quantity is greater than zero."));
                    if (bool(row.values().get("IsLotTracked")) && clean(row.values().get("OpeningLotNumber")).isBlank())
                        errors.add(error(row, "OpeningLotNumber", "Lot number is required for lot-tracked opening stock.", "Lot number is required for lot-tracked opening stock."));
                }
            }
        return errors;
    }

    private List<CellError> validateStockCount(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        for (var row : rows)
            if (row.sheet().equals("count")) {
                var counted = optionalDecimal(row.values().get("CountedPhysicalQuantity"));
                if (counted != null && counted.signum() < 0)
                    errors.add(error(row, "CountedPhysicalQuantity", "Counted quantity cannot be negative.", "Counted quantity cannot be negative."));
            }
        return errors;
    }

    private List<CellError> validateBom(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        for (var row : rows)
            if (row.sheet().equals("bom")) {
                var parent = clean(row.values().get("ParentFinishedItemCode"));
                var component = clean(row.values().get("ComponentRawItemCode"));
                if (!parent.isBlank() && parent.equalsIgnoreCase(component))
                    errors.add(error(row, "ComponentRawItemCode", "An item cannot be a component of itself.", "An item cannot be a component of itself."));
            }
        return errors;
    }

    private List<CellError> validateShiftRoster(List<PreviewRow> rows) {
        var errors = new ArrayList<CellError>();
        for (var row : rows)
            if (row.sheet().equals("roster")) {
                int consecutiveNights = 0;
                for (int day = 1; day <= 31; day++) {
                    var value = clean(row.values().get(String.format("Day_%02d", day)));
                    if (value.equalsIgnoreCase("N")) consecutiveNights++;
                    else consecutiveNights = 0;
                    if (consecutiveNights > 6) {
                        errors.add(error(row, String.format("Day_%02d", day), "More than 6 consecutive night shifts; review fatigue/labor rules.", "More than 6 consecutive night shifts; review fatigue/labor rules."));
                        break;
                    }
                }
            }
        return errors;
    }

    private List<CellError> validateDuplicates(List<PreviewRow> rows, String sheet, List<String> keys) {
        var errors = new ArrayList<CellError>();
        for (var key : keys) {
            var seen = new HashMap<String, PreviewRow>();
            for (var row : rows)
                if (row.sheet().equals(sheet)) {
                    var value = clean(row.values().get(key));
                    if (value.isBlank()) continue;
                    var normalized = value.toLowerCase(Locale.ROOT);
                    if (seen.containsKey(normalized)) {
                        errors.add(error(row, key, "Duplicate value in import file: " + value, "Duplicate value in import file: " + value));
                    } else seen.put(normalized, row);
                }
        }
        return errors;
    }

    private CellError error(PreviewRow row, String column, String en, String ar) {
        return new CellError(row.rowNumber(), row.sheet(), column, en, ar);
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private BigDecimal decimal(String value) {
        var parsed = optionalDecimal(value);
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    private BigDecimal optionalDecimal(String value) {
        try {
            return clean(value).isBlank() ? null : new BigDecimal(clean(value).replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean bool(String value) {
        return Set.of("TRUE", "YES", "1").contains(clean(value).toUpperCase(Locale.ROOT));
    }

    public record ValidationResult(List<PreviewRow> rows, List<CellError> errors) {
    }
}
