package com.bemo.hr.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ArabicAmountInWords {

    private ArabicAmountInWords() {}

    private static final String[] UNITS = {
        "صفر", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"
    };

    private static final String[] TEENS = {
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر",
        "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    };

    private static final String[] TENS = {
        "", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون",
        "ستون", "سبعون", "ثمانون", "تسعون"
    };

    private static final String[] HUNDREDS = {
        "", "مائة", "مائتان", "ثلاثمئة", "أربعمائة", "خمسمائة",
        "ستمئة", "سبعمائة", "ثمانمئة", "تسعمائة"
    };

    private static final String ZERO_TEXT = "صفر ج.م";

    public static String convert(BigDecimal amount) {
        if (amount == null) {
            return ZERO_TEXT;
        }

        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal absAmount = rounded.abs();
        long whole = absAmount.longValue();
        int fraction = absAmount.subtract(BigDecimal.valueOf(whole))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        if (whole == 0 && fraction == 0) {
            return rounded.signum() < 0 ? "سالب " + ZERO_TEXT : ZERO_TEXT;
        }

        StringBuilder sb = new StringBuilder();
        if (rounded.signum() < 0) {
            sb.append("سالب ");
        }

        if (whole > 0) {
            sb.append(convertWhole(whole));
        }

        if (fraction > 0) {
            if (whole > 0) {
                sb.append(" و");
            }
            sb.append(convertWhole(fraction));
            sb.append(" قرش");
        }

        sb.append(" ج.م");
        return sb.toString();
    }

    static String convertWhole(long number) {
        if (number == 0) {
            return UNITS[0];
        }

        StringBuilder sb = new StringBuilder();
        long remaining = number;
        boolean hasMillions = remaining >= 1_000_000;

        if (remaining >= 1_000_000) {
            long millions = remaining / 1_000_000;
            remaining %= 1_000_000;
            appendGroup(sb, millions, "مليون", "مليونان", "ملايين");
        }

        if (remaining >= 1_000) {
            long thousands = remaining / 1_000;
            remaining %= 1_000;
            if (hasMillions || thousands >= 100) {
                appendCompoundThousands(sb, thousands);
            } else {
                appendGroup(sb, thousands, "ألف", "ألفان", "آلاف");
            }
        }

        if (remaining >= 100) {
            long hundreds = remaining / 100;
            remaining %= 100;
            if (sb.length() > 0 && hundreds == 1 && remaining == 0) {
                sb.append(" وال").append(HUNDREDS[(int) hundreds]);
            } else if (sb.length() > 0) {
                sb.append(" و").append(HUNDREDS[(int) hundreds]);
            } else {
                sb.append(HUNDREDS[(int) hundreds]);
            }
        }

        if (remaining >= 10) {
            long tens = remaining / 10;
            long ones = remaining % 10;
            if (ones == 0) {
                if (sb.length() > 0) sb.append(" و");
                sb.append(TENS[(int) tens]);
            } else {
                if (remaining < 20) {
                    if (sb.length() > 0) sb.append(" و");
                    sb.append(TEENS[(int) (remaining - 10)]);
                } else {
                    if (sb.length() > 0) sb.append(" و");
                    sb.append(UNITS[(int) ones]);
                    sb.append(" و").append(TENS[(int) tens]);
                }
            }
        } else if (remaining > 0) {
            if (sb.length() > 0) sb.append(" و");
            sb.append(UNITS[(int) remaining]);
        }

        return sb.toString();
    }

    private static void appendCompoundThousands(StringBuilder sb, long count) {
        String word;
        if (count == 1) {
            word = "ألف";
        } else if (count == 2) {
            word = "ألفان";
        } else if (count % 100 == 0) {
            word = convertArabicNumber(count) + " ألف";
        } else if (count % 100 < 10) {
            word = convertArabicNumber(count) + " ألفاً";
        } else {
            word = convertArabicNumber(count) + " ألفاً";
        }
        if (sb.length() > 0) {
            sb.append(" و");
        }
        sb.append(word);
    }

    private static void appendGroup(StringBuilder sb, long count, String singular, String dual, String plural) {
        String word;
        if (count == 1) {
            word = singular;
        } else if (count == 2) {
            word = dual;
        } else {
            word = convertArabicNumber(count) + " " + plural;
        }

        if (sb.length() > 0) {
            sb.append(" و");
        }
        sb.append(word);
    }

    static String convertArabicNumber(long number) {
        if (number < 10) return UNITS[(int) number];

        StringBuilder sb = new StringBuilder();
        long remaining = number;

        if (remaining >= 100) {
            long hundreds = remaining / 100;
            remaining %= 100;
            sb.append(HUNDREDS[(int) hundreds]);
        }

        if (remaining >= 10) {
            long tens = remaining / 10;
            long ones = remaining % 10;
            if (ones == 0) {
                if (sb.length() > 0) sb.append(" و");
                sb.append(TENS[(int) tens]);
            } else {
                if (remaining < 20) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(TEENS[(int) (remaining - 10)]);
                } else {
                    if (sb.length() > 0) sb.append(" و");
                    sb.append(UNITS[(int) ones]);
                    sb.append(" و").append(TENS[(int) tens]);
                }
            }
        } else if (remaining > 0) {
            if (sb.length() > 0) sb.append(" و");
            sb.append(UNITS[(int) remaining]);
        }

        return sb.toString();
    }
}
