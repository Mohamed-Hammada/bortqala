package com.bemo.hr.manufacturing.quality.api;

import com.bemo.hr.manufacturing.quality.application.QualityPlanService;
import com.bemo.hr.manufacturing.quality.domain.QualityDisposition;
import com.bemo.hr.manufacturing.quality.domain.QualityPlanHeader;
import com.bemo.hr.shared.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing/quality")
public class QualityPlanController {

    private final QualityPlanService qualityPlanService;

    public QualityPlanController(QualityPlanService qualityPlanService) {
        this.qualityPlanService = qualityPlanService;
    }

    @PostMapping("/plans")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER + " or " + Roles.QUALITY_MANAGER)
    public QualityPlanHeader createPlan(@RequestBody CreatePlanPayload payload) {
        return qualityPlanService.createPlan(payload.planCode(), payload.name(), payload.itemId(), QualityPlanHeader.TargetCategory.valueOf(payload.targetCategory()));
    }

    @PostMapping("/dispositions")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER + " or " + Roles.QUALITY_MANAGER)
    public QualityDisposition recordDisposition(@RequestBody RecordDispositionPayload payload) {
        return qualityPlanService.recordDisposition(payload.dispositionNumber(), payload.planId(), payload.inspectionId(), QualityDisposition.Result.valueOf(payload.result()), payload.notes());
    }

    @GetMapping("/plans/items/{itemId}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.MANUFACTURING_MANAGER + " or " + Roles.QUALITY_MANAGER + " or " + Roles.VIEWER)
    public List<QualityPlanHeader> getPlansByItem(@PathVariable String itemId) {
        return qualityPlanService.getPlansByItem(itemId);
    }

    public record CreatePlanPayload(String planCode, String name, String itemId, String targetCategory) {
    }

    public record RecordDispositionPayload(String dispositionNumber, String planId, String inspectionId, String result,
                                           String notes) {
    }
}
