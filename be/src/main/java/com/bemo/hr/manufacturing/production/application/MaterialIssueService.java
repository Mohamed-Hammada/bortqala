package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialIssueHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialIssueLine;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueLineRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class MaterialIssueService {

    private final MaterialIssueHeaderRepository issueHeaderRepository;
    private final MaterialIssueLineRepository issueLineRepository;

    public MaterialIssueService(MaterialIssueHeaderRepository issueHeaderRepository,
                                MaterialIssueLineRepository issueLineRepository) {
        this.issueHeaderRepository = issueHeaderRepository;
        this.issueLineRepository = issueLineRepository;
    }

    @Transactional
    public MaterialIssueHeader createIssue(String issueNumber, String productionOrderId, LocalDate issueDate) {
        log.debug("createIssue called with issueNumber={}, productionOrderId={}", issueNumber, productionOrderId);
        MaterialIssueHeader header = new MaterialIssueHeader(issueNumber, productionOrderId, issueDate);
        MaterialIssueHeader saved = issueHeaderRepository.save(header);
        log.info("MaterialIssueHeader {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public MaterialIssueLine addIssueLine(String issueId, String itemId, BigDecimal quantity, String warehouseId) {
        log.debug("addIssueLine called with issueId={}, itemId={}", issueId, itemId);
        MaterialIssueLine line = new MaterialIssueLine(issueId, itemId, quantity, warehouseId);
        MaterialIssueLine saved = issueLineRepository.save(line);
        log.info("MaterialIssueLine {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public MaterialIssueHeader cancelIssue(String issueId) {
        log.debug("cancelIssue called with issueId={}", issueId);
        MaterialIssueHeader header = issueHeaderRepository.findById(issueId)
                .orElseThrow(() -> new BusinessRuleException("Material issue not found", "MATERIAL_ISSUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        header.cancel();
        MaterialIssueHeader saved = issueHeaderRepository.save(header);
        log.info("MaterialIssueHeader {} cancelled successfully", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MaterialIssueHeader> getIssuesByProductionOrder(String productionOrderId) {
        log.debug("getIssuesByProductionOrder called with productionOrderId={}", productionOrderId);
        return issueHeaderRepository.findByProductionOrderId(productionOrderId);
    }
}
