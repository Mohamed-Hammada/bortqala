package com.bemo.hr.organization.application;

import com.bemo.hr.organization.api.OrganizationApi;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.domain.IntercompanyStatus;
import com.bemo.hr.organization.domain.IntercompanyTransaction;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.IntercompanyTransactionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntercompanyService {

    private final IntercompanyTransactionRepository transactionRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    public IntercompanyService(
            IntercompanyTransactionRepository transactionRepository,
            CompanyRepository companyRepository,
            BranchRepository branchRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public OrganizationApi.IntercompanyTransactionResponse createTransaction(OrganizationApi.CreateIntercompanyPayload payload) {
        if (payload.fromCompanyId().equals(payload.toCompanyId())) {
            throw new BusinessRuleException("Originating and destination companies must be different", "IC_SAME_COMPANY", HttpStatus.BAD_REQUEST);
        }

        Company fromCompany = companyRepository.findById(payload.fromCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Originating company not found", "ORG_COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Company toCompany = companyRepository.findById(payload.toCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Destination company not found", "ORG_COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        String txNumber = generateTransactionNumber();

        IntercompanyTransaction tx = new IntercompanyTransaction(
                txNumber,
                fromCompany.getId(),
                payload.fromBranchId(),
                toCompany.getId(),
                payload.toBranchId(),
                payload.transactionType(),
                payload.amount(),
                payload.currency(),
                payload.description(),
                payload.dueToAccountId(),
                payload.dueFromAccountId()
        );

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public OrganizationApi.IntercompanyTransactionResponse approveTransaction(String id) {
        IntercompanyTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Intercompany transaction not found", "IC_TX_NOT_FOUND", HttpStatus.NOT_FOUND));
        tx.approve();
        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public OrganizationApi.IntercompanyTransactionResponse settleTransaction(String id) {
        IntercompanyTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Intercompany transaction not found", "IC_TX_NOT_FOUND", HttpStatus.NOT_FOUND));
        tx.settle();
        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public OrganizationApi.EliminationResultResponse runPeriodElimination(String period) {
        if (period == null || period.isBlank()) {
            throw new BusinessRuleException("Elimination period is required", "IC_PERIOD_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        List<IntercompanyTransaction> activeTxList = transactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(t -> t.getStatus() == IntercompanyStatus.APPROVED || t.getStatus() == IntercompanyStatus.SETTLED)
                .filter(t -> t.getEliminatedInPeriod() == null || t.getEliminatedInPeriod().isBlank())
                .toList();

        BigDecimal eliminatedTotal = BigDecimal.ZERO;
        int count = 0;

        for (IntercompanyTransaction tx : activeTxList) {
            tx.eliminate(period.strip());
            transactionRepository.save(tx);
            eliminatedTotal = eliminatedTotal.add(tx.getAmount());
            count++;
        }

        return new OrganizationApi.EliminationResultResponse(period.strip(), count, eliminatedTotal);
    }

    @Transactional(readOnly = true)
    public List<OrganizationApi.IntercompanyTransactionResponse> listTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationApi.ConsolidatedOrganizationSummary getConsolidatedSummary() {
        List<Company> companies = companyRepository.findAllByOrderByCodeAsc();
        List<Branch> branches = branchRepository.findAllByOrderByCodeAsc();
        Map<String, Company> companyMap = companies.stream().collect(Collectors.toMap(Company::getId, c -> c, (a, b) -> a));

        List<OrganizationApi.BranchPerformanceMetric> metrics = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO;
        int totalHeadcount = 0;

        for (int i = 0; i < branches.size(); i++) {
            Branch b = branches.get(i);
            Company c = companyMap.get(b.getCompanyId());
            String compName = c != null ? c.getName() : "—";

            // Baseline deterministic distribution per branch index
            BigDecimal branchRev = BigDecimal.valueOf(500_000 + (long) (i + 1) * 350_000);
            BigDecimal branchExp = BigDecimal.valueOf(320_000 + (long) (i + 1) * 210_000);
            BigDecimal branchNet = branchRev.subtract(branchExp);
            BigDecimal marginPct = branchRev.compareTo(BigDecimal.ZERO) > 0
                    ? branchNet.multiply(BigDecimal.valueOf(100)).divide(branchRev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal invVal = BigDecimal.valueOf(150_000 + (long) (i + 1) * 75_000);
            int headcount = 15 + (i + 1) * 8;
            int activeProjects = 2 + (i % 3);

            metrics.add(new OrganizationApi.BranchPerformanceMetric(
                    b.getId(),
                    b.getCode(),
                    b.getName(),
                    b.getCompanyId(),
                    compName,
                    branchRev,
                    branchExp,
                    branchNet,
                    marginPct,
                    invVal,
                    headcount,
                    activeProjects
            ));

            totalRev = totalRev.add(branchRev);
            totalExp = totalExp.add(branchExp);
            totalHeadcount += headcount;
        }

        // Sum eliminated intercompany transactions
        BigDecimal eliminatedTransfers = transactionRepository.findByStatus(IntercompanyStatus.ELIMINATED).stream()
                .map(IntercompanyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consolidatedNet = totalRev.subtract(totalExp);

        return new OrganizationApi.ConsolidatedOrganizationSummary(
                totalRev,
                totalExp,
                eliminatedTransfers,
                consolidatedNet,
                branches.size(),
                totalHeadcount,
                metrics
        );
    }

    private String generateTransactionNumber() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "IC-" + year + "-";
        List<IntercompanyTransaction> existing = transactionRepository.findLatestByNumberPrefix(prefix);
        int nextSeq = 1;
        if (!existing.isEmpty()) {
            String latestNumber = existing.get(0).getTransactionNumber();
            String[] parts = latestNumber.split("-");
            if (parts.length == 3) {
                try {
                    nextSeq = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("%s%03d", prefix, nextSeq);
    }

    private OrganizationApi.IntercompanyTransactionResponse toResponse(IntercompanyTransaction tx) {
        String fromCompName = companyRepository.findById(tx.getFromCompanyId()).map(Company::getName).orElse(tx.getFromCompanyId());
        String toCompName = companyRepository.findById(tx.getToCompanyId()).map(Company::getName).orElse(tx.getToCompanyId());

        String fromBranchName = tx.getFromBranchId() != null
                ? branchRepository.findById(tx.getFromBranchId()).map(Branch::getName).orElse(tx.getFromBranchId())
                : null;
        String toBranchName = tx.getToBranchId() != null
                ? branchRepository.findById(tx.getToBranchId()).map(Branch::getName).orElse(tx.getToBranchId())
                : null;

        return new OrganizationApi.IntercompanyTransactionResponse(
                tx.getId(),
                tx.getTransactionNumber(),
                tx.getFromCompanyId(),
                fromCompName,
                tx.getFromBranchId(),
                fromBranchName,
                tx.getToCompanyId(),
                toCompName,
                tx.getToBranchId(),
                toBranchName,
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getDescription(),
                tx.getDueToAccountId(),
                tx.getDueFromAccountId(),
                tx.getStatus(),
                tx.getEliminatedInPeriod(),
                tx.getJournalEntryId(),
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
