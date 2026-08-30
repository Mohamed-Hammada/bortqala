package com.bemo.hr.assets.api;

import com.bemo.hr.assets.application.AssetDepreciationService;
import com.bemo.hr.assets.application.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** WP-04: fixed-asset register, month-end depreciation runs and disposals. */
@RestController
@RequestMapping("/api/v1/fixed-assets")
public class AssetsController {

    private final AssetService assetService;
    private final AssetDepreciationService depreciationService;

    public AssetsController(AssetService assetService,
                            AssetDepreciationService depreciationService) {
        this.assetService = assetService;
        this.depreciationService = depreciationService;
    }

    @GetMapping
    @PreAuthorize("@auth.hasPermission('asset.read')")
    public List<AssetsApi.FixedAssetResponse> list() {
        return assetService.list().stream().map(AssetService::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@auth.hasPermission('asset.manage')")
    public AssetsApi.FixedAssetResponse create(@Valid @RequestBody AssetsApi.FixedAssetPayload payload,
                                               Authentication authentication) {
        return AssetService.toResponse(assetService.create(payload, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@auth.hasPermission('asset.manage')")
    public AssetsApi.FixedAssetResponse update(@PathVariable String id,
                                               @Valid @RequestBody AssetsApi.FixedAssetPayload payload,
                                               @RequestParam(required = false) Long expectedVersion,
                                               Authentication authentication) {
        return AssetService.toResponse(assetService.update(id, payload, expectedVersion, authentication.getName()));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("@auth.hasPermission('asset.manage')")
    public AssetsApi.FixedAssetResponse dispose(@PathVariable String id,
                                                @Valid @RequestBody AssetsApi.DisposalRequest request,
                                                Authentication authentication) {
        return assetService.dispose(id, request, authentication.getName());
    }

    /** Manual month-end run (admin). The monthly scheduler calls the same service. */
    @PostMapping("/run-depreciation")
    @PreAuthorize("@auth.hasPermission('asset.manage')")
    public AssetsApi.DepreciationRunResponse runDepreciation(@RequestParam String yearMonth,
                                                             Authentication authentication) {
        if (!depreciationService.tryLock()) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "A depreciation run is already in progress.", "ASSET_RUN_IN_PROGRESS", HttpStatus.CONFLICT);
        }
        try {
            return depreciationService.runDepreciation(yearMonth, authentication.getName());
        } finally {
            depreciationService.unlock();
        }
    }
}
