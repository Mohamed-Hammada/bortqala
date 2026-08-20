package com.bemo.hr.manufacturing.quality.api;

import com.bemo.hr.manufacturing.quality.application.QualityStockDispositionService;
import com.bemo.hr.manufacturing.quality.domain.QualityStockDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/dispositions")
public class QualityStockDispositionController {

    private final QualityStockDispositionService dispositionService;

    public QualityStockDispositionController(QualityStockDispositionService dispositionService) {
        this.dispositionService = dispositionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'QUALITY_MANAGER')")
    public QualityStockDisposition createDisposition(@RequestBody CreateDispositionPayload payload) {
        return dispositionService.createDisposition(payload.inspectionId(), payload.dispositionType(), payload.quantity(), payload.reason());
    }

    @GetMapping("/{inspectionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'QUALITY_MANAGER', 'VIEWER')")
    public List<QualityStockDisposition> getDispositionsForInspection(@PathVariable String inspectionId) {
        return dispositionService.getDispositionsForInspection(inspectionId);
    }

    public record CreateDispositionPayload(String inspectionId, String dispositionType, BigDecimal quantity,
                                           String reason) {
    }
}
