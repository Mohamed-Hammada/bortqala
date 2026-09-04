import { Injectable, inject } from '@angular/core';
import { NotificationService } from '../notification.service';
import { I18nService } from '../i18n.service';
import { ReceiptPrintData } from '../../features/trade/pos/pos.models';

@Injectable({
  providedIn: 'root',
})
export class ThermalPrinterService {
  private readonly notification = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  /**
   * Dispatches receipt print job according to connection type and server dispatch state.
   */
  async printReceipt(data: ReceiptPrintData): Promise<boolean> {
    if (data.sentToPrinter) {
      this.notification.success(this.i18n.t('pos.testPrintSuccess'));
      return true;
    }

    if (data.connectionType === 'BLUETOOTH' && typeof navigator !== 'undefined' && 'bluetooth' in navigator) {
      try {
        return await this.printWebBluetooth(data.base64Bytes);
      } catch (err) {
        // Fallback to standard window print if bluetooth pairing is canceled or unsupported
      }
    }

    // Default fallback: Trigger browser receipt printing window
    this.printViaBrowserFallback(data);
    return true;
  }

  /**
   * Connects to Bluetooth thermal printer using standard Web Bluetooth GATT serial service.
   */
  async printWebBluetooth(base64Data: string): Promise<boolean> {
    try {
      const binaryString = atob(base64Data);
      const len = binaryString.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      // Standard Bluetooth SPP / printer service UUIDs
      const nav = navigator as unknown as {
        bluetooth: {
          requestDevice(options: unknown): Promise<{
            gatt?: {
              connect(): Promise<{
                getPrimaryService(uuid: string): Promise<{
                  getCharacteristics(): Promise<Array<{ writeValue(data: Uint8Array): Promise<void> }>>;
                }>;
              }>;
            };
          }>;
        };
      };

      const device = await nav.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: [
          '000018f0-0000-1000-8000-00805f9b34fb',
          '49535343-fe7d-4ae5-8fa9-9fafd205e455',
          'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
        ],
      });

      if (!device.gatt) return false;
      const server = await device.gatt.connect();
      const services = [
        '000018f0-0000-1000-8000-00805f9b34fb',
        '49535343-fe7d-4ae5-8fa9-9fafd205e455',
        'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
      ];

      for (const sUuid of services) {
        try {
          const service = await server.getPrimaryService(sUuid);
          const chars = await service.getCharacteristics();
          if (chars.length > 0) {
            const char = chars[0];
            // Chunk transmission in 512-byte blocks
            const chunkSize = 512;
            for (let offset = 0; offset < bytes.length; offset += chunkSize) {
              const chunk = bytes.subarray(offset, Math.min(offset + chunkSize, bytes.length));
              await char.writeValue(chunk);
            }
            this.notification.success(this.i18n.t('pos.testPrintSuccess'));
            return true;
          }
        } catch {
          // Try next service
        }
      }
      return false;
    } catch {
      return false;
    }
  }

  private printViaBrowserFallback(data: ReceiptPrintData): void {
    const is58mm = data.paperWidth === 'MM_58';
    const widthCss = is58mm ? '58mm' : '80mm';

    const printWindow = window.open('', '_blank', 'width=450,height=650');
    if (!printWindow) {
      window.print();
      return;
    }

    printWindow.document.write(`
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <title>Receipt ${data.transactionNumber}</title>
        <style>
          @page { size: ${widthCss} auto; margin: 0; }
          body {
            width: ${widthCss};
            margin: 0 auto;
            padding: 8px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            color: #000;
            background: #fff;
          }
          .center { text-align: center; }
          .bold { font-weight: bold; }
          .line { border-top: 1px dashed #000; margin: 6px 0; }
          .double-line { border-top: 2px solid #000; margin: 6px 0; }
          .row { display: flex; justify-content: space-between; margin: 2px 0; }
          .reprint-badge { border: 2px solid #000; padding: 4px; text-align: center; margin: 4px 0; font-weight: bold; }
          @media print {
            body { padding: 0; }
          }
        </style>
      </head>
      <body>
        <div class="center bold" style="font-size: 16px;">BEMO POS</div>
        <div class="center">${data.printerName || 'Thermal Receipt'}</div>
        ${data.reprintCount > 0 ? `<div class="reprint-badge">*** DUPLICATE (REPRINT #${data.reprintCount}) ***</div>` : ''}
        <div class="double-line"></div>
        <div class="row"><span>الإيصال:</span><span>${data.transactionNumber}</span></div>
        <div class="line"></div>
        <div class="center bold" style="margin-top: 8px;">جاهز للطباعة الحرارية المباشرة</div>
        <div class="center" style="font-size: 10px; margin-top: 4px;">ESC/POS Binary Payload: ${data.base64Bytes.length} bytes</div>
        <div class="line"></div>
        <div class="center">شكراً لتعاملكم معنا</div>
      </body>
      </html>
    `);

    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  }
}
