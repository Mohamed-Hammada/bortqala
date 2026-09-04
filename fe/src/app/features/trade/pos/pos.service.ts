import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CloseSessionPayload,
  OpenSessionPayload,
  PosSession,
  PosSummary,
  PosTerminal,
  PosTransaction,
  ProcessReturnPayload,
  ProcessSalePayload,
  ReceiptPrintData,
  SavePrinterPayload,
  SaveTerminalPayload,
  TestPrintResponse,
  ThermalPrinter,
} from './pos.models';

@Injectable({
  providedIn: 'root',
})
export class PosDataService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/trade/pos';

  getTerminals(): Observable<PosTerminal[]> {
    return this.http.get<PosTerminal[]>(`${this.baseUrl}/terminals`);
  }

  saveTerminal(payload: SaveTerminalPayload): Observable<PosTerminal> {
    return this.http.post<PosTerminal>(`${this.baseUrl}/terminals`, payload);
  }

  getSessions(): Observable<PosSession[]> {
    return this.http.get<PosSession[]>(`${this.baseUrl}/sessions`);
  }

  getActiveSession(terminalId: string): Observable<PosSession | null> {
    return this.http.get<PosSession | null>(`${this.baseUrl}/sessions/active`, {
      params: { terminalId },
    });
  }

  openSession(payload: OpenSessionPayload): Observable<PosSession> {
    return this.http.post<PosSession>(`${this.baseUrl}/sessions/open`, payload);
  }

  closeSession(id: string, payload: CloseSessionPayload): Observable<PosSession> {
    return this.http.post<PosSession>(`${this.baseUrl}/sessions/${id}/close`, payload);
  }

  processSale(payload: ProcessSalePayload): Observable<PosTransaction> {
    return this.http.post<PosTransaction>(`${this.baseUrl}/transactions/sale`, payload);
  }

  processReturn(payload: ProcessReturnPayload): Observable<PosTransaction> {
    return this.http.post<PosTransaction>(`${this.baseUrl}/transactions/return`, payload);
  }

  getTransactions(): Observable<PosTransaction[]> {
    return this.http.get<PosTransaction[]>(`${this.baseUrl}/transactions`);
  }

  getSummary(): Observable<PosSummary> {
    return this.http.get<PosSummary>(`${this.baseUrl}/summary`);
  }

  getPrinters(): Observable<ThermalPrinter[]> {
    return this.http.get<ThermalPrinter[]>(`${this.baseUrl}/printers`);
  }

  savePrinter(payload: SavePrinterPayload): Observable<ThermalPrinter> {
    return this.http.post<ThermalPrinter>(`${this.baseUrl}/printers`, payload);
  }

  deletePrinter(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/printers/${id}`);
  }

  testPrint(id: string): Observable<TestPrintResponse> {
    return this.http.post<TestPrintResponse>(`${this.baseUrl}/printers/${id}/test-print`, {});
  }

  getReceiptEscPos(transactionId: string, printerId?: string): Observable<ReceiptPrintData> {
    return this.http.get<ReceiptPrintData>(`${this.baseUrl}/transactions/${transactionId}/receipt-escpos`, {
      params: printerId ? { printerId } : {},
    });
  }

  reprintReceipt(transactionId: string, reason: string, printerId?: string): Observable<ReceiptPrintData> {
    return this.http.post<ReceiptPrintData>(`${this.baseUrl}/transactions/${transactionId}/reprint`, {
      reason,
      printerId,
    });
  }
}
