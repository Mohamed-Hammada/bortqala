package com.bemo.hr.medical.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.LabOrder;
import com.bemo.hr.medical.domain.LabTestItem;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.LabOrderRepository;
import com.bemo.hr.medical.infrastructure.LabTestItemRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional
public class MedicalLabService {

    private final LabTestItemRepository testRepository;
    private final LabOrderRepository orderRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;

    public MedicalLabService(LabTestItemRepository testRepository,
                             LabOrderRepository orderRepository,
                             PatientRepository patientRepository,
                             EmployeeRepository employeeRepository) {
        this.testRepository = testRepository;
        this.orderRepository = orderRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
    }

    public LabTestItemDto saveLabTest(SaveLabTestItemRequest request) {
        String appId = TenantContext.require();

        LabTestItem.Category category = LabTestItem.Category.LAB;
        if (request.category() != null && !request.category().trim().isEmpty()) {
            try {
                category = LabTestItem.Category.valueOf(request.category().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        LabTestItem item = testRepository.findByAppIdAndCode(appId, request.code())
                .orElseGet(() -> new LabTestItem(
                        request.code(),
                        LabTestItem.Category.LAB,
                        request.name(),
                        request.sampleType(),
                        request.normalRangeText(),
                        request.price()
                ));

        item.setCategory(category);
        item.setName(request.name());
        item.setSampleType(request.sampleType());
        item.setNormalRangeText(request.normalRangeText());
        item.setPrice(request.price());
        item.setUpdatedAt(Instant.now().toEpochMilli());

        LabTestItem saved = testRepository.save(item);
        log.info("Saved lab test {} ({}) in tenant {}", saved.getName(), saved.getCode(), appId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LabTestItemDto> getAllLabTests(String categoryStr) {
        String appId = TenantContext.require();
        if (categoryStr != null && !categoryStr.trim().isEmpty()) {
            try {
                LabTestItem.Category cat = LabTestItem.Category.valueOf(categoryStr.trim().toUpperCase());
                return testRepository.findAllByAppIdAndCategoryOrderByCodeAsc(appId, cat)
                        .stream().map(this::toDto).toList();
            } catch (IllegalArgumentException ignored) {}
        }
        return testRepository.findAllByAppIdOrderByCodeAsc(appId)
                .stream().map(this::toDto).toList();
    }

    public LabOrderDto createLabOrder(CreateLabOrderRequest request) {
        String appId = TenantContext.require();

        Patient patient = patientRepository.findByAppIdAndId(appId, request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found", "PATIENT_NOT_FOUND"));

        LabTestItem test = testRepository.findByAppIdAndId(appId, request.testId())
                .orElseThrow(() -> new NotFoundException("Test not found", "LAB_TEST_NOT_FOUND"));

        LabOrder order = new LabOrder(
                patient.getId(),
                request.visitId(),
                request.doctorEmployeeId(),
                test.getId(),
                test.getCategory(),
                test.getCode(),
                test.getName(),
                request.externalLabPartyId(),
                request.externalLabName()
        );

        LabOrder saved = orderRepository.save(order);
        log.info("Created lab order {} for patient {} in tenant {}", saved.getId(), patient.getMrn(), appId);
        return toDto(saved);
    }

    public LabOrderDto collectSample(String orderId) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        if (order.getStatus() != LabOrder.Status.ORDERED) {
            throw new BusinessRuleException("Cannot collect sample for non-ordered test", "LAB_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        order.markCollected();
        return toDto(orderRepository.save(order));
    }

    public LabOrderDto sendOutOrder(String orderId, SendOutLabOrderRequest request) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        if (order.getStatus() != LabOrder.Status.ORDERED && order.getStatus() != LabOrder.Status.COLLECTED) {
            throw new BusinessRuleException("Cannot send out order in current status", "LAB_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        order.markSentOut(request.externalLabPartyId(), request.externalLabName());
        return toDto(orderRepository.save(order));
    }

    public LabOrderDto enterResult(String orderId, EnterLabResultRequest request) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        if (order.getStatus() == LabOrder.Status.VALIDATED || order.getStatus() == LabOrder.Status.CANCELLED) {
            throw new BusinessRuleException("Cannot enter result for validated or cancelled order", "LAB_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        LabOrder.ResultFlag flag = LabOrder.ResultFlag.NORMAL;
        if (request.resultFlag() != null && !request.resultFlag().trim().isEmpty()) {
            try {
                flag = LabOrder.ResultFlag.valueOf(request.resultFlag().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        order.enterResult(
                request.resultValueText(),
                flag,
                request.resultNotes(),
                request.attachmentId(),
                request.attachmentFilename()
        );

        if (flag == LabOrder.ResultFlag.CRITICAL) {
            log.warn("CRITICAL LAB VALUE recorded on order {} for patient {} - notifying doctor {}",
                    orderId, order.getPatientId(), order.getDoctorEmployeeId());
        }

        return toDto(orderRepository.save(order));
    }

    public LabOrderDto validateOrder(String orderId) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        if (order.getStatus() != LabOrder.Status.RESULTED) {
            throw new BusinessRuleException("Cannot validate order before entering test results", "LAB_VALIDATION_REQUIRES_RESULT", HttpStatus.CONFLICT);
        }

        order.validateResult();
        return toDto(orderRepository.save(order));
    }

    public LabOrderDto cancelOrder(String orderId) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        if (order.getStatus() != LabOrder.Status.ORDERED) {
            throw new BusinessRuleException("Only orders in ORDERED status can be cancelled", "LAB_CANNOT_CANCEL_NON_ORDERED", HttpStatus.CONFLICT);
        }

        order.cancel();
        return toDto(orderRepository.save(order));
    }

    public LabOrderDto acknowledgeCritical(String orderId) {
        String appId = TenantContext.require();
        LabOrder order = orderRepository.findByAppIdAndId(appId, orderId)
                .orElseThrow(() -> new NotFoundException("Lab order not found", "LAB_ORDER_NOT_FOUND"));

        order.acknowledgeCritical();
        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<LabOrderDto> getAllOrders(String statusStr) {
        String appId = TenantContext.require();
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                LabOrder.Status status = LabOrder.Status.valueOf(statusStr.trim().toUpperCase());
                return orderRepository.findAllByAppIdAndStatusOrderByOrderedAtDesc(appId, status)
                        .stream().map(this::toDto).toList();
            } catch (IllegalArgumentException ignored) {}
        }
        return orderRepository.findAllByAppIdOrderByOrderedAtDesc(appId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LabOrderDto> getOrdersByPatient(String patientId, boolean validatedOnly) {
        String appId = TenantContext.require();
        if (validatedOnly) {
            return orderRepository.findAllByAppIdAndPatientIdAndStatusOrderByOrderedAtDesc(appId, patientId, LabOrder.Status.VALIDATED)
                    .stream().map(this::toDto).toList();
        }
        return orderRepository.findAllByAppIdAndPatientIdOrderByOrderedAtDesc(appId, patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LabOrderDto> getAgingSentOutOrders() {
        String appId = TenantContext.require();
        long thresholdEpoch = Instant.now().minusSeconds(3 * 24 * 3600).toEpochMilli();
        return orderRepository.findAgingSentOutOrders(appId, thresholdEpoch)
                .stream().map(this::toDto).toList();
    }

    private LabTestItemDto toDto(LabTestItem i) {
        return new LabTestItemDto(
                i.getId(),
                i.getCode(),
                i.getCategory().name(),
                i.getName(),
                i.getSampleType(),
                i.getNormalRangeText(),
                i.getPrice()
        );
    }

    private LabOrderDto toDto(LabOrder o) {
        String appId = TenantContext.require();
        Patient p = patientRepository.findByAppIdAndId(appId, o.getPatientId()).orElse(null);
        String patientName = p != null ? p.getFullName() : "Unknown";
        String patientMrn = p != null ? p.getMrn() : "";
        String doctorName = employeeRepository.findById(o.getDoctorEmployeeId())
                .map(Employee::getFullName).orElse(o.getDoctorEmployeeId());

        return new LabOrderDto(
                o.getId(),
                o.getPatientId(),
                patientName,
                patientMrn,
                o.getVisitId(),
                o.getDoctorEmployeeId(),
                doctorName,
                o.getTestId(),
                o.getCategory().name(),
                o.getTestCode(),
                o.getTestName(),
                o.getStatus().name(),
                o.getOrderedAt(),
                o.getCollectedAt(),
                o.getSentOutAt(),
                o.getResultedAt(),
                o.getValidatedAt(),
                o.getResultValueText(),
                o.getResultFlag() != null ? o.getResultFlag().name() : null,
                o.getResultNotes(),
                o.getExternalLabPartyId(),
                o.getExternalLabName(),
                o.getAttachmentId(),
                o.getAttachmentFilename(),
                o.isCriticalAcknowledged(),
                o.getCriticalAcknowledgedAt()
        );
    }
}
