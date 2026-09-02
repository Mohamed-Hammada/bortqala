package com.bemo.hr.platform.application;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class BulkUpdateService {

    private static final Set<String> ALLOWED_EMPLOYEE_FIELDS = Set.of("employmentType", "active");
    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of("employee");

    public PlatformApi.BulkUpdateResponse execute(String appId, PlatformApi.BulkUpdateRequest request, String actor) {
        if (request.ids() == null || request.ids().isEmpty()) {
            throw new BusinessRuleException("No IDs provided", "BULK_UPDATE_EMPTY_IDS", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_ENTITY_TYPES.contains(request.entityType())) {
            throw new BusinessRuleException("Unsupported entity type: " + request.entityType(),
                    "BULK_UPDATE_INVALID_ENTITY", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_EMPLOYEE_FIELDS.contains(request.field())) {
            throw new BusinessRuleException("Unsupported field: " + request.field(),
                    "BULK_UPDATE_INVALID_FIELD", HttpStatus.BAD_REQUEST);
        }

        List<PlatformApi.BulkUpdateResultItem> results = new ArrayList<>();

        for (String id : request.ids()) {
            try {
                updateEmployeeField(appId, id, request.field(), request.value());
                results.add(new PlatformApi.BulkUpdateResultItem(id, true, null));
            } catch (BusinessRuleException e) {
                results.add(new PlatformApi.BulkUpdateResultItem(id, false, e.getMessage()));
            } catch (Exception e) {
                log.error("Unexpected error updating {} field={}: {}", id, request.field(), e.getMessage(), e);
                results.add(new PlatformApi.BulkUpdateResultItem(id, false, "Unexpected error"));
            }
        }

        return new PlatformApi.BulkUpdateResponse(results);
    }

    private void updateEmployeeField(String appId, String employeeId, String field, String value) {
        // In a real implementation this would call EmployeeService/HrConfigurationService
        // For now, we validate the value format and return success
        switch (field) {
            case "employmentType" -> {
                if (!Set.of("FIXED", "DAILY").contains(value)) {
                    throw new BusinessRuleException("Invalid employment type: " + value,
                            "BULK_UPDATE_INVALID_VALUE", HttpStatus.BAD_REQUEST);
                }
            }
            case "active" -> {
                if (!Set.of("true", "false").contains(value)) {
                    throw new BusinessRuleException("Invalid active value: " + value,
                            "BULK_UPDATE_INVALID_VALUE", HttpStatus.BAD_REQUEST);
                }
            }
            default -> throw new BusinessRuleException("Unsupported field: " + field,
                    "BULK_UPDATE_INVALID_FIELD", HttpStatus.BAD_REQUEST);
        }
    }
}
