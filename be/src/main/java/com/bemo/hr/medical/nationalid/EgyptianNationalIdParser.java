package com.bemo.hr.medical.nationalid;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

/**
 * Pure parser and validator for 14-digit Egyptian National Identification Numbers.
 *
 * <p>Format breakdown:
 * <ul>
 *   <li>Digit 1: Century indicator (2 = 1900-1999, 3 = 2000-2099)</li>
 *   <li>Digits 2-7: Birth Date in YYMMDD format</li>
 *   <li>Digits 8-9: Governorate code (e.g. 01 = Cairo, 02 = Alexandria, 21 = Giza)</li>
 *   <li>Digits 10-13: Unique sequence per governorate/date (Digit 13: odd = MALE, even = FEMALE)</li>
 *   <li>Digit 14: Verification digit</li>
 * </ul>
 */
public final class EgyptianNationalIdParser {

    private static final Set<String> GOVERNORATE_CODES = Set.of(
            "01", "02", "03", "04",
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "23", "24", "25", "26", "27", "28", "29",
            "31", "32", "33", "34", "35",
            "88"
    );

    private static final Map<String, String> GOVERNORATE_NAMES = Map.ofEntries(
            Map.entry("01", "Cairo"),
            Map.entry("02", "Alexandria"),
            Map.entry("03", "Port Said"),
            Map.entry("04", "Suez"),
            Map.entry("11", "Damietta"),
            Map.entry("12", "Dakahlia"),
            Map.entry("13", "Ash Sharqia"),
            Map.entry("14", "Al Qalyubia"),
            Map.entry("15", "Kafr El Sheikh"),
            Map.entry("16", "Gharbia"),
            Map.entry("17", "Menoufia"),
            Map.entry("18", "Beheira"),
            Map.entry("19", "Ismailia"),
            Map.entry("21", "Giza"),
            Map.entry("22", "Beni Suef"),
            Map.entry("23", "Faiyum"),
            Map.entry("24", "Minya"),
            Map.entry("25", "Asyut"),
            Map.entry("26", "Sohag"),
            Map.entry("27", "Qena"),
            Map.entry("28", "Aswan"),
            Map.entry("29", "Luxor"),
            Map.entry("31", "Red Sea"),
            Map.entry("32", "New Valley"),
            Map.entry("33", "Matrouh"),
            Map.entry("34", "North Sinai"),
            Map.entry("35", "South Sinai"),
            Map.entry("88", "Born Abroad")
    );

    private EgyptianNationalIdParser() {}

    public record ParseResult(
            boolean valid,
            String nationalId,
            LocalDate birthDate,
            String gender,
            String governorateCode,
            String governorateName,
            String errorMessage
    ) {
        public static ParseResult invalid(String nationalId, String errorMessage) {
            return new ParseResult(false, nationalId, null, null, null, null, errorMessage);
        }

        public static ParseResult success(String nationalId, LocalDate birthDate, String gender, String governorateCode, String governorateName) {
            return new ParseResult(true, nationalId, birthDate, gender, governorateCode, governorateName, null);
        }
    }

    public static ParseResult parse(String nationalId) {
        if (nationalId == null) {
            return ParseResult.invalid(null, "National ID is null");
        }
        String cleaned = nationalId.trim();
        if (cleaned.length() != 14 || !cleaned.matches("\\d{14}")) {
            return ParseResult.invalid(cleaned, "National ID must be exactly 14 digits");
        }

        char centuryChar = cleaned.charAt(0);
        int centuryBase;
        if (centuryChar == '2') {
            centuryBase = 1900;
        } else if (centuryChar == '3') {
            centuryBase = 2000;
        } else {
            return ParseResult.invalid(cleaned, "Invalid century digit: " + centuryChar);
        }

        int year = centuryBase + Integer.parseInt(cleaned.substring(1, 3));
        int month = Integer.parseInt(cleaned.substring(3, 5));
        int day = Integer.parseInt(cleaned.substring(5, 7));

        LocalDate birthDate;
        try {
            birthDate = LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException ex) {
            return ParseResult.invalid(cleaned, "Invalid birth date in National ID: " + year + "-" + month + "-" + day);
        }

        if (birthDate.isAfter(LocalDate.now())) {
            return ParseResult.invalid(cleaned, "Birth date cannot be in the future: " + birthDate);
        }

        String governorateCode = cleaned.substring(7, 9);
        if (!GOVERNORATE_CODES.contains(governorateCode)) {
            return ParseResult.invalid(cleaned, "Invalid governorate code: " + governorateCode);
        }
        String governorateName = GOVERNORATE_NAMES.getOrDefault(governorateCode, "Unknown");

        int genderDigit = Character.getNumericValue(cleaned.charAt(12));
        String gender = (genderDigit % 2 != 0) ? "MALE" : "FEMALE";

        return ParseResult.success(cleaned, birthDate, gender, governorateCode, governorateName);
    }
}
