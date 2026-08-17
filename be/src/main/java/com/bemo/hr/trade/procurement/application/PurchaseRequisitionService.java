package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisitionLine;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository requisitionRepository;
    private final PurchaseRequisitionLineRepository requisitionLineRepository;

    public PurchaseRequisitionService(PurchaseRequisitionRepository requisitionRepository,
                                      PurchaseRequisitionLineRepository requisitionLineRepository) {
        this.requisitionRepository = requisitionRepository;
        this.requisitionLineRepository = requisitionLineRepository;
    }

    @Transactional
    public PurchaseRequisition createRequisition(String requisitionNumber, String departmentId, String requestedBy) {
        log.debug("createRequisition called with requisitionNumber={}, departmentId={}, requestedBy={}", requisitionNumber, departmentId, requestedBy);
        PurchaseRequisition req = new PurchaseRequisition(requisitionNumber, departmentId, requestedBy);
        PurchaseRequisition saved = requisitionRepository.save(req);
        log.info("PurchaseRequisition {} created successfully", saved.getId());
        return saved;
    }

    @Transactional
    public PurchaseRequisitionLine addRequisitionLine(String requisitionId, String itemId, String itemName, BigDecimal requestedQuantity, BigDecimal unitPriceEstimate, String notes) {
        log.debug("addRequisitionLine called with requisitionId={}, itemId={}, requestedQuantity={}", requisitionId, itemId, requestedQuantity);
        PurchaseRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != PurchaseRequisition.Status.DRAFT) {
            log.warn("Validation failed: Lines can only be added to DRAFT requisitions, current status={}", req.getStatus());
            throw new BusinessRuleException("Lines can only be added to DRAFT requisitions", "REQUISITION_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        PurchaseRequisitionLine line = new PurchaseRequisitionLine(requisitionId, itemId, itemName, requestedQuantity, unitPriceEstimate, notes);
        PurchaseRequisitionLine saved = requisitionLineRepository.save(line);
        log.info("RequisitionLine {} added to requisition {} successfully", saved.getId(), requisitionId);
        return saved;
    }

    @Transactional
    public PurchaseRequisition submitRequisition(String requisitionId) {
        log.debug("submitRequisition called with requisitionId={}", requisitionId);
        PurchaseRequisition req = getRequisition(requisitionId);
        List<PurchaseRequisitionLine> lines = requisitionLineRepository.findByRequisitionId(requisitionId);
        if (lines.isEmpty()) {
            log.warn("Validation failed: Cannot submit requisition {} without lines", requisitionId);
            throw new BusinessRuleException("Cannot submit a requisition without lines", "REQUISITION_NO_LINES", HttpStatus.CONFLICT);
        }
        req.submit();
        PurchaseRequisition saved = requisitionRepository.save(req);
        log.info("PurchaseRequisition {} submitted successfully", requisitionId);
        return saved;
    }

    @Transactional
    public PurchaseRequisition approveRequisition(String requisitionId) {
        log.debug("approveRequisition called with requisitionId={}", requisitionId);
        PurchaseRequisition req = getRequisition(requisitionId);
        req.approve();
        PurchaseRequisition saved = requisitionRepository.save(req);
        log.info("PurchaseRequisition {} approved successfully", requisitionId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequisition> getApprovedRequisitions() {
        log.debug("getApprovedRequisitions called");
        List<PurchaseRequisition> results = requisitionRepository.findByStatus(PurchaseRequisition.Status.APPROVED);
        log.debug("getApprovedRequisitions returned {} results", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequisitionLine> getRequisitionLines(String requisitionId) {
        log.debug("getRequisitionLines called with requisitionId={}", requisitionId);
        return requisitionLineRepository.findByRequisitionId(requisitionId);
    }

    private PurchaseRequisition getRequisition(String id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Purchase Requisition not found", "REQUISITION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
