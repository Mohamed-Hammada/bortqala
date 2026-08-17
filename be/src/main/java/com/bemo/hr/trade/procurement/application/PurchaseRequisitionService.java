package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisitionLine;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionLineRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
        PurchaseRequisition req = new PurchaseRequisition(requisitionNumber, departmentId, requestedBy);
        return requisitionRepository.save(req);
    }

    @Transactional
    public PurchaseRequisitionLine addRequisitionLine(String requisitionId, String itemId, String itemName, BigDecimal requestedQuantity, BigDecimal unitPriceEstimate, String notes) {
        PurchaseRequisition req = getRequisition(requisitionId);
        if (req.getStatus() != PurchaseRequisition.Status.DRAFT) {
            throw new BusinessRuleException("Lines can only be added to DRAFT requisitions", "REQUISITION_NOT_DRAFT", HttpStatus.CONFLICT);
        }
        PurchaseRequisitionLine line = new PurchaseRequisitionLine(requisitionId, itemId, itemName, requestedQuantity, unitPriceEstimate, notes);
        return requisitionLineRepository.save(line);
    }

    @Transactional
    public PurchaseRequisition submitRequisition(String requisitionId) {
        PurchaseRequisition req = getRequisition(requisitionId);
        List<PurchaseRequisitionLine> lines = requisitionLineRepository.findByRequisitionId(requisitionId);
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cannot submit a requisition without lines", "REQUISITION_NO_LINES", HttpStatus.CONFLICT);
        }
        req.submit();
        return requisitionRepository.save(req);
    }

    @Transactional
    public PurchaseRequisition approveRequisition(String requisitionId) {
        PurchaseRequisition req = getRequisition(requisitionId);
        req.approve();
        return requisitionRepository.save(req);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequisition> getApprovedRequisitions() {
        return requisitionRepository.findByStatus(PurchaseRequisition.Status.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequisitionLine> getRequisitionLines(String requisitionId) {
        return requisitionLineRepository.findByRequisitionId(requisitionId);
    }

    private PurchaseRequisition getRequisition(String id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Purchase Requisition not found", "REQUISITION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
