package com.bemo.hr.medical;

import com.bemo.hr.medical.nationalid.EgyptianNationalIdParser;
import com.bemo.hr.medical.nationalid.EgyptianNationalIdParser.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EgyptianNationalIdParserTests {

    @Test
    @DisplayName("Century 2 (1900s) male born in Cairo")
    void testCentury2MaleCairo() {
        // 2 85 07 15 01 0234 5 -> 1985-07-15, Cairo (01), seq 023 (Digit 13 is '4' -> FEMALE? wait: index 12 is '4')
        // Let's check: 2 85 07 15 01 0231 5 -> index 12 is '1' -> odd -> MALE
        String nationalId = "28507150102315";
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);

        assertTrue(result.valid());
        assertEquals(LocalDate.of(1985, 7, 15), result.birthDate());
        assertEquals("MALE", result.gender());
        assertEquals("01", result.governorateCode());
        assertEquals("Cairo", result.governorateName());
    }

    @Test
    @DisplayName("Century 3 (2000s) female born in Giza")
    void testCentury3FemaleGiza() {
        // 3 04 11 20 21 0042 8 -> 2004-11-20, Giza (21), index 12 is '2' -> FEMALE
        String nationalId = "30411202100428";
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);

        assertTrue(result.valid());
        assertEquals(LocalDate.of(2004, 11, 20), result.birthDate());
        assertEquals("FEMALE", result.gender());
        assertEquals("21", result.governorateCode());
        assertEquals("Giza", result.governorateName());
    }

    @Test
    @DisplayName("Leap year Feb 29 (2000) valid")
    void testLeapYearValid() {
        String nationalId = "30002290200115"; // 2000-02-29, Alex (02), MALE
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);

        assertTrue(result.valid());
        assertEquals(LocalDate.of(2000, 2, 29), result.birthDate());
        assertEquals("Alexandria", result.governorateName());
    }

    @Test
    @DisplayName("Invalid Feb 29 in non-leap year (1999)")
    void testInvalidFeb29NonLeapYear() {
        String nationalId = "29902290100115"; // 1999-02-29 invalid
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);

        assertFalse(result.valid());
        assertNotNull(result.errorMessage());
    }

    @Test
    @DisplayName("Citizen born abroad (code 88)")
    void testBornAbroad() {
        String nationalId = "29205108801224"; // 1992-05-10, Abroad (88), FEMALE
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);

        assertTrue(result.valid());
        assertEquals(LocalDate.of(1992, 5, 10), result.birthDate());
        assertEquals("Born Abroad", result.governorateName());
        assertEquals("FEMALE", result.gender());
    }

    @ParameterizedTest
    @CsvSource({
            "28001019900115, Invalid governorate code: 99",
            "18001010100115, Invalid century digit: 1",
            "48001010100115, Invalid century digit: 4",
            "28513010100115, Invalid birth date in National ID: 1985-13-1",
            "28501320100115, Invalid birth date in National ID: 1985-1-32",
            "12345, National ID must be exactly 14 digits",
            "2850715010231A, National ID must be exactly 14 digits"
    })
    @DisplayName("Invalid formats and boundaries")
    void testInvalidCases(String nationalId, String expectedErrorSubstr) {
        ParseResult result = EgyptianNationalIdParser.parse(nationalId);
        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains(expectedErrorSubstr.split(":")[0]));
    }
}
