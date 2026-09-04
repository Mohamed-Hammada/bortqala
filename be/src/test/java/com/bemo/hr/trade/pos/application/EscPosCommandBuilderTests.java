package com.bemo.hr.trade.pos.application;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EscPosCommandBuilderTests {

    @Test
    void buildsInitializationAndFormattingCommands() {
        EscPosCommandBuilder builder = EscPosCommandBuilder.create80mm();
        builder.initialize()
                .align(EscPosCommandBuilder.Alignment.CENTER)
                .bold(true)
                .line("BEMO POS")
                .bold(false)
                .feed(1)
                .cutPaper()
                .kickDrawer();

        byte[] bytes = builder.toByteArray();
        assertThat(bytes).isNotEmpty();

        // ESC @ is 0x1B, 0x40
        assertThat(bytes[0]).isEqualTo((byte) 0x1B);
        assertThat(bytes[1]).isEqualTo((byte) 0x40);

        // Contains text
        String str = new String(bytes, StandardCharsets.UTF_8);
        assertThat(str).contains("BEMO POS");

        // Base64 is non-empty
        assertThat(builder.toBase64()).isNotEmpty();
    }

    @Test
    void formatsTwoAndThreeColumnRows() {
        EscPosCommandBuilder builder58 = EscPosCommandBuilder.create58mm();
        assertThat(builder58.getColumnWidth()).isEqualTo(32);

        builder58.rowTwoColumns("Subtotal:", "150.00 EGP");
        builder58.rowThreeColumns("Item Name", "2", "300.00");

        byte[] bytes = builder58.toByteArray();
        String output = new String(bytes, StandardCharsets.UTF_8);
        assertThat(output).contains("Subtotal:");
        assertThat(output).contains("150.00 EGP");
        assertThat(output).contains("Item Name");
    }

    @Test
    void generatesBarcodeAndQrCodeCommands() {
        EscPosCommandBuilder builder = EscPosCommandBuilder.create80mm();
        builder.barcode128("TXN-2026-0001")
                .qrCode("https://bemo.cloud/invoice/123");

        byte[] bytes = builder.toByteArray();
        assertThat(bytes).isNotEmpty();

        // Check barcode header GS k 73
        boolean hasBarcode = false;
        for (int i = 0; i < bytes.length - 2; i++) {
            if (bytes[i] == 0x1D && bytes[i + 1] == 'k' && bytes[i + 2] == 73) {
                hasBarcode = true;
                break;
            }
        }
        assertThat(hasBarcode).isTrue();

        // Check QR code header GS ( k
        boolean hasQr = false;
        for (int i = 0; i < bytes.length - 2; i++) {
            if (bytes[i] == 0x1D && bytes[i + 1] == '(' && bytes[i + 2] == 'k') {
                hasQr = true;
                break;
            }
        }
        assertThat(hasQr).isTrue();
    }

    @Test
    void buildsEgyptianTaxTlvQrString() {
        String tlvBase64 = EscPosCommandBuilder.buildTlvQrString(
                "Al-Amal Trading LLC",
                "123-456-789",
                "2026-09-04T12:00:00Z",
                "1140.00",
                "140.00"
        );
        assertThat(tlvBase64).isNotEmpty();
        byte[] decoded = java.util.Base64.getDecoder().decode(tlvBase64);
        assertThat(decoded).isNotEmpty();
        // Tag 1 should be seller name
        assertThat(decoded[0]).isEqualTo((byte) 1);
        int sellerLen = decoded[1] & 0xFF;
        String seller = new String(decoded, 2, sellerLen, StandardCharsets.UTF_8);
        assertThat(seller).isEqualTo("Al-Amal Trading LLC");
    }
}
