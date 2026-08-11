package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.manufacturing.production.domain.MaterialIssueHeader;
import com.bemo.hr.manufacturing.production.domain.MaterialIssueLine;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.MaterialIssueLineRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        MaterialIssueHeader header = new MaterialIssueHeader(issueNumber, productionOrderId, issueDate);
        return issueHeaderRepository.save(header);
    }

    @Transactional
    public MaterialIssueLine addIssueLine(String issueId, String itemId, BigDecimal quantity, String warehouseId) {
        MaterialIssueLine line = new MaterialIssueLine(issueId, itemId, quantity, warehouseId);
        return issueLineRepository.save(line);
    }

    @Transactional
    public MaterialIssueHeader cancelIssue(String issueId) {
        MaterialIssueHeader header = issueHeaderRepository.findById(issueId)
                .orElseThrow(() -> new BusinessRuleException("Material issue not found", "MATERIAL_ISSUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        header.cancel();
        return issueHeaderRepository.save(header);
    }

    @Transactional(readOnly = true)
    public List<MaterialIssueHeader> getIssuesByProductionOrder(String productionOrderId) {
        return issueHeaderRepository.findByProductionOrderId(productionOrderId);
    }
}
