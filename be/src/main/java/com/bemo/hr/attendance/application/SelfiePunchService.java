package com.bemo.hr.attendance.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.attendance.api.SelfiePunchApi;
import com.bemo.hr.attendance.domain.AttendanceSelfiePunch;
import com.bemo.hr.attendance.infrastructure.AttendanceSelfiePunchRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

/**
 * WP-14 AC-3: selfie-punch intake for the Android shell (and any ESS client).
 * Idempotent on the client-generated operation id — offline-outbox retries can never
 * produce a second punch; replays return the original record flagged as duplicates.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SelfiePunchService {

    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final AttendanceSelfiePunchRepository punchRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public SelfiePunchService(AttendanceSelfiePunchRepository punchRepository,
                              EmployeeRepository employeeRepository,
                              AuditService auditService) {
        this.punchRepository = punchRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SelfiePunchApi.SelfiePunchResponse punch(String username, SelfiePunchApi.SelfiePunchRequest request) {
        log.debug("selfie punch called user={} operationId={}", username, request.operationId());
        var existing = punchRepository.findByOperationId(request.operationId().strip());
        if (existing.isPresent()) {
            AttendanceSelfiePunch replayed = existing.get();
            if (!replayed.belongsTo(resolveEmployeeId(username)))
                throw new BusinessRuleException("This operation id was already used by another employee.",
                        "ATT_SELFIE_OPERATION_TAKEN", HttpStatus.CONFLICT);
            log.info("selfie punch {} replayed exactly-once", replayed.getId());
            return toResponse(replayed, true);
        }

        String employeeId = resolveEmployeeId(username);
        String imageData = decodeAndValidateImage(request.imageBase64(), request.imageBytes());
        AttendanceSelfiePunch punch = new AttendanceSelfiePunch(employeeId, request.operationId().strip(),
                request.clientTimestamp(), request.imageContentType() == null ? "image/jpeg" : request.imageContentType(),
                imageData);
        AttendanceSelfiePunch saved = punchRepository.save(punch);
        auditService.record("CREATE", "ATT_SELFIE_PUNCH", saved.getId(), username,
                "{\"employeeId\":\"" + employeeId + "\",\"operationId\":\"" + saved.getOperationId()
                        + "\",\"punchedAt\":" + saved.getPunchedAt() + "}", null);
        log.info("selfie punch {} accepted for employee {}", saved.getId(), employeeId);
        return toResponse(saved, false);
    }

    private String resolveEmployeeId(String username) {
        Employee employee = employeeRepository.findByDeviceUserId(username == null ? "" : username.strip())
                .orElseThrow(() -> new BusinessRuleException(
                        "No employee record is linked to the signed-in user.", "ATT_SELFIE_EMPLOYEE_NOT_LINKED",
                        HttpStatus.CONFLICT));
        return employee.getId();
    }

    private String decodeAndValidateImage(String imageBase64, Integer declaredBytes) {
        if (imageBase64 == null || imageBase64.isBlank())
            throw new BusinessRuleException("A selfie image is required.", "ATT_SELFIE_IMAGE_REQUIRED", HttpStatus.BAD_REQUEST);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(imageBase64.strip());
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("The selfie image is not valid base64.", "ATT_SELFIE_IMAGE_INVALID",
                    HttpStatus.BAD_REQUEST);
        }
        if (decoded.length == 0 || decoded.length > MAX_IMAGE_BYTES)
            throw new BusinessRuleException("The selfie image must not exceed 2 MB.",
                    "ATT_SELFIE_IMAGE_TOO_LARGE", HttpStatus.BAD_REQUEST);
        if (declaredBytes != null && declaredBytes != decoded.length)
            throw new BusinessRuleException("Declared image size does not match the uploaded payload.",
                    "ATT_SELFIE_IMAGE_INVALID", HttpStatus.BAD_REQUEST);
        return imageBase64.strip();
    }

    private SelfiePunchApi.SelfiePunchResponse toResponse(AttendanceSelfiePunch punch, boolean duplicate) {
        return new SelfiePunchApi.SelfiePunchResponse(punch.getId(), punch.getEmployeeId(), punch.getOperationId(),
                punch.getPunchedAt(), duplicate);
    }
}
