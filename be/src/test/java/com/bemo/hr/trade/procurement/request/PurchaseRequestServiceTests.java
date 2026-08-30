package com.bemo.hr.trade.procurement.request;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.request.api.PurchaseRequestApi;
import com.bemo.hr.trade.procurement.request.application.PurchaseRequestService;
import com.bemo.hr.trade.procurement.request.domain.PurchaseRequest;
import com.bemo.hr.trade.procurement.request.domain.PurchaseRequestLine;
import com.bemo.hr.trade.procurement.request.infrastructure.PurchaseRequestLineRepository;
import com.bemo.hr.trade.procurement.request.infrastructure.PurchaseRequestRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseRequestServiceTests {

    private PurchaseRequestRepository requestRepository;
    private PurchaseRequestLineRepository lineRepository;
    private ProcurementService procurementService;
    private DepartmentRepository departmentRepository;
    private DocumentNumberService documentNumberService;
    private AuditService auditService;
    private PurchaseRequestService service;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        requestRepository = mock(PurchaseRequestRepository.class);
        lineRepository = mock(PurchaseRequestLineRepository.class);
        procurementService = mock(ProcurementService.class);
        departmentRepository = mock(DepartmentRepository.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditService = mock(AuditService.class);
        service = new PurchaseRequestService(requestRepository, lineRepository, procurementService,
                departmentRepository, documentNumberService, auditService);
        when(documentNumberService.next(anyString(), anyString(), any())).thenReturn("PRQ-2026-00001");
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private PurchaseRequestApi.PurchaseRequestPayload payload(int lines) {
        List<PurchaseRequestApi.PurchaseRequestLinePayload> items = java.util.stream.IntStream.rangeClosed(1, lines)
                .mapToObj(index -> new PurchaseRequestApi.PurchaseRequestLinePayload(
                        "item-" + index, "Item " + index, new BigDecimal("5"), "BOX", new BigDecimal("10.50")))
                .toList();
        return new PurchaseRequestApi.PurchaseRequestPayload("merl", null,
                today.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), null, items);
    }

    private PurchaseRequest savedDraft() {
        PurchaseRequest request = new PurchaseRequest("PRQ-2026-00001", "merl", null, today, null);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        return request;
    }

    private PurchaseRequest inStatus(PurchaseRequest.Status status) {
        PurchaseRequest request = savedDraft();
        switch (status) {
            case SUBMITTED -> request.submit();
            case APPROVED -> {
                request.submit();
                request.approve();
            }
            case REJECTED -> {
                request.submit();
                request.reject();
            }
            case CANCELLED -> request.cancel();
            case CONVERTED -> {
                request.submit();
                request.approve();
                request.markConverted("po-seed");
            }
            default -> { }
        }
        return request;
    }

    private ProcurementApi.PurchaseOrderResponse poResponse(String poId, String poNumber) {
        return new ProcurementApi.PurchaseOrderResponse(poId, poNumber, 0L, "sup-1", "Supplier",
                "pr-1", null, null, null, null, null, "EGP", "EGP",
                BigDecimal.ONE, 0L, "SNAPSHOT", null, BigDecimal.ZERO, "OPEN",
                BigDecimal.ZERO, List.of(), 0L, 0L);
    }

    // ─── Creation ─────────────────────────────────────────────────────

    @Test
    void createGeneratesPrqNumberAndStoresLines() {
        when(requestRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.of(new PurchaseRequest("PRQ-2026-00001", "merl", null, today, null)));
        PurchaseRequestApi.PurchaseRequestResponse response = service.create(payload(2));
        assertThat(response.requestNumber()).isEqualTo("PRQ-2026-00001");
        assertThat(response.status()).isEqualTo(PurchaseRequest.Status.DRAFT.name());
        verify(lineRepository, times(1)).saveAll(any());
        ArgumentCaptor<List<PurchaseRequestLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(lineRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        verify(auditService).record(eq("CREATE"), eq("PURCHASE_REQUEST"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void createWithNoLinesThrowsEmptyLines() {
        PurchaseRequestApi.PurchaseRequestPayload empty =
                new PurchaseRequestApi.PurchaseRequestPayload("merl", null, null, null, List.of());
        Assertions.assertThatThrownBy(() -> service.create(empty))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_EMPTY_LINES"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRejectsNonPositiveQuantity() {
        PurchaseRequestApi.PurchaseRequestLinePayload badLine =
                new PurchaseRequestApi.PurchaseRequestLinePayload("item-1", "Item", BigDecimal.ZERO, null, null);
        PurchaseRequestApi.PurchaseRequestPayload bad =
                new PurchaseRequestApi.PurchaseRequestPayload("merl", null, null, null, List.of(badLine));
        Assertions.assertThatThrownBy(() -> service.create(bad))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_LINE_QUANTITY_INVALID"));
    }

    // ─── State machine matrix ─────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "DRAFT,true,SUBMITTED",
            "SUBMITTED,false,SUBMITTED",
            "APPROVED,false,APPROVED"
    })
    void submitOnlyAllowedFromDraft(String from, boolean allowed, String expectedEndState) {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.valueOf(from));
        if (allowed) {
            assertThat(service.submit(request.getId()).status()).isEqualTo(expectedEndState);
        } else {
            Assertions.assertThatThrownBy(() -> service.submit(request.getId()))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                            Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
            assertThat(request.getStatus().name()).isEqualTo(expectedEndState);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "SUBMITTED,true,APPROVED",
            "DRAFT,false,DRAFT",
            "REJECTED,false,REJECTED",
            "CONVERTED,false,CONVERTED"
    })
    void approveOnlyAllowedFromSubmitted(String from, boolean allowed, String expectedEndState) {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.valueOf(from));
        if (allowed) {
            assertThat(service.approve(request.getId(), "ok").status()).isEqualTo(expectedEndState);
        } else {
            Assertions.assertThatThrownBy(() -> service.approve(request.getId(), "ok"))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                            Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
            assertThat(request.getStatus().name()).isEqualTo(expectedEndState);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "SUBMITTED,true,REJECTED",
            "APPROVED,false,APPROVED",
            "CANCELLED,false,CANCELLED"
    })
    void rejectOnlyAllowedFromSubmitted(String from, boolean allowed, String expectedEndState) {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.valueOf(from));
        if (allowed) {
            assertThat(service.reject(request.getId(), "no").status()).isEqualTo(expectedEndState);
        } else {
            Assertions.assertThatThrownBy(() -> service.reject(request.getId(), "no"))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                            Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
            assertThat(request.getStatus().name()).isEqualTo(expectedEndState);
        }
    }

    @Test
    void cancelAllowedFromDraftAndBlockedFromApproved() {
        PurchaseRequest draft = savedDraft();
        assertThat(service.cancel(draft.getId()).status()).isEqualTo(PurchaseRequest.Status.CANCELLED.name());
        PurchaseRequest approved = inStatus(PurchaseRequest.Status.APPROVED);
        Assertions.assertThatThrownBy(() -> service.cancel(approved.getId()))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
    }

    @Test
    void updateWhileDraftReplacesLinesAndBlockedAfterSubmit() {
        PurchaseRequest draft = savedDraft();
        service.update(draft.getId(), payload(3));
        verify(lineRepository).deleteByRequestId(draft.getId());

        PurchaseRequest submitted = inStatus(PurchaseRequest.Status.SUBMITTED);
        Assertions.assertThatThrownBy(() -> service.update(submitted.getId(), payload(1)))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
    }

    @Test
    void unknownIdThrowsNotFound() {
        when(requestRepository.findById("missing")).thenReturn(Optional.empty());
        Assertions.assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_NOT_FOUND"));
    }

    // ─── Conversion ───────────────────────────────────────────────────

    @Test
    void convertBuildsOnePoMirroringRemainingLines() {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.APPROVED);
        PurchaseRequestLine line = new PurchaseRequestLine(request.getId(), "item-1", "Steel",
                new BigDecimal("10"), "TON", new BigDecimal("120.00"));
        when(lineRepository.findByRequestIdOrderByItemNameAsc(request.getId())).thenReturn(List.of(line));
        when(procurementService.create(any())).thenReturn(poResponse("po-9", "PO-2026-00009"));

        PurchaseRequestApi.PurchaseRequestResponse response = service.convert(request.getId(), "sup-1");

        assertThat(response.status()).isEqualTo(PurchaseRequest.Status.CONVERTED.name());
        assertThat(response.convertedPoId()).isEqualTo("po-9");
        assertThat(line.getConvertedQuantity()).isEqualByComparingTo(new BigDecimal("10"));

        ArgumentCaptor<ProcurementApi.PurchaseOrderPayload> captor =
                ArgumentCaptor.forClass(ProcurementApi.PurchaseOrderPayload.class);
        verify(procurementService).create(captor.capture());
        ProcurementApi.PurchaseOrderPayload sent = captor.getValue();
        assertThat(sent.purchaseRequestId()).isEqualTo(request.getId());
        assertThat(sent.supplierId()).isEqualTo("sup-1");
        assertThat(sent.items()).hasSize(1);
        assertThat(sent.items().get(0).itemId()).isEqualTo("item-1");
        assertThat(sent.items().get(0).quantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(sent.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
        verify(auditService).record(eq("CONVERT"), eq("PURCHASE_REQUEST"), eq(request.getId()),
                anyString(), anyString(), any());
    }

    @Test
    void convertOnlyAllowedFromApproved() {
        PurchaseRequest submitted = inStatus(PurchaseRequest.Status.SUBMITTED);
        Assertions.assertThatThrownBy(() -> service.convert(submitted.getId(), "sup-1"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_INVALID_STATE"));
        verify(procurementService, never()).create(any());
    }

    @Test
    void doubleConvertThrowsAlreadyConverted() {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.APPROVED);
        PurchaseRequestLine line = new PurchaseRequestLine(request.getId(), "item-1", "Steel",
                new BigDecimal("4"), null, null);
        when(lineRepository.findByRequestIdOrderByItemNameAsc(request.getId())).thenReturn(List.of(line));
        when(procurementService.create(any())).thenReturn(poResponse("po-1", "PO-1"));

        service.convert(request.getId(), "sup-1");
        Assertions.assertThatThrownBy(() -> service.convert(request.getId(), "sup-1"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_ALREADY_CONVERTED"));
        verify(procurementService, times(1)).create(any());
    }

    @Test
    void convertBlocksWhenAnyLineAlreadyOverConverted() {
        PurchaseRequest request = inStatus(PurchaseRequest.Status.APPROVED);
        PurchaseRequestLine line = new PurchaseRequestLine(request.getId(), "item-1", "Steel",
                new BigDecimal("2"), null, null);
        line.registerConversion(new BigDecimal("2"));
        when(lineRepository.findByRequestIdOrderByItemNameAsc(request.getId())).thenReturn(List.of(line));

        Assertions.assertThatThrownBy(() -> service.convert(request.getId(), "sup-1"))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        Assertions.assertThat(ex.getCode()).isEqualTo("PR_QUANTITY_EXCEEDED"));
        verify(procurementService, never()).create(any());
    }
}
