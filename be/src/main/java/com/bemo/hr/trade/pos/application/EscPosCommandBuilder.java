package com.bemo.hr.trade.pos.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Production-grade ESC/POS binary byte stream builder for 58mm and 80mm thermal receipt printers.
 */
public class EscPosCommandBuilder {

    // Control codes
    private static final byte ESC = 0x1B;
    private static final byte FS = 0x1C;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    private final Charset charset;
    private final int columnWidth;

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    public enum FontSize {
        NORMAL, DOUBLE_HEIGHT, DOUBLE_WIDTH, DOUBLE_BOTH
    }

    public EscPosCommandBuilder(int columnWidth, String codePageName) {
        this.columnWidth = columnWidth <= 0 ? 48 : columnWidth;
        Charset resolvedCharset;
        try {
            if ("CP864".equalsIgnoreCase(codePageName)) {
                resolvedCharset = Charset.forName("Cp864");
            } else if ("CP1256".equalsIgnoreCase(codePageName) || "windows-1256".equalsIgnoreCase(codePageName)) {
                resolvedCharset = Charset.forName("windows-1256");
            } else {
                resolvedCharset = StandardCharsets.UTF_8;
            }
        } catch (Exception ignored) {
            resolvedCharset = StandardCharsets.UTF_8;
        }
        this.charset = resolvedCharset;
    }

    public static EscPosCommandBuilder create80mm() {
        return new EscPosCommandBuilder(48, "CP864");
    }

    public static EscPosCommandBuilder create58mm() {
        return new EscPosCommandBuilder(32, "CP864");
    }

    public EscPosCommandBuilder initialize() {
        stream.write(ESC);
        stream.write('@');
        return this;
    }

    public EscPosCommandBuilder setCodePage(String codePage) {
        stream.write(ESC);
        stream.write('t');
        if ("CP864".equalsIgnoreCase(codePage)) {
            stream.write(37); // CP864
        } else if ("CP1256".equalsIgnoreCase(codePage)) {
            stream.write(47); // Windows-1256
        } else {
            stream.write(0);  // Standard Europe PC437
        }
        return this;
    }

    public EscPosCommandBuilder align(Alignment alignment) {
        stream.write(ESC);
        stream.write('a');
        switch (alignment) {
            case CENTER -> stream.write(1);
            case RIGHT -> stream.write(2);
            default -> stream.write(0);
        }
        return this;
    }

    public EscPosCommandBuilder bold(boolean enable) {
        stream.write(ESC);
        stream.write('E');
        stream.write(enable ? 1 : 0);
        return this;
    }

    public EscPosCommandBuilder underline(boolean enable) {
        stream.write(ESC);
        stream.write('-');
        stream.write(enable ? 1 : 0);
        return this;
    }

    public EscPosCommandBuilder fontSize(FontSize size) {
        stream.write(GS);
        stream.write('!');
        switch (size) {
            case DOUBLE_HEIGHT -> stream.write(0x01);
            case DOUBLE_WIDTH -> stream.write(0x10);
            case DOUBLE_BOTH -> stream.write(0x11);
            default -> stream.write(0x00);
        }
        return this;
    }

    public EscPosCommandBuilder text(String text) {
        if (text != null && !text.isEmpty()) {
            byte[] bytes = text.getBytes(charset);
            stream.write(bytes, 0, bytes.length);
        }
        return this;
    }

    public EscPosCommandBuilder line(String line) {
        text(line);
        stream.write(LF);
        return this;
    }

    public EscPosCommandBuilder feed(int lines) {
        for (int i = 0; i < lines; i++) {
            stream.write(LF);
        }
        return this;
    }

    public EscPosCommandBuilder separator(char ch) {
        StringBuilder sb = new StringBuilder(columnWidth);
        for (int i = 0; i < columnWidth; i++) {
            sb.append(ch);
        }
        return line(sb.toString());
    }

    public EscPosCommandBuilder rowTwoColumns(String left, String right) {
        if (left == null) left = "";
        if (right == null) right = "";
        int space = columnWidth - (left.length() + right.length());
        if (space < 1) {
            // Truncate left if it exceeds width
            int maxLeft = Math.max(1, columnWidth - right.length() - 1);
            if (left.length() > maxLeft) {
                left = left.substring(0, maxLeft);
            }
            space = Math.max(1, columnWidth - (left.length() + right.length()));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < space; i++) {
            sb.append(' ');
        }
        sb.append(right);
        return line(sb.toString());
    }

