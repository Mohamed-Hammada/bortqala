package com.bemo.hr.trade.sales.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.posting.SubledgerPostingService;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.api.SalesApi;
import com.bemo.hr.trade.sales.domain.*;
import com.bemo.hr.trade.sales.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesReceivablesService {
    private final CustomerCreditProfileRepository creditRepository;
    private final CustomerInvoiceRepository invoiceRepository;
    private final CustomerReceiptRepository receiptRepository;
    private final CustomerReceiptAllocationRepository allocationRepository;
    private final CustomerCreditNoteRepository creditNoteRepository;
    private final CollectionTaskRepository taskRepository;
    private final BusinessPartyRepository partyRepository;
    private final PartnerLedgerEntryRepository ledgerRepository;
    private final AuditService auditService;
    private final SubledgerPostingService subledgerPostingService;

    private static LocalDate date(long value) {
        return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static long ms(LocalDate value) {
        return value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    @Transactional(readOnly = true)
    public List<SalesApi.InvoiceResponse> invoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc().stream().map(this::invoice).toList();
    }

    @Transactional(readOnly = true)
    public List<SalesApi.ReceiptResponse> receipts() {
        return receiptRepository.findAllByOrderByReceiptDateDescCreatedAtDesc().stream().map(this::receipt).toList();
    }

    @Transactional(readOnly = true)
    public SalesApi.CreditProfileResponse credit(String customerId) {
        requireCustomer(customerId);
        return creditResponse(creditRepository.findByCustomerId(customerId).orElse(null), customerId);
    }

    @Transactional
    public SalesApi.CreditProfileResponse updateCredit(String customerId, SalesApi.CreditProfileRequest request, String actor) {
        requireCustomer(customerId);
        CustomerCreditProfile profile = creditRepository.findByCustomerId(customerId).orElseGet(() -> new CustomerCreditProfile(customerId));
        profile.update(request.creditLimit(), request.paymentTermsDays(), request.creditHold());
        creditRepository.save(profile);
        auditService.record("UPDATE", "CUSTOMER_CREDIT", customerId, actor, "{\"limit\":" + request.creditLimit() + ",\"hold\":" + request.creditHold() + "}", null);
        return creditResponse(profile, customerId);
    }

    public void assertCreditAvailable(String customerId, BigDecimal exposure) {
        CustomerCreditProfile profile = creditRepository.findByCustomerId(customerId).orElse(null);
        if (profile == null) return;
        if (profile.isCreditHold()) throw error("AR_CUSTOMER_CREDIT_HOLD", HttpStatus.CONFLICT);
        if (profile.getCreditLimit().subtract(invoiceRepository.outstanding(customerId)).compareTo(exposure) < 0)
            throw error("AR_CREDIT_LIMIT_EXCEEDED", HttpStatus.CONFLICT);
    }

    @Transactional
    public SalesApi.InvoiceResponse createInvoice(SalesApi.InvoiceRequest request, String actor) {
        if (invoiceRepository.existsByInvoiceNumberIgnoreCase(request.invoiceNumber()))
            throw error("AR_INVOICE_NUMBER_EXISTS", HttpStatus.CONFLICT);
        requireCustomer(request.customerId());
        LocalDate date = date(request.invoiceDate());
        LocalDate due = request.dueDate() > 0 ? date(request.dueDate()) :
                date.plusDays(creditRepository.findByCustomerId(request.customerId()).map(CustomerCreditProfile::getPaymentTermsDays).orElse(30));
        if (due.isBefore(date)) throw error("AR_DUE_DATE_INVALID", HttpStatus.BAD_REQUEST);
        CustomerInvoice saved = invoiceRepository.save(new CustomerInvoice(request.invoiceNumber(), request.customerId(), request.salesOrderId(), date, due, request.currencyCode(), request.amount()));
        auditService.record("CREATE", "CUSTOMER_INVOICE", saved.getId(), actor, "{\"number\":\"" + saved.getInvoiceNumber() + "\"}", null);
        return invoice(saved);
    }

    @Transactional
    public CustomerInvoice createAndIssueDeliveryInvoice(String invoiceNumber, String customerId, String salesOrderId,
                                                         LocalDate invoiceDate, String currencyCode, BigDecimal amount, String actor) {
        SalesApi.InvoiceResponse created = createInvoice(new SalesApi.InvoiceRequest(invoiceNumber, customerId, salesOrderId,
                ms(invoiceDate), ms(invoiceDate.plusDays(creditRepository.findByCustomerId(customerId)
                .map(CustomerCreditProfile::getPaymentTermsDays).orElse(30))), currencyCode, amount), actor);
        issueInvoice(created.id(), actor);
        return invoiceRepository.findById(created.id()).orElseThrow(() -> error("AR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public CustomerCreditNote applyReturnCredit(String operationId, String creditNoteNumber, String invoiceId,
                                                String salesOrderId, String deliveryId, String returnId, LocalDate creditDate,
                                                BigDecimal amount, String actor) {
        CustomerCreditNote replay = creditNoteRepository.findByOperationId(operationId).orElse(null);
        if (replay != null) return replay;
        CustomerInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElseThrow(() -> error("AR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
        try {
            invoice.applyCredit(amount);
        } catch (IllegalArgumentException ex) {
            throw error("O2C_CREDIT_AMOUNT_INVALID", HttpStatus.CONFLICT);
        }
        CustomerCreditNote note = creditNoteRepository.save(new CustomerCreditNote(creditNoteNumber, invoice.getCustomerId(),
                invoice.getId(), salesOrderId, deliveryId, returnId, creditDate, invoice.getCurrencyCode(), amount, operationId, actor));
        invoiceRepository.save(invoice);
        ledgerRepository.save(new PartnerLedgerEntry(invoice.getCustomerId(), "CUSTOMER_CREDIT_NOTE", amount.negate(),
                creditNoteNumber, "Customer return credit", creditDate.atStartOfDay(ZoneOffset.UTC).toInstant(), actor));
        subledgerPostingService.postSubledgerEvent("SALES", "CUSTOMER_CREDIT_NOTE", note.getId(),
                "CUSTOMER_CREDIT_NOTE_ISSUED", "AR:CREDIT_NOTE:" + operationId, creditDate,
                "Customer credit note " + creditNoteNumber, amount, amount, null,
                invoice.getCustomerId(), invoice.getCurrencyCode(), actor);
        auditService.record("CREATE", "CUSTOMER_CREDIT_NOTE", note.getId(), actor,
                "{\"returnId\":\"" + returnId + "\",\"amount\":" + amount + "}", null);
        return note;
    }

    @Transactional
    public SalesApi.InvoiceResponse issueInvoice(String id, String actor) {
        CustomerInvoice invoice = invoiceRepository.findById(id).orElseThrow(() -> error("AR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (invoice.getStatus() != CustomerInvoice.Status.DRAFT) return invoice(invoice);
        assertCreditAvailable(invoice.getCustomerId(), invoice.getAmount());
        invoice.issue(actor);
        invoiceRepository.save(invoice);
        ledgerRepository.save(new PartnerLedgerEntry(invoice.getCustomerId(), "CUSTOMER_INVOICE", invoice.getAmount(), invoice.getInvoiceNumber(), "Customer invoice", invoice.getInvoiceDate().atStartOfDay(ZoneOffset.UTC).toInstant(), actor));
        subledgerPostingService.postSubledgerEvent("SALES", "CUSTOMER_INVOICE", invoice.getId(),
                "CUSTOMER_INVOICE_ISSUED", "AR:INVOICE:" + invoice.getId(), invoice.getInvoiceDate(),
                "Customer invoice " + invoice.getInvoiceNumber(), invoice.getAmount(), invoice.getAmount(), null,
                invoice.getCustomerId(), invoice.getCurrencyCode(), actor);
        auditService.record("ISSUE", "CUSTOMER_INVOICE", id, actor, "{\"amount\":" + invoice.getAmount() + "}", null);
        return invoice(invoice);
    }

    @Transactional
    public SalesApi.ReceiptResponse recordReceipt(SalesApi.ReceiptRequest request, String actor) {
        Optional<CustomerReceipt> replay = receiptRepository.findByOperationId(request.operationId());
        if (replay.isPresent()) return receipt(replay.get());
        if (receiptRepository.existsByReceiptNumberIgnoreCase(request.receiptNumber()))
            throw error("AR_RECEIPT_NUMBER_EXISTS", HttpStatus.CONFLICT);
        requireCustomer(request.customerId());
        List<SalesApi.AllocationRequest> requested = request.allocations() == null ? List.of() : request.allocations();
        if (requested.stream().map(SalesApi.AllocationRequest::invoiceId).distinct().count() != requested.size())
            throw error("AR_DUPLICATE_ALLOCATION", HttpStatus.BAD_REQUEST);
        BigDecimal allocated = requested.stream().map(SalesApi.AllocationRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocated.compareTo(request.amount()) > 0)
            throw error("AR_ALLOCATION_EXCEEDS_RECEIPT", HttpStatus.CONFLICT);
        Map<String, CustomerInvoice> invoices = requested.isEmpty() ? Map.of() : invoiceRepository.findAllByIdForUpdate(requested.stream().map(SalesApi.AllocationRequest::invoiceId).toList()).stream().collect(Collectors.toMap(CustomerInvoice::getId, Function.identity()));
        if (invoices.size() != requested.size()) throw error("AR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND);
        CustomerReceipt receipt = new CustomerReceipt(request.receiptNumber(), request.customerId(), date(request.receiptDate()), request.currencyCode(), request.amount(), request.operationId(), actor);
        List<CustomerReceiptAllocation> allocations = new ArrayList<>();
        for (SalesApi.AllocationRequest row : requested) {
            CustomerInvoice invoice = invoices.get(row.invoiceId());
            if (!invoice.getCustomerId().equals(request.customerId()) || !invoice.getCurrencyCode().equalsIgnoreCase(request.currencyCode()))
                throw error("AR_ALLOCATION_CUSTOMER_CURRENCY_MISMATCH", HttpStatus.CONFLICT);
            try {
                invoice.allocate(row.amount());
                receipt.allocate(row.amount());
            } catch (IllegalArgumentException | IllegalStateException ex) {
                throw error("AR_ALLOCATION_EXCEEDS_OUTSTANDING", HttpStatus.CONFLICT);
            }
            invoiceRepository.save(invoice);
            allocations.add(new CustomerReceiptAllocation(receipt.getId(), invoice.getId(), row.amount()));
            if (invoice.getStatus() == CustomerInvoice.Status.PAID)
                taskRepository.findByInvoiceId(invoice.getId()).ifPresent(CollectionTask::close);
        }
        receipt = receiptRepository.save(receipt);
        allocationRepository.saveAll(allocations);
        ledgerRepository.save(new PartnerLedgerEntry(request.customerId(), "CUSTOMER_RECEIPT", request.amount().negate(), request.receiptNumber(), "Customer receipt", receipt.getReceiptDate().atStartOfDay(ZoneOffset.UTC).toInstant(), actor));
        subledgerPostingService.postSubledgerEvent("SALES", "CUSTOMER_RECEIPT", receipt.getId(),
                "CUSTOMER_RECEIPT_RECORDED", "AR:RECEIPT:" + request.operationId(), receipt.getReceiptDate(),
                "Customer receipt " + receipt.getReceiptNumber(), receipt.getAmount(), receipt.getAmount(), null,
                request.customerId(), request.currencyCode(), actor);
        auditService.record("RECEIPT", "CUSTOMER_RECEIPT", receipt.getId(), actor, "{\"operationId\":\"" + request.operationId() + "\",\"amount\":" + request.amount() + "}", null);
        return receipt(receipt);
    }

    @Transactional(readOnly = true)
    public SalesApi.AgingResponse aging(long asOfMillis) {
        if (asOfMillis <= 0) throw error("AR_AS_OF_DATE_REQUIRED", HttpStatus.BAD_REQUEST);
        LocalDate asOf = date(asOfMillis);
        BigDecimal current = BigDecimal.ZERO, b1 = BigDecimal.ZERO, b2 = BigDecimal.ZERO, b3 = BigDecimal.ZERO, b4 = BigDecimal.ZERO;
        for (CustomerInvoice invoice : invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc()) {
            if (invoice.getOutstandingAmount().signum() == 0) continue;
            long days = ChronoUnit.DAYS.between(invoice.getDueDate(), asOf);
            if (days <= 0) current = current.add(invoice.getOutstandingAmount());
            else if (days <= 30) b1 = b1.add(invoice.getOutstandingAmount());
            else if (days <= 60) b2 = b2.add(invoice.getOutstandingAmount());
            else if (days <= 90) b3 = b3.add(invoice.getOutstandingAmount());
            else b4 = b4.add(invoice.getOutstandingAmount());
        }
        return new SalesApi.AgingResponse(ms(asOf), current, b1, b2, b3, b4, current.add(b1).add(b2).add(b3).add(b4));
    }

    @Transactional
    public List<SalesApi.CollectionTaskResponse> collections(LocalDate asOf) {
        Map<String, CustomerInvoice> invoices = invoiceRepository.findAllByOrderByInvoiceDateDescCreatedAtDesc().stream().collect(Collectors.toMap(CustomerInvoice::getId, Function.identity()));
        for (CustomerInvoice invoice : invoices.values()) {
            if (invoice.overdue(asOf) && taskRepository.findByInvoiceId(invoice.getId()).isEmpty())
                taskRepository.save(new CollectionTask(invoice.getId()));
            if (invoice.getStatus() == CustomerInvoice.Status.PAID)
                taskRepository.findByInvoiceId(invoice.getId()).ifPresent(CollectionTask::close);
        }
        return taskRepository.findAllByOrderByNextActionDateAscCreatedAtAsc().stream().map(t -> task(t, invoices.get(t.getInvoiceId()), asOf)).toList();
    }

    @Transactional
    public SalesApi.CollectionTaskResponse updateTask(String id, SalesApi.CollectionTaskRequest request, String actor) {
        CollectionTask task = taskRepository.findById(id).orElseThrow(() -> error("AR_COLLECTION_TASK_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (task.getVersion() != request.version()) throw error("STALE_STATE", HttpStatus.CONFLICT);
        CollectionTask.Status status;
        try {
            status = CollectionTask.Status.valueOf(request.status());
        } catch (Exception ex) {
            throw error("AR_COLLECTION_STATUS_INVALID", HttpStatus.BAD_REQUEST);
        }
        task.update(status, request.ownerUserId(), request.nextActionDate() > 0 ? date(request.nextActionDate()) : null, request.note());
        taskRepository.save(task);
        auditService.record("UPDATE", "AR_COLLECTION_TASK", id, actor, "{\"status\":\"" + status + "\"}", null);
        CustomerInvoice invoice = invoiceRepository.findById(task.getInvoiceId()).orElseThrow(() -> error("AR_INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return task(task, invoice, date(request.asOf()));
    }

    private BusinessParty requireCustomer(String id) {
        BusinessParty party = partyRepository.findById(id).orElseThrow(() -> error("AR_CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if ("SUPPLIER".equals(party.getPartyType()) || !party.isActive())
            throw error("AR_CUSTOMER_INACTIVE", HttpStatus.CONFLICT);
        return party;
    }

    private SalesApi.CreditProfileResponse creditResponse(CustomerCreditProfile p, String customer) {
        BigDecimal outstanding = invoiceRepository.outstanding(customer), limit = p == null ? BigDecimal.ZERO : p.getCreditLimit();
        return new SalesApi.CreditProfileResponse(customer, limit, p == null ? 30 : p.getPaymentTermsDays(), p != null && p.isCreditHold(), outstanding, p == null ? BigDecimal.ZERO : limit.subtract(outstanding), p == null ? 0 : p.getVersion());
    }

    private SalesApi.InvoiceResponse invoice(CustomerInvoice i) {
        return new SalesApi.InvoiceResponse(i.getId(), i.getInvoiceNumber(), i.getCustomerId(), i.getSalesOrderId(), ms(i.getInvoiceDate()), ms(i.getDueDate()), i.getCurrencyCode(), i.getAmount(), i.getOutstandingAmount(), i.getStatus().name(), i.getVersion());
    }

    private SalesApi.ReceiptResponse receipt(CustomerReceipt r) {
        return new SalesApi.ReceiptResponse(r.getId(), r.getReceiptNumber(), r.getCustomerId(), ms(r.getReceiptDate()), r.getCurrencyCode(), r.getAmount(), r.getUnallocatedAmount(), r.getOperationId(), allocationRepository.findByReceiptId(r.getId()).stream().map(a -> new SalesApi.AllocationResponse(a.getInvoiceId(), a.getAmount())).toList());
    }

    private SalesApi.CollectionTaskResponse task(CollectionTask t, CustomerInvoice i, LocalDate asOf) {
        return new SalesApi.CollectionTaskResponse(t.getId(), t.getInvoiceId(), i == null ? "—" : i.getInvoiceNumber(), i == null ? "" : i.getCustomerId(), i == null ? BigDecimal.ZERO : i.getOutstandingAmount(), i == null ? 0 : ms(i.getDueDate()), i == null ? 0 : (int) Math.max(0, ChronoUnit.DAYS.between(i.getDueDate(), asOf)), t.getStatus().name(), t.getOwnerUserId(), t.getNextActionDate() == null ? 0 : ms(t.getNextActionDate()), t.getNote(), t.getVersion());
    }
}
