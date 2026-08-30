package com.bemo.hr.serviceops.application;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.domain.WorkOrder;
import com.bemo.hr.serviceops.domain.WorkOrderLaborLine;
import com.bemo.hr.serviceops.domain.WorkOrderPartsLine;
import com.bemo.hr.serviceops.infrastructure.WorkOrderLaborLineRepository;
import com.bemo.hr.serviceops.infrastructure.WorkOrderPartsLineRepository;
import com.bemo.hr.serviceops.infrastructure.WorkOrderRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderLaborLineRepository laborLineRepository;
    private final WorkOrderPartsLineRepository partsLineRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderLaborLineRepository laborLineRepository,
                            WorkOrderPartsLineRepository partsLineRepository) {
        this.workOrderRepository = workOrderRepository;
        this.laborLineRepository = laborLineRepository;
        this.partsLineRepository = partsLineRepository;
    }

    @Transactional
    public ServiceOpsApi.WorkOrderResponse createWorkOrder(ServiceOpsApi.WorkOrderCreateRequest request) {
        String appId = TenantContext.require();
        WorkOrder workOrder = new WorkOrder(
                appId,
                request.ticketNo(),
                request.customerPartyId(),
                request.customerName(),
                request.title(),
                request.description(),
                request.assignedEmployeeId(),
                request.priority(),
                request.promisedAt()
        );
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Created work order {} for app {}", saved.getTicketNo(), appId);
        return toWorkOrderResponse(saved);
    }

    public List<ServiceOpsApi.WorkOrderResponse> listWorkOrders() {
        String appId = TenantContext.require();
        return workOrderRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toWorkOrderResponse)
                .collect(Collectors.toList());
    }

    public ServiceOpsApi.WorkOrderResponse getWorkOrder(String id) {
        String appId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("WORK_ORDER_NOT_FOUND", "WORK_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toWorkOrderResponse(workOrder);
    }

    @Transactional
    public ServiceOpsApi.WorkOrderResponse addLaborLine(String id, ServiceOpsApi.AddLaborLineRequest request) {
        String appId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("WORK_ORDER_NOT_FOUND", "WORK_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (workOrder.getStatus() == WorkOrder.Status.DELIVERED || workOrder.getStatus() == WorkOrder.Status.CANCELLED) {
            throw new BusinessRuleException("WORK_ORDER_INVALID_STATE", "WORK_ORDER_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }

        WorkOrderLaborLine laborLine = new WorkOrderLaborLine(
                appId,
                request.description(),
                request.hours(),
                request.hourlyRate()
        );
        workOrder.addLaborLine(laborLine);
        WorkOrder saved = workOrderRepository.save(workOrder);
        return toWorkOrderResponse(saved);
    }

    @Transactional
    public ServiceOpsApi.WorkOrderResponse addPartsLine(String id, ServiceOpsApi.AddPartsLineRequest request) {
        String appId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("WORK_ORDER_NOT_FOUND", "WORK_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (workOrder.getStatus() == WorkOrder.Status.DELIVERED || workOrder.getStatus() == WorkOrder.Status.CANCELLED) {
            throw new BusinessRuleException("WORK_ORDER_INVALID_STATE", "WORK_ORDER_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }

        WorkOrderPartsLine partsLine = new WorkOrderPartsLine(
                appId,
                request.itemCode(),
                request.itemName(),
                request.quantity(),
                request.unitPrice()
        );
        workOrder.addPartsLine(partsLine);
        WorkOrder saved = workOrderRepository.save(workOrder);
        return toWorkOrderResponse(saved);
    }

    @Transactional
    public ServiceOpsApi.WorkOrderResponse updateStatus(String id, ServiceOpsApi.UpdateWorkOrderStatusRequest request) {
        String appId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("WORK_ORDER_NOT_FOUND", "WORK_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        WorkOrder.Status current = workOrder.getStatus();
        WorkOrder.Status next = request.status();

        if (current == WorkOrder.Status.DELIVERED || current == WorkOrder.Status.CANCELLED) {
            throw new BusinessRuleException("WORK_ORDER_INVALID_STATE", "WORK_ORDER_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }

        // Enforce WAITING_PARTS -> DONE rule
        if (current == WorkOrder.Status.WAITING_PARTS && next == WorkOrder.Status.DONE) {
            boolean hasParts = !workOrder.getPartsLines().isEmpty();
            boolean hasOverride = request.overrideNote() != null && !request.overrideNote().trim().isEmpty();
            if (!hasParts && !hasOverride) {
                throw new BusinessRuleException("WORK_ORDER_PARTS_REQUIRED", "WORK_ORDER_PARTS_REQUIRED", HttpStatus.BAD_REQUEST);
            }
        }

        if (request.overrideNote() != null && !request.overrideNote().isBlank()) {
            workOrder.setOverrideNote(request.overrideNote());
        }

        workOrder.setStatus(next);
        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Updated work order {} status to {}", saved.getTicketNo(), next);
        return toWorkOrderResponse(saved);
    }

    @Transactional
    public ServiceOpsApi.WorkOrderResponse deliverAndCreateInvoice(String id) {
        String appId = TenantContext.require();
        WorkOrder workOrder = workOrderRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("WORK_ORDER_NOT_FOUND", "WORK_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (workOrder.getStatus() != WorkOrder.Status.DONE) {
            throw new BusinessRuleException("WORK_ORDER_INVALID_STATE", "WORK_ORDER_INVALID_STATE", HttpStatus.BAD_REQUEST);
        }



        String invoiceDraftId = "INV-DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        workOrder.setInvoiceId(invoiceDraftId);
        workOrder.setStatus(WorkOrder.Status.DELIVERED);

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Delivered work order {} and generated draft invoice {}", saved.getTicketNo(), invoiceDraftId);
        return toWorkOrderResponse(saved);
    }

    // --- Mapper ---

    private ServiceOpsApi.WorkOrderResponse toWorkOrderResponse(WorkOrder wo) {
        List<ServiceOpsApi.WorkOrderLaborLineResponse> laborResponses = wo.getLaborLines().stream()
                .map(l -> new ServiceOpsApi.WorkOrderLaborLineResponse(
                        l.getId(),
                        l.getDescription(),
                        l.getHours(),
                        l.getHourlyRate(),
                        l.getTotalAmount()
                ))
                .collect(Collectors.toList());

        List<ServiceOpsApi.WorkOrderPartsLineResponse> partsResponses = wo.getPartsLines().stream()
                .map(p -> new ServiceOpsApi.WorkOrderPartsLineResponse(
                        p.getId(),
                        p.getItemCode(),
                        p.getItemName(),
                        p.getQuantity(),
                        p.getUnitPrice(),
                        p.getTotalAmount()
                ))
                .collect(Collectors.toList());

        return new ServiceOpsApi.WorkOrderResponse(
                wo.getId(),
                wo.getTicketNo(),
                wo.getCustomerPartyId(),
                wo.getCustomerName(),
                wo.getTitle(),
                wo.getDescription(),
                wo.getAssignedEmployeeId(),
                wo.getPriority(),
                wo.getStatus(),
                wo.getPromisedAt(),
                wo.getLaborTotal(),
                wo.getPartsTotal(),
                wo.getGrandTotal(),
                wo.getInvoiceId(),
                wo.getOverrideNote(),
                laborResponses,
                partsResponses,
                wo.getCreatedAt(),
                wo.getUpdatedAt()
        );
    }
}
