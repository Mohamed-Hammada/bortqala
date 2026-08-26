package com.bemo.hr.expenses.application;

import com.bemo.hr.expenses.api.ExpenseClaimApi;
import com.bemo.hr.expenses.domain.ExpenseClaim;
import com.bemo.hr.expenses.infrastructure.ExpenseClaimRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.operations.EmployeeAdvanceEntry;
import com.bemo.hr.expenses.infrastructure.ExpenseReimbursementEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseClaimService {
    private final ExpenseClaimRepository expenseClaimRepository;
    private final EmployeeRepository employeeRepository;
    private final ExpenseReimbursementEntryRepository advanceEntryRepository;

    @Value("${hr.expenses.sod-enabled:true}")
    private String sodEnabled;

    @Transactional
    public ExpenseClaim create(String username, ExpenseClaimApi.CreateClaimRequest request) {
        String employeeId = resolveEmployeeId(username);
        var claim = new ExpenseClaim(employeeId,
                ExpenseClaim.Category.valueOf(request.category().strip().toUpperCase()),
                request.spentOn(), request.amount(), request.currency(), request.description());
        if (request.attachmentName() != null && !request.attachmentName().isBlank()) {
            validateAttachment(request.attachmentName(), request.attachmentContentType(), request.attachmentSize());
            claim.assignReceipt(request.attachmentName(), request.attachmentContentType(), request.attachmentSize());
        }
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim update(String username, String claimId, ExpenseClaimApi.UpdateClaimRequest request) {
        String employeeId = resolveEmployeeId(username);
        ExpenseClaim claim = requireOwned(claimId, employeeId);
        if (ExpenseClaim.Status.valueOf(claim.getStatus()) != ExpenseClaim.Status.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT claims can be edited.", "EXPENSE_INVALID_STATE", HttpStatus.CONFLICT);
        }
        claim.setCategory(request.category().strip().toUpperCase());
        claim.setSpentOn(request.spentOn());
        claim.setAmount(request.amount());
        claim.setCurrency(request.currency() == null || request.currency().isBlank() ? "EGP" : request.currency().strip().toUpperCase());
        claim.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().strip());
        if (request.attachmentName() != null && !request.attachmentName().isBlank()) {
            validateAttachment(request.attachmentName(), request.attachmentContentType(), request.attachmentSize());
            claim.assignReceipt(request.attachmentName(), request.attachmentContentType(), request.attachmentSize());
        } else {
            claim.clearReceipt();
        }
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim submit(String username, String claimId) {
        String employeeId = resolveEmployeeId(username);
        ExpenseClaim claim = requireOwned(claimId, employeeId);
        claim.submit();
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim approve(String claimId, String note) {
        String approver = actor();
        ExpenseClaim claim = requireById(claimId);
        requireSubmitted(claim);
        if (sodActive()) {
            assertNotSelfApproval(claim, approver);
        }
        claim.approve(approver);
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim reject(String claimId, String note) {
        String approver = actor();
        ExpenseClaim claim = requireById(claimId);
        requireSubmitted(claim);
        if (sodActive()) {
            assertNotSelfApproval(claim, approver);
        }
        claim.reject(approver, note);
        return expenseClaimRepository.save(claim);
    }

    @Transactional
    public ExpenseClaim reimburse(String claimId, String reference) {
        ExpenseClaim claim = requireById(claimId);
        claim.reimburse(reference); // throws if not APPROVED
        ExpenseClaim saved = expenseClaimRepository.save(claim);
        EmployeeAdvanceEntry entry = new EmployeeAdvanceEntry(
                claim.getEmployeeId(),
                claim.getAmount().negate(),
                "EXPENSE_REIMBURSEMENT",
                "Reimbursement for claim " + claim.getId(),
                Instant.now(),
                actor());
        advanceEntryRepository.save(entry);
        return saved;
    }

    public List<ExpenseClaim> listMine(String username) {
        String employeeId = resolveEmployeeId(username);
        return expenseClaimRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    public List<ExpenseClaim> listPending() {
        return expenseClaimRepository.findByStatusOrderByCreatedAtDesc("SUBMITTED");
    }

    public ExpenseClaim getById(String id) {
        return requireById(id);
    }

    private ExpenseClaim requireById(String id) {
        return expenseClaimRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense claim not found: " + id));
    }

    private ExpenseClaim requireOwned(String claimId, String employeeId) {
        ExpenseClaim claim = requireById(claimId);
        if (!claim.getEmployeeId().equals(employeeId)) {
            throw new BusinessRuleException(
                    "You can only access your own expense claims.",
                    "EXPENSE_NOT_OWN", HttpStatus.FORBIDDEN);
        }
        return claim;
    }

    private void requireSubmitted(ExpenseClaim claim) {
        if (ExpenseClaim.Status.valueOf(claim.getStatus()) != ExpenseClaim.Status.SUBMITTED) {
            throw new BusinessRuleException(
                    "Only SUBMITTED claims can be approved/rejected.",
                    "EXPENSE_INVALID_STATE", HttpStatus.CONFLICT);
        }
    }

    private void assertNotSelfApproval(ExpenseClaim claim, String approver) {
        if (approver == null) return;
        String claimant = employeeRepository.findById(claim.getEmployeeId())
                .map(e -> e.getDeviceUserId())
                .orElse(null);
        if (approver.equals(claimant)) {
            throw new BusinessRuleException(
                    "You cannot approve your own expense claim.",
                    "EXPENSE_SELF_APPROVAL", HttpStatus.CONFLICT);
        }
    }

    private String resolveEmployeeId(String username) {
        return employeeRepository.findByDeviceUserId(username == null ? "" : username.strip())
                .map(e -> e.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No employee record is linked to the signed-in user.",
                        "EXPENSE_NO_EMPLOYEE_LINKED", HttpStatus.CONFLICT));
    }

    private void validateAttachment(String name, String contentType, Long size) {
        if (size != null && size > 5 * 1024 * 1024) {
            throw new BusinessRuleException(
                    "Receipt file must be 5MB or smaller.",
                    "EXPENSE_RECEIPT_TOO_LARGE", HttpStatus.BAD_REQUEST);
        }
        if (contentType != null && !contentType.isBlank()) {
            String type = contentType.toLowerCase();
            boolean allowed = type.startsWith("image/")
                    || type.equals("application/pdf")
                    || type.equals("application/vnd.ms-excel")
                    || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            if (!allowed) {
                throw new BusinessRuleException(
                        "Receipt must be an image, PDF, or Excel file.",
                        "EXPENSE_RECEIPT_INVALID_TYPE", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean sodActive() {
        return !"false".equalsIgnoreCase(sodEnabled);
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}
