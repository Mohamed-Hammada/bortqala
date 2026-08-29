package com.bemo.hr.project.application;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.domain.SiteCustody;
import com.bemo.hr.project.domain.SiteCustodyExpense;
import com.bemo.hr.project.domain.SiteCustodyReturn;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyExpenseRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyReturnRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteCustodyService {

    private final SiteCustodyRepository custodyRepository;
    private final SiteCustodyExpenseRepository expenseRepository;
    private final SiteCustodyReturnRepository returnRepository;
    private final ProjectRepository projectRepository;

    public SiteCustodyService(
            SiteCustodyRepository custodyRepository,
            SiteCustodyExpenseRepository expenseRepository,
            SiteCustodyReturnRepository returnRepository,
            ProjectRepository projectRepository) {
        this.custodyRepository = custodyRepository;
        this.expenseRepository = expenseRepository;
        this.returnRepository = returnRepository;
        this.projectRepository = projectRepository;
    }

    public SiteCustodyResponse issueCustody(String projectId, IssueCustodyRequest req) {
        String tenantId = TenantContext.require();
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Project not found with ID: " + projectId));

        SiteCustody custody = new SiteCustody(
                UUID.randomUUID().toString(),
                tenantId,
                projectId,
                req.custodyCode(),
                req.custodianEmployeeId(),
                req.custodianName(),
                req.custodyType(),
                req.initialAmount(),
                System.currentTimeMillis(),
                req.notes()
        );

        custody = custodyRepository.save(custody);
        return mapToResponse(custody);
    }

    @Transactional(readOnly = true)
    public List<SiteCustodyResponse> getCustodiesByProject(String projectId) {
        return custodyRepository.findByProjectIdOrderByIssuedAtDesc(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteCustodyResponse getCustody(String custodyId) {
        SiteCustody custody = custodyRepository.findById(custodyId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_NOT_FOUND", "Custody not found with ID: " + custodyId));
        return mapToResponse(custody);
    }

    public SiteCustodyExpenseResponse recordExpense(String custodyId, RecordCustodyExpenseRequest req) {
        String tenantId = TenantContext.require();
        SiteCustody custody = custodyRepository.findById(custodyId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_NOT_FOUND", "Custody not found with ID: " + custodyId));

        if (!"ACTIVE".equals(custody.getStatus())) {
            throw new BusinessRuleException("Cannot record expense on settled or closed custody.", "CUSTODY_ALREADY_SETTLED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (req.amount().compareTo(custody.getRemainingBalance()) > 0) {
            throw new BusinessRuleException("Expense amount exceeds remaining custody balance.", "CUSTODY_INSUFFICIENT_BALANCE", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        SiteCustodyExpense expense = new SiteCustodyExpense(
                UUID.randomUUID().toString(),
                tenantId,
                custodyId,
                req.expenseDate() > 0 ? req.expenseDate() : System.currentTimeMillis(),
                req.amount(),
                req.category(),
                req.description(),
                req.receiptNumber(),
                req.recordedBy()
        );

        expense = expenseRepository.save(expense);
        return mapToExpenseResponse(expense);
    }

    public SiteCustodyExpenseResponse approveExpense(String expenseId) {
        SiteCustodyExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_EXPENSE_NOT_FOUND", "Expense not found: " + expenseId));

        if (!"SUBMITTED".equals(expense.getStatus())) {
            throw new BusinessRuleException("Expense has already been decided.", "CUSTODY_EXPENSE_ALREADY_DECIDED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        String custodyId = expense.getCustodyId();
        SiteCustody custody = custodyRepository.findById(custodyId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_NOT_FOUND", "Custody not found: " + custodyId));

        if (expense.getAmount().compareTo(custody.getRemainingBalance()) > 0) {
            throw new BusinessRuleException("Expense amount exceeds remaining custody balance.", "CUSTODY_INSUFFICIENT_BALANCE", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        custody.deductBalance(expense.getAmount());
        custodyRepository.save(custody);

        expense.approve();
        expense = expenseRepository.save(expense);
        return mapToExpenseResponse(expense);
    }

    public SiteCustodyExpenseResponse rejectExpense(String expenseId) {
        SiteCustodyExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_EXPENSE_NOT_FOUND", "Expense not found: " + expenseId));

        if (!"SUBMITTED".equals(expense.getStatus())) {
            throw new BusinessRuleException("Expense has already been decided.", "CUSTODY_EXPENSE_ALREADY_DECIDED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        expense.reject();
        expense = expenseRepository.save(expense);
        return mapToExpenseResponse(expense);
    }

    public SiteCustodyResponse settleCustody(String custodyId, SettleCustodyRequest req) {
        String tenantId = TenantContext.require();
        SiteCustody custody = custodyRepository.findById(custodyId)
                .orElseThrow(() -> new NotFoundException("CUSTODY_NOT_FOUND", "Custody not found: " + custodyId));

        if (!"ACTIVE".equals(custody.getStatus())) {
            throw new BusinessRuleException("Custody is already settled or closed.", "CUSTODY_ALREADY_SETTLED", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (req.amountReturned().compareTo(BigDecimal.ZERO) > 0) {
            SiteCustodyReturn ret = new SiteCustodyReturn(
                    UUID.randomUUID().toString(),
                    tenantId,
                    custodyId,
                    System.currentTimeMillis(),
                    req.amountReturned(),
                    req.receivedBy(),
                    req.notes()
            );
            returnRepository.save(ret);
        }

        custody.settle();
        custody = custodyRepository.save(custody);
        return mapToResponse(custody);
    }

    private SiteCustodyResponse mapToResponse(SiteCustody c) {
        List<SiteCustodyExpenseResponse> expenses = expenseRepository.findByCustodyIdOrderByExpenseDateDesc(c.getId())
                .stream()
                .map(this::mapToExpenseResponse)
                .toList();

        List<SiteCustodyReturnResponse> returns = returnRepository.findByCustodyIdOrderByReturnDateDesc(c.getId())
                .stream()
                .map(this::mapToReturnResponse)
                .toList();

        return new SiteCustodyResponse(
                c.getId(),
                c.getProjectId(),
                c.getCustodyCode(),
                c.getCustodianEmployeeId(),
                c.getCustodianName(),
                c.getCustodyType(),
                c.getInitialAmount(),
                c.getRemainingBalance(),
                c.getStatus(),
                c.getIssuedAt(),
                c.getSettledAt(),
                c.getNotes(),
                c.getVersion(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                expenses,
                returns
        );
    }

    private SiteCustodyExpenseResponse mapToExpenseResponse(SiteCustodyExpense e) {
        return new SiteCustodyExpenseResponse(
                e.getId(),
                e.getCustodyId(),
                e.getExpenseDate(),
                e.getAmount(),
                e.getCategory(),
                e.getDescription(),
                e.getReceiptNumber(),
                e.getRecordedBy(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private SiteCustodyReturnResponse mapToReturnResponse(SiteCustodyReturn r) {
        return new SiteCustodyReturnResponse(
                r.getId(),
                r.getCustodyId(),
                r.getReturnDate(),
                r.getAmountReturned(),
                r.getReceivedBy(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
