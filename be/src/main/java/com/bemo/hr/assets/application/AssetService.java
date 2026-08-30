package com.bemo.hr.assets.application;

import com.bemo.hr.assets.api.AssetsApi;
import com.bemo.hr.assets.domain.FixedAsset;
import com.bemo.hr.assets.infrastructure.FixedAssetRepository;
import com.bemo.hr.audit.application.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AssetService {

    private final FixedAssetRepository assetRepository;
    private final AssetDepreciationService depreciationService;
    private final AuditService auditService;

    public AssetService(FixedAssetRepository assetRepository,
                        AssetDepreciationService depreciationService,
                        AuditService auditService) {
        this.assetRepository = assetRepository;
        this.depreciationService = depreciationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FixedAsset> list() {
        return assetRepository.findAllByOrderByAcquisitionDateDesc();
    }

    @Transactional(readOnly = true)
    public FixedAsset require(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new com.bemo.hr.shared.domain.BusinessRuleException(
                        "Fixed asset not found.", "ASSET_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public FixedAsset create(AssetsApi.FixedAssetPayload payload, String username) {
        log.debug("create fixed asset called by {}", username);
        FixedAsset asset = new FixedAsset(payload.name(), parseCategory(payload.category()),
                payload.acquisitionDate(), payload.acquisitionCost(),
                payload.salvageValue() == null ? java.math.BigDecimal.ZERO : payload.salvageValue(),
                payload.usefulLifeMonths(), payload.branchId(), payload.costCenterId());
        FixedAsset saved = assetRepository.save(asset);
        auditService.record("CREATE", "FIXED_ASSET", saved.getId(), username,
                "{\"name\":\"" + saved.getName() + "\",\"cost\":" + saved.getAcquisitionCost() + "}", null);
        log.info("Fixed asset {} created by {}", saved.getId(), username);
        return saved;
    }

    @Transactional
    public FixedAsset update(String id, AssetsApi.FixedAssetPayload payload, Long expectedVersion, String username) {
        FixedAsset asset = require(id);
        if (expectedVersion != null && asset.getVersion() != null && !expectedVersion.equals(asset.getVersion()))
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "The asset was changed by someone else. Reload and try again.",
                    "ASSET_VERSION_CONFLICT", HttpStatus.CONFLICT);
        if (asset.getStatus() == FixedAsset.Status.DISPOSED)
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Disposed assets cannot be edited.", "ASSET_DISPOSAL_INVALID", HttpStatus.CONFLICT);
        asset.update(payload.name(), parseCategory(payload.category()), payload.acquisitionDate(),
                payload.acquisitionCost(),
                payload.salvageValue() == null ? java.math.BigDecimal.ZERO : payload.salvageValue(),
                payload.usefulLifeMonths(), payload.branchId(), payload.costCenterId());
        FixedAsset saved = assetRepository.save(asset);
        auditService.record("UPDATE", "FIXED_ASSET", saved.getId(), username, "{\"name\":\"" + saved.getName() + "\"}", null);
        return saved;
    }

    /** Disposes the asset then books the balanced disposal journal. */
    @Transactional
    public AssetsApi.FixedAssetResponse dispose(String id, AssetsApi.DisposalRequest request, String username) {
        FixedAsset asset = require(id);
        java.math.BigDecimal proceeds = request.proceeds();
        if (proceeds.signum() < 0)
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Disposal proceeds cannot be negative.", "ASSET_DISPOSAL_INVALID", HttpStatus.BAD_REQUEST);
        long disposalEpochMilli = request.disposalDate() > 0 ? request.disposalDate()
                : java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();

        // Book the journal from the pre-disposal state, then flip the asset.
        AssetsApi.DisposalJournalSummary summary =
                depreciationService.disposeWithJournal(asset, disposalEpochMilli, proceeds, username);
        asset.dispose(disposalEpochMilli, proceeds);
        assetRepository.save(asset);
        return toResponse(asset, summary);
    }

    static FixedAsset.Category parseCategory(String category) {
        try {
            return FixedAsset.Category.valueOf(category.strip().toUpperCase());
        } catch (Exception exception) {
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Unknown asset category.", "ASSET_CATEGORY_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    public static AssetsApi.FixedAssetResponse toResponse(FixedAsset asset) {
        return toResponse(asset, null);
    }

    public static AssetsApi.FixedAssetResponse toResponse(FixedAsset asset,
                                                          AssetsApi.DisposalJournalSummary summary) {
        return new AssetsApi.FixedAssetResponse(
                asset.getId(), asset.getName(), asset.getCategory().name(), asset.getAcquisitionDate(),
                asset.getAcquisitionCost(), asset.getSalvageValue(), asset.getUsefulLifeMonths(),
                asset.monthlyCharge(), asset.getAccumulatedDepreciation(), asset.netBookValue(),
                asset.getLastPostedYearMonth(), asset.getStatus().name(),
                asset.getDisposalDate(), asset.getDisposalProceeds(),
                asset.getBranchId(), asset.getCostCenterId(), asset.getVersion());
    }
}