    public EscPosCommandBuilder rowThreeColumns(String col1, String col2, String col3) {
        if (col1 == null) col1 = "";
        if (col2 == null) col2 = "";
        if (col3 == null) col3 = "";

        // E.g. for 48 cols: col1=26, col2=10, col3=12
        // for 32 cols: col1=16, col2=6, col3=10
        int c1Width = (int) (columnWidth * 0.55);
        int c2Width = (int) (columnWidth * 0.20);
        int c3Width = columnWidth - c1Width - c2Width;

        if (col1.length() > c1Width) {
            col1 = col1.substring(0, c1Width);
        }
        if (col2.length() > c2Width) {
            col2 = col2.substring(0, c2Width);
        }
        if (col3.length() > c3Width) {
            col3 = col3.substring(0, c3Width);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(padRight(col1, c1Width));
        sb.append(padRight(col2, c2Width));
        sb.append(padLeft(col3, c3Width));
        return line(sb.toString());
    }

    public EscPosCommandBuilder barcode128(String barcodeData) {
        if (barcodeData == null || barcodeData.isEmpty()) {
            return this;
        }
        align(Alignment.CENTER);
        // Height
        stream.write(GS);
        stream.write('h');
        stream.write(60);

        // Width
        stream.write(GS);
        stream.write('w');
        stream.write(2);

        // HRI characters below
        stream.write(GS);
        stream.write('H');
        stream.write(2);

        // Print Code 128
        byte[] dataBytes = barcodeData.getBytes(StandardCharsets.US_ASCII);
        stream.write(GS);
        stream.write('k');
        stream.write(73); // CODE128
        stream.write(dataBytes.length);
        stream.write(dataBytes, 0, dataBytes.length);
        stream.write(LF);
        return this;
    }

    public EscPosCommandBuilder qrCode(String qrData) {
        if (qrData == null || qrData.isEmpty()) {
            return this;
        }
        align(Alignment.CENTER);
        byte[] dataBytes = qrData.getBytes(StandardCharsets.UTF_8);
        int len = dataBytes.length + 3;
        int pL = len % 256;
        int pH = len / 256;

        // Model 2
        stream.write(GS);
        stream.write('(');
        stream.write('k');
        stream.write(0x04);
        stream.write(0x00);
        stream.write(0x31);
        stream.write(0x41);
        stream.write(0x32);
        stream.write(0x00);

        // Module size (4 dots)
        stream.write(GS);
        stream.write('(');
        stream.write('k');
        stream.write(0x03);
        stream.write(0x00);
        stream.write(0x31);
        stream.write(0x43);
        stream.write(0x04);

        // Error correction level M
        stream.write(GS);
        stream.write('(');
        stream.write('k');
        stream.write(0x03);
        stream.write(0x00);
        stream.write(0x31);
        stream.write(0x45);
        stream.write(0x31);

        // Store data
        stream.write(GS);
        stream.write('(');
        stream.write('k');
        stream.write((byte) pL);
        stream.write((byte) pH);
        stream.write(0x31);
        stream.write(0x50);
        stream.write(0x30);
        stream.write(dataBytes, 0, dataBytes.length);

        // Print symbol
        stream.write(GS);
        stream.write('(');
        stream.write('k');
        stream.write(0x03);
        stream.write(0x00);
        stream.write(0x31);
        stream.write(0x51);
        stream.write(0x30);

        stream.write(LF);
        return this;
    }

    public EscPosCommandBuilder kickDrawer() {
        stream.write(ESC);
        stream.write('p');
        stream.write(0x00);
        stream.write(0x19);
        stream.write((byte) 0xFA);
        return this;
    }

    public EscPosCommandBuilder cutPaper() {
        feed(3);
        stream.write(GS);
        stream.write('V');
        stream.write(0x42);
        stream.write(0x00);
        return this;
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(toByteArray());
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String padLeft(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder();
        while (sb.length() < (n - s.length())) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }

    /**
     * Generates Egyptian E-Invoice / ZATCA TLV standard base64 string for QR codes.
     */
    public static String buildTlvQrString(String sellerName, String taxNumber, String timestamp, String totalAmount, String taxAmount) {
        try {
            ByteArrayOutputStream tlvStream = new ByteArrayOutputStream();
            appendTlv(tlvStream, 1, sellerName != null ? sellerName : "");
            appendTlv(tlvStream, 2, taxNumber != null ? taxNumber : "");
            appendTlv(tlvStream, 3, timestamp != null ? timestamp : "");
            appendTlv(tlvStream, 4, totalAmount != null ? totalAmount : "0.00");
            appendTlv(tlvStream, 5, taxAmount != null ? taxAmount : "0.00");
            return Base64.getEncoder().encodeToString(tlvStream.toByteArray());
        } catch (IOException e) {
            return sellerName + "|" + totalAmount;
        }
    }

    private static void appendTlv(ByteArrayOutputStream out, int tag, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(tag);
        out.write(bytes.length);
        out.write(bytes);
    }
}
