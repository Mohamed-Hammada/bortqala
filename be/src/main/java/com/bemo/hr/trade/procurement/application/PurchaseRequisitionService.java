package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.procurement.domain.PurchaseRequisition;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseRequisitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PurchaseRequisitionService {

    private final PurchaseRequisitionRepository requisitionRepository;

    public PurchaseRequisitionService(PurchaseRequisitionRepository requisitionRepository) {
        this.requisitionRepository = requisitionRepository;
    }

    @Transactional
    public PurchaseRequisition createRequisition(String requisitionNumber, String departmentId, String requestedBy) {
        PurchaseRequisition req = new PurchaseRequisition(requisitionNumber, departmentId, requestedBy);
        return requisitionRepository.save(req);
    }

    @Transactional
    public PurchaseRequisition submitRequisition(String requisitionId) {
        PurchaseRequisition req = getRequisition(requisitionId);
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

    private PurchaseRequisition getRequisition(String id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Purchase Requisition not found", "REQUISITION_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
