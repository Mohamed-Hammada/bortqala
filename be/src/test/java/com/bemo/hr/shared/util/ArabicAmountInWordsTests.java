package com.bemo.hr.shared.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ArabicAmountInWordsTests {

    @Test
    void zeroReturnsZeroPounds() {
        assertEquals("صفر ج.م", ArabicAmountInWords.convert(BigDecimal.ZERO));
    }

    @Test
    void onePound() {
        assertEquals("واحد ج.م", ArabicAmountInWords.convert(BigDecimal.ONE));
    }

    @Test
    void twoPounds() {
        assertEquals("اثنان ج.م", ArabicAmountInWords.convert(new BigDecimal("2")));
    }

    @Test
    void threePounds() {
        assertEquals("ثلاثة ج.م", ArabicAmountInWords.convert(new BigDecimal("3")));
    }

    @Test
    void fourPounds() {
        assertEquals("أربعة ج.م", ArabicAmountInWords.convert(new BigDecimal("4")));
    }

    @Test
    void fivePounds() {
        assertEquals("خمسة ج.م", ArabicAmountInWords.convert(new BigDecimal("5")));
    }

    @Test
    void sixPounds() {
        assertEquals("ستة ج.م", ArabicAmountInWords.convert(new BigDecimal("6")));
    }

    @Test
    void sevenPounds() {
        assertEquals("سبعة ج.م", ArabicAmountInWords.convert(new BigDecimal("7")));
    }

    @Test
    void eightPounds() {
        assertEquals("ثمانية ج.م", ArabicAmountInWords.convert(new BigDecimal("8")));
    }

    @Test
    void ninePounds() {
        assertEquals("تسعة ج.م", ArabicAmountInWords.convert(new BigDecimal("9")));
    }

    @Test
    void tenPounds() {
        assertEquals("عشرة ج.م", ArabicAmountInWords.convert(new BigDecimal("10")));
    }

    @Test
    void elevenPounds() {
        assertEquals("أحد عشر ج.م", ArabicAmountInWords.convert(new BigDecimal("11")));
    }

    @Test
    void fifteenPounds() {
        assertEquals("خمسة عشر ج.م", ArabicAmountInWords.convert(new BigDecimal("15")));
    }

    @Test
    void nineteenPounds() {
        assertEquals("تسعة عشر ج.م", ArabicAmountInWords.convert(new BigDecimal("19")));
    }

    @Test
    void twentyPounds() {
        assertEquals("عشرون ج.م", ArabicAmountInWords.convert(new BigDecimal("20")));
    }

    @Test
    void twentyFivePounds() {
        assertEquals("خمسة وعشرون ج.م", ArabicAmountInWords.convert(new BigDecimal("25")));
    }

    @Test
    void thirtyPounds() {
        assertEquals("ثلاثون ج.م", ArabicAmountInWords.convert(new BigDecimal("30")));
    }

    @Test
    void fortyTwoPounds() {
        assertEquals("اثنان وأربعون ج.م", ArabicAmountInWords.convert(new BigDecimal("42")));
    }

    @Test
    void fiftyPounds() {
        assertEquals("خمسون ج.م", ArabicAmountInWords.convert(new BigDecimal("50")));
    }

    @Test
    void ninetyNinePounds() {
        assertEquals("تسعة وتسعون ج.م", ArabicAmountInWords.convert(new BigDecimal("99")));
    }

    @Test
    void oneHundredPounds() {
        assertEquals("مائة ج.م", ArabicAmountInWords.convert(new BigDecimal("100")));
    }

    @Test
    void twoHundredPounds() {
        assertEquals("مائتان ج.م", ArabicAmountInWords.convert(new BigDecimal("200")));
    }

    @Test
    void threeHundredPounds() {
        assertEquals("ثلاثمئة ج.م", ArabicAmountInWords.convert(new BigDecimal("300")));
    }

    @Test
    void fiveHundredPounds() {
        assertEquals("خمسمائة ج.م", ArabicAmountInWords.convert(new BigDecimal("500")));
    }

    @Test
    void nineHundredNinetyNinePounds() {
        assertEquals("تسعمائة وتسعة وتسعون ج.م", ArabicAmountInWords.convert(new BigDecimal("999")));
    }

    @Test
    void oneThousandPounds() {
        assertEquals("ألف ج.م", ArabicAmountInWords.convert(new BigDecimal("1000")));
    }

    @Test
    void twoThousandPounds() {
        assertEquals("ألفان ج.م", ArabicAmountInWords.convert(new BigDecimal("2000")));
    }

    @Test
    void fiveThousandPounds() {
        assertEquals("خمسة آلاف ج.م", ArabicAmountInWords.convert(new BigDecimal("5000")));
    }

    @Test
    void oneThousandTwoHundredThirtyFourPounds() {
        assertEquals("ألف ومائتان وأربعة وثلاثون ج.م", ArabicAmountInWords.convert(new BigDecimal("1234")));
    }

    @Test
    void tenThousandPounds() {
        assertEquals("عشرة آلاف ج.م", ArabicAmountInWords.convert(new BigDecimal("10000")));
    }

    @Test
    void oneHundredThousandPounds() {
        assertEquals("مائة ألف ج.م", ArabicAmountInWords.convert(new BigDecimal("100000")));
    }

    @Test
    void fiveHundredFiftyFiveThousand() {
        assertEquals("خمسمائة وخمسة وخمسون ألفاً وخمسمائة وخمسة وخمسون ج.م",
                ArabicAmountInWords.convert(new BigDecimal("555555")));
    }

    @Test
    void oneMillionPounds() {
        assertEquals("مليون ج.م", ArabicAmountInWords.convert(new BigDecimal("1000000")));
    }

    @Test
    void twoMillionPounds() {
        assertEquals("مليونان ج.م", ArabicAmountInWords.convert(new BigDecimal("2000000")));
    }

    @Test
    void fiveMillionPounds() {
        assertEquals("خمسة ملايين ج.م", ArabicAmountInWords.convert(new BigDecimal("5000000")));
    }

    @Test
    void largeNumber() {
        assertEquals("اثنا عشر ملايين وخمسمائة وثلاثة وثلاثون ألفاً وأربعمائة وخمسة وخمسون ج.م",
                ArabicAmountInWords.convert(new BigDecimal("12533455")));
    }

    @Test
    void decimalRoundsToTwoPlaces() {
        assertEquals("واحد وعشرون وتسعة وأربعون قرش ج.م", ArabicAmountInWords.convert(new BigDecimal("21.49")));
    }

    @Test
    void withFractionalPiasters() {
        assertEquals("واحد وعشرة قرش ج.م", ArabicAmountInWords.convert(new BigDecimal("1.10")));
    }

    @Test
    void withFiftyPiasters() {
        assertEquals("واحد وخمسون قرش ج.م", ArabicAmountInWords.convert(new BigDecimal("1.50")));
    }

    @Test
    void zeroPointZeroOne() {
        assertEquals("عشرة قرش ج.م", ArabicAmountInWords.convert(new BigDecimal("0.10")));
    }

    @Test
    void nullReturnsZero() {
        assertEquals("صفر ج.م", ArabicAmountInWords.convert(null));
    }

    @Test
    void negativeOnePound() {
        assertEquals("سالب واحد ج.م", ArabicAmountInWords.convert(new BigDecimal("-1")));
    }

    @Test
    void negativeLargeAmount() {
        assertEquals("سالب مليون ج.م", ArabicAmountInWords.convert(new BigDecimal("-1000000")));
    }

    @Test
    void nineHundredNinetyNineThousand() {
        assertEquals("تسعمائة وتسعة وتسعون ألفاً وتسعمائة وتسعة وتسعون ج.م",
                ArabicAmountInWords.convert(new BigDecimal("999999")));
    }

    @Test
    void oneHundredOnePounds() {
        assertEquals("مائة وواحد ج.م", ArabicAmountInWords.convert(new BigDecimal("101")));
    }

    @Test
    void oneHundredElevenPounds() {
        assertEquals("مائة وأحد عشر ج.م", ArabicAmountInWords.convert(new BigDecimal("111")));
    }

    @Test
    void sevenHundredSeventySevenPounds() {
        assertEquals("سبعمائة وسبعة وسبعون ج.م", ArabicAmountInWords.convert(new BigDecimal("777")));
    }

    @Test
    void thousandWithFraction() {
        assertEquals("ألف وخمسة وعشرون قرش ج.م", ArabicAmountInWords.convert(new BigDecimal("1000.25")));
    }

    @Test
    void nineHundredAndNinePounds() {
        assertEquals("تسعمائة وتسعة ج.م", ArabicAmountInWords.convert(new BigDecimal("909")));
    }

    @Test
    void sixHundredAndSixPounds() {
        assertEquals("ستمئة وستة ج.م", ArabicAmountInWords.convert(new BigDecimal("606")));
    }

    @Test
    void fourHundredFortyFourPounds() {
        assertEquals("أربعمائة وأربعة وأربعون ج.م", ArabicAmountInWords.convert(new BigDecimal("444")));
    }

    @Test
    void convertArabicNumberBasic() {
        assertEquals("واحد", ArabicAmountInWords.convertArabicNumber(1));
    }

    @Test
    void convertArabicNumberEleven() {
        assertEquals("أحد عشر", ArabicAmountInWords.convertArabicNumber(11));
    }

    @Test
    void convertArabicNumberThirty() {
        assertEquals("ثلاثون", ArabicAmountInWords.convertArabicNumber(30));
    }
}
