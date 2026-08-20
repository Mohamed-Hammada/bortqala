package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.CostCenterService;
import com.bemo.hr.finance.application.CostCenterService.CostCenterPayload;
import com.bemo.hr.finance.application.CostCenterService.CostCenterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/cost-centers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE_MANAGER','ACCOUNTANT')")
public class CostCenterController {

    private final CostCenterService costCenterService;

    @GetMapping
    public List<CostCenterResponse> list(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return activeOnly ? costCenterService.listActive() : costCenterService.listAll();
    }

    @GetMapping("/{id}")
    public CostCenterResponse getById(@PathVariable String id) {
        return costCenterService.getById(id);
    }

    @PostMapping
    public CostCenterResponse create(@RequestBody CostCenterPayload payload) {
        return costCenterService.create(payload);
    }

    @PutMapping("/{id}")
    public CostCenterResponse update(@PathVariable String id, @RequestBody CostCenterPayload payload) {
        return costCenterService.update(id, payload);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        costCenterService.delete(id);
    }
}
