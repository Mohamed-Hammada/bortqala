package com.bemo.hr.assets.domain;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixedAssetTests {

    private FixedAsset asset(String name, long acquisitionEpoch, String cost, String salvage, int lifeMonths) {
        return new FixedAsset(name, FixedAsset.Category.VEHICLE, acquisitionEpoch,
                new BigDecimal(cost), new BigDecimal(salvage), lifeMonths, null, null);
    }

    private static long epoch(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @Test
    void straightLineChargeIsRoundedHalfUpPerMonth() {
        FixedAsset asset = asset("Lathe", epoch(2026, 1, 15), "10000.00", "1000.00", 3);
        assertThat(asset.monthlyCharge()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void firstChargeMonthIsTheMonthAfterAcquisition() {
        FixedAsset asset = asset("Van", epoch(2026, 1, 31), "12000.00", "0.00", 12);
        assertThat(asset.firstChargeMonth()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(asset.chargeFor(YearMonth.of(2026, 1))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.chargeFor(YearMonth.of(2026, 2))).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(asset.chargeFor(YearMonth.of(2027, 3))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void finalMonthPostsTheExactRemainderSoTheLifeNeverDrifts() {
        FixedAsset asset = asset("Press", epoch(2026, 1, 10), "1000.00", "0.00", 3);
        YearMonth first = asset.firstChargeMonth();
        asset.registerPostedCharge(first, asset.chargeFor(first));
        asset.registerPostedCharge(first.plusMonths(1), asset.chargeFor(first.plusMonths(1)));
        BigDecimal remainder = asset.chargeFor(first.plusMonths(2));
        assertThat(remainder).isEqualByComparingTo(new BigDecimal("333.34"));
        asset.registerPostedCharge(first.plusMonths(2), remainder);
        assertThat(asset.getStatus()).isEqualTo(FixedAsset.Status.FULLY_DEPRECIATED);
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(asset.netBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void fullyDepreciatedAssetsCarryNoFurtherCharge() {
        FixedAsset asset = asset("Old van", epoch(2025, 1, 1), "600.00", "0.00", 6);
        asset.registerPostedCharge(asset.finalChargeMonth(), asset.chargeFor(asset.finalChargeMonth()));
        assertThat(asset.chargeFor(asset.finalChargeMonth().plusMonths(1))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void salvageMustBeBelowAcquisitionCostAndLifeWithinBounds() {
        assertThatThrownBy(() -> asset("X", epoch(2026, 1, 1), "1000.00", "1000.00", 12))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_SALVAGE_INVALID");
        assertThatThrownBy(() -> asset("X", epoch(2026, 1, 1), "1000.00", "0.00", 481))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_LIFE_INVALID");
        assertThatThrownBy(() -> new FixedAsset("X", FixedAsset.Category.OTHER,
                epoch(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ZERO, 12, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_COST_INVALID");
    }

    @Test
    void disposalIsAllowedExactlyOnce() {
        FixedAsset asset = asset("Van", epoch(2026, 1, 1), "12000.00", "0.00", 12);
        asset.dispose(epoch(2026, 6, 30), new BigDecimal("5000.00"));
        assertThat(asset.getStatus()).isEqualTo(FixedAsset.Status.DISPOSED);
        assertThat(asset.disposalGainOrLoss()).isEqualByComparingTo(new BigDecimal("-7000.00"));
        assertThatThrownBy(() -> asset.dispose(epoch(2026, 7, 1), BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "ASSET_DISPOSAL_INVALID");
    }

    @Test
    void disposedAssetsCarryNoChargeEvenInsideTheirLife() {
        FixedAsset asset = asset("Van", epoch(2026, 1, 1), "12000.00", "0.00", 12);
        asset.dispose(epoch(2026, 3, 15), new BigDecimal("8000.00"));
        assertThat(asset.chargeFor(YearMonth.of(2026, 4))).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
