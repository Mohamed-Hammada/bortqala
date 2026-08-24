package com.bemo.hr.trade.procurement.request.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.trade.procurement.api.ProcurementApi;
import com.bemo.hr.trade.procurement.application.ProcurementService;
import com.bemo.hr.trade.procurement.request.api.PurchaseRequestApi;
import com.bemo.hr.trade.procurement.request.domain.PurchaseRequest;
import com.bemo.hr.trade.procurement.request.domain.PurchaseRequestLine;
import com.bemo.hr.trade.procurement.request.infrastructure.PurchaseRequestLineRepository;
import com.bemo.hr.trade.procurement.request.infrastructure.PurchaseRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PurchaseRequestService {

    /**
     * Approvals use direct approve/reject endpoints with full audit records rather than the
     * generic approval-engine inbox: wiring a PURCHASE_REQUEST workflow type into
     * {@code ApprovalWorkflowService} would need callback hooks on final decisions that do not
     * exist yet (decision documented per WP-03 AC-5 fallback).
     */
    public static final String DOCUMENT_TYPE = "PURCHASE_REQUEST";

    private final PurchaseRequestRepository requestRepository;
    private final PurchaseRequestLineRepository lineRepository;
    private final ProcurementService procurementService;
    private final DepartmentRepository departmentRepository;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public PurchaseRequestService(PurchaseRequestRepository requestRepository,
                                  PurchaseRequestLineRepository lineRepository,
                                  ProcurementService procurementService,
                                  DepartmentRepository departmentRepository,
                                  DocumentNumberService documentNumberService,
                                  AuditService auditService) {
        this.requestRepository = requestRepository;
        this.lineRepository = lineRepository;
        this.procurementService = procurementService;
        this.departmentRepository = departmentRepository;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    // ─── Queries ──────────────────────────────────────────────────────

    public List<PurchaseRequestApi.PurchaseRequestResponse> list(String status, String departmentId) {
        List<PurchaseRequest> requests;
        if (status != null && !status.isBlank() && departmentId != null && !departmentId.isBlank()) {
            requests = requestRepository.findByStatusAndDepartmentIdOrderByCreatedAtDesc(
                    parseStatus(status), departmentId.strip());
        } else if (status != null && !status.isBlank()) {
            requests = requestRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status));
        } else if (departmentId != null && !departmentId.isBlank()) {
            requests = requestRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId.strip());
        } else {
            requests = requestRepository.findAllByOrderByCreatedAtDesc();
        }
        Map<String, String> supplierNames = resolveDepartmentNames(requests);
        return requests.stream().map(request -> toResponse(request, loadLines(request.getId()), supplierNames)).toList();
    }

    public PurchaseRequestApi.PurchaseRequestResponse get(String id) {
        PurchaseRequest request = requireRequest(id);
        return toResponse(request, loadLines(id), resolveDepartmentNames(List.of(request)));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse create(PurchaseRequestApi.PurchaseRequestPayload payload) {
        log.debug("create purchase request called requestedBy={}", payload.requestedBy());
        requireLines(payload.lines());
        LocalDate neededBy = toLocalDate(payload.neededBy());
        String number = documentNumberService.next(DOCUMENT_TYPE, "PRQ", LocalDate.now(ZoneOffset.UTC));
        PurchaseRequest request = new PurchaseRequest(number, payload.requestedBy().strip(),
                payload.departmentId(), neededBy, payload.notes());
        PurchaseRequest saved = requestRepository.save(request);
        lineRepository.saveAll(buildLines(saved.getId(), payload.lines()));
        auditService.record("CREATE", DOCUMENT_TYPE, saved.getId(), getCurrentUser(),
                "{\"requestNumber\":\"" + saved.getRequestNumber() + "\",\"lines\":" + payload.lines().size() + "}", null);
        log.info("PurchaseRequest {} created with number {}", saved.getId(), saved.getRequestNumber());
        return get(saved.getId());
    }

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse update(String id, PurchaseRequestApi.PurchaseRequestPayload payload) {
        PurchaseRequest request = requireRequest(id);
        requireLines(payload.lines());
        request.editDraft(payload.requestedBy(), payload.departmentId(), toLocalDate(payload.neededBy()), payload.notes());
        lineRepository.deleteByRequestId(id);
        lineRepository.saveAll(buildLines(id, payload.lines()));
        auditService.record("UPDATE", DOCUMENT_TYPE, id, getCurrentUser(),
                "{\"lines\":" + payload.lines().size() + "}", null);
        return get(id);
    }

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse submit(String id) {
        PurchaseRequest request = requireRequest(id);
        request.submit();
        auditService.record("SUBMIT", DOCUMENT_TYPE, id, getCurrentUser(), "{}", null);
        return get(id);
    }

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse approve(String id, String decisionNote) {
        PurchaseRequest request = requireRequest(id);
        request.approve();
        auditService.record("APPROVE", DOCUMENT_TYPE, id, getCurrentUser(),
                "{\"note\":" + jsonString(decisionNote) + "}", null);
        return get(id);
    }

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse reject(String id, String decisionNote) {
        PurchaseRequest request = requireRequest(id);
        request.reject();
        auditService.record("REJECT", DOCUMENT_TYPE, id, getCurrentUser(),
                "{\"note\":" + jsonString(decisionNote) + "}", null);
        return get(id);
    }

    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse cancel(String id) {
        PurchaseRequest request = requireRequest(id);
        request.cancel();
        auditService.record("CANCEL", DOCUMENT_TYPE, id, getCurrentUser(), "{}", null);
        return get(id);
    }

    // ─── Conversion ───────────────────────────────────────────────────

    /**
     * Converts an APPROVED request into exactly ONE purchase order mirroring its open lines and
     * carrying {@code purchaseRequestId}, finally populating the previously dangling PO field.
     */
    @Transactional
    public PurchaseRequestApi.PurchaseRequestResponse convert(String id, String supplierId) {
        log.debug("convert called for purchase request {} supplier {}", id, supplierId);
        PurchaseRequest request = requireRequest(id);
        if (request.getStatus() == PurchaseRequest.Status.CONVERTED || request.getConvertedPoId() != null)
            throw new BusinessRuleException("This purchase request was already converted to a purchase order.",
                    "PR_ALREADY_CONVERTED", HttpStatus.CONFLICT);
        if (request.getStatus() != PurchaseRequest.Status.APPROVED)
            throw new BusinessRuleException("Conversion is only allowed while the purchase request is APPROVED.",
                    "PR_INVALID_STATE", HttpStatus.CONFLICT);
        List<PurchaseRequestLine> lines = loadLines(id);
        for (PurchaseRequestLine line : lines) {
            if (line.getConvertedQuantity().compareTo(line.getQuantity()) > 0)
                throw new BusinessRuleException("Line for item " + line.getItemName()
                        + " was already converted beyond its requested quantity.", "PR_QUANTITY_EXCEEDED", HttpStatus.CONFLICT);
        }
        List<ProcurementApi.PurchaseOrderLinePayload> poItems = lines.stream()
                .filter(line -> line.remainingQuantity().signum() > 0)
                .map(line -> new ProcurementApi.PurchaseOrderLinePayload(line.getItemId(), line.getItemName(), null,
                        line.remainingQuantity(), line.getUnitOfMeasure(),
                        line.getEstimatedUnitPrice() == null ? BigDecimal.ZERO : line.getEstimatedUnitPrice(),
                        null, null, null))
                .toList();
        if (poItems.isEmpty())
            throw new BusinessRuleException("This purchase request has no remaining quantity left to convert.",
                    "PR_QUANTITY_EXCEEDED", HttpStatus.CONFLICT);

        long today = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        ProcurementApi.PurchaseOrderResponse po = procurementService.create(new ProcurementApi.PurchaseOrderPayload(
                null, today, supplierId, request.getId(), request.getDepartmentId(),
                null, null, null, null, poItems));

        lines.forEach(line -> line.registerConversion(line.remainingQuantity()));
        lineRepository.saveAll(lines);
        request.markConverted(po.id());
        auditService.record("CONVERT", DOCUMENT_TYPE, id, getCurrentUser(),
                "{\"purchaseOrderId\":\"" + po.id() + "\",\"poNumber\":\"" + po.poNumber() + "\"}", null);
        log.info("PurchaseRequest {} converted into PO {}", id, po.id());
        return get(id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void requireLines(List<PurchaseRequestApi.PurchaseRequestLinePayload> lines) {
        if (lines == null || lines.isEmpty())
            throw new BusinessRuleException("A purchase request requires at least one line.", "PR_EMPTY_LINES", HttpStatus.CONFLICT);
    }

    private List<PurchaseRequestLine> buildLines(String requestId, List<PurchaseRequestApi.PurchaseRequestLinePayload> payloads) {
        return payloads.stream()
                .map(payload -> new PurchaseRequestLine(requestId, payload.itemId().strip(),
                        payload.itemName() == null || payload.itemName().isBlank() ? payload.itemId() : payload.itemName().strip(),
                        payload.quantity(), payload.unitOfMeasure(), payload.estimatedUnitPrice()))
                .toList();
    }

    private PurchaseRequest requireRequest(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Purchase request not found.", "PR_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private List<PurchaseRequestLine> loadLines(String requestId) {
        return lineRepository.findByRequestIdOrderByItemNameAsc(requestId);
    }

    private PurchaseRequest.Status parseStatus(String status) {
        try {
            return PurchaseRequest.Status.valueOf(status.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Unknown purchase request status: " + status + ".",
                    "PR_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }
    }

    private LocalDate toLocalDate(Long epochMilli) {
        return epochMilli == null ? null : Instant.ofEpochMilli(epochMilli).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String jsonString(String value) {
        return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private Map<String, String> resolveDepartmentNames(List<PurchaseRequest> requests) {
        return requests.stream()
                .map(PurchaseRequest::getDepartmentId)
                .filter(departmentId -> departmentId != null)
                .distinct()
                .collect(Collectors.toMap(departmentId -> departmentId,
                        departmentId -> departmentRepository.findById(departmentId)
                                .map(department -> department.getName())
                                .orElse(departmentId),
                        (a, b) -> a));
    }

    private PurchaseRequestApi.PurchaseRequestResponse toResponse(PurchaseRequest request,
                                                                  List<PurchaseRequestLine> lines,
                                                                  Map<String, String> departmentNames) {
        BigDecimal estimatedTotal = lines.stream()
                .map(line -> line.getQuantity()
                        .multiply(line.getEstimatedUnitPrice() == null ? BigDecimal.ZERO : line.getEstimatedUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PurchaseRequestApi.PurchaseRequestLineResponse> lineResponses = lines.stream()
                .map(line -> new PurchaseRequestApi.PurchaseRequestLineResponse(line.getId(), line.getItemId(),
                        line.getItemName(), line.getQuantity(), line.getUnitOfMeasure(),
                        line.getEstimatedUnitPrice(), line.getConvertedQuantity()))
                .toList();
        return new PurchaseRequestApi.PurchaseRequestResponse(request.getId(), request.getRequestNumber(),
                request.getRequestedBy(), request.getDepartmentId(),
                request.getDepartmentId() == null ? null : departmentNames.getOrDefault(request.getDepartmentId(), request.getDepartmentId()),
                request.getStatus().name(),
                request.getNeededBy() == null ? null : request.getNeededBy().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                request.getNotes(), request.getConvertedPoId(), estimatedTotal, lineResponses,
                request.getCreatedAt(), request.getUpdatedAt());
    }

    private String getCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.getName() != null && !authentication.getName().isBlank())
                ? authentication.getName() : "system";
    }
}
