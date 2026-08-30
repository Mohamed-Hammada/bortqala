package com.bemo.hr.operations.application;

import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.StockMovement;
import com.bemo.hr.operations.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAnalyticsServiceTests {

    @Mock
    private InventoryItemRepository itemRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private InventoryAnalyticsService analyticsService;

    private InventoryItem cement;
    private InventoryItem rebar;
    private InventoryItem bricks;

    @BeforeEach
    void setUp() {
        cement = new InventoryItem("MAT-CEM", "Portland Cement 50kg", "RAW_MATERIAL", "BAG");
        rebar = new InventoryItem("MAT-REB", "Steel Rebar 12mm", "RAW_MATERIAL", "TON");
        bricks = new InventoryItem("MAT-BRK", "Clay Red Bricks", "RAW_MATERIAL", "UNIT");
    }

    @Test
    @DisplayName("Calculates stock aging buckets accurately based on last movement date")
    void testStockAgingSummary() {
        when(itemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cement, rebar, bricks));

        when(stockMovementRepository.balance(cement.getId())).thenReturn(BigDecimal.valueOf(100));
        when(stockMovementRepository.balance(rebar.getId())).thenReturn(BigDecimal.valueOf(50));
        when(stockMovementRepository.balance(bricks.getId())).thenReturn(BigDecimal.ZERO);

        Instant recentInstant = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant agedInstant = Instant.now().minus(45, ChronoUnit.DAYS);

        StockMovement cementMov = new StockMovement(cement.getId(), null, "IN", BigDecimal.valueOf(100), null, "REC-01", null, recentInstant, "admin");
        StockMovement rebarMov = new StockMovement(rebar.getId(), null, "IN", BigDecimal.valueOf(50), null, "REC-02", null, agedInstant, "admin");

        when(stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(cement.getId())).thenReturn(Optional.of(cementMov));
        when(stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(rebar.getId())).thenReturn(Optional.of(rebarMov));

        InventoryAnalyticsService.StockAgingSummary summary = analyticsService.getStockAgingSummary();

        assertThat(summary.getTotalOnHandItems()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(summary.getBucket0To30Qty()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(summary.getBucket31To60Qty()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(summary.getBucket61To90Qty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getBucket90PlusQty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Detects dead stock items based on days threshold or explicit flag")
    void testDeadStockItems() {
        bricks.configureTracking("1234567890", null, "NONE", null, true); // explicitly flagged

        when(itemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cement, bricks));
        when(stockMovementRepository.balance(cement.getId())).thenReturn(BigDecimal.valueOf(50));
        when(stockMovementRepository.balance(bricks.getId())).thenReturn(BigDecimal.valueOf(200));

        Instant oldInstant = Instant.now().minus(120, ChronoUnit.DAYS);
        StockMovement cementMov = new StockMovement(cement.getId(), null, "IN", BigDecimal.valueOf(50), null, "REC-01", null, oldInstant, "admin");

        when(stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(cement.getId())).thenReturn(Optional.of(cementMov));
        when(stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(bricks.getId())).thenReturn(Optional.empty());

        List<InventoryAnalyticsService.DeadStockItem> deadStock = analyticsService.getDeadStockItems(90);

        assertThat(deadStock).hasSize(2);
        assertThat(deadStock).extracting(InventoryAnalyticsService.DeadStockItem::getItemCode)
                .containsExactlyInAnyOrder("MAT-CEM", "MAT-BRK");
    }

    @Test
    @DisplayName("Generates reorder alerts with correct shortage and suggested quantities")
    void testReorderAlerts() {
        cement.configureReorder(BigDecimal.valueOf(100), BigDecimal.valueOf(50)); // reorder point 100, qty 50
        rebar.configureReorder(BigDecimal.valueOf(20), BigDecimal.valueOf(10));

        when(itemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cement, rebar));
        when(stockMovementRepository.balance(cement.getId())).thenReturn(BigDecimal.valueOf(20)); // below 50% -> WARNING
        when(stockMovementRepository.balance(rebar.getId())).thenReturn(BigDecimal.ZERO); // 0 -> CRITICAL

        List<InventoryAnalyticsService.ReorderAlertItem> alerts = analyticsService.getReorderAlerts();

        assertThat(alerts).hasSize(2);

        InventoryAnalyticsService.ReorderAlertItem cementAlert = alerts.stream().filter(a -> a.getItemCode().equals("MAT-CEM")).findFirst().orElseThrow();
        assertThat(cementAlert.getShortageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(cementAlert.getSuggestedOrderQuantity()).isEqualByComparingTo(BigDecimal.valueOf(130));
        assertThat(cementAlert.getUrgency()).isEqualTo("WARNING");

        InventoryAnalyticsService.ReorderAlertItem rebarAlert = alerts.stream().filter(a -> a.getItemCode().equals("MAT-REB")).findFirst().orElseThrow();
        assertThat(rebarAlert.getShortageQuantity()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(rebarAlert.getSuggestedOrderQuantity()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(rebarAlert.getUrgency()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Looks up item by barcode or barcode alias")
    void testBarcodeLookup() {
        cement.configureTracking("6221234567890", "CEM-50,CEMENT-BAG", "LOT", 180, false);
        when(itemRepository.findByBarcodeOrAlias("6221234567890")).thenReturn(List.of(cement));
        when(stockMovementRepository.balance(cement.getId())).thenReturn(BigDecimal.valueOf(300));

        Optional<InventoryAnalyticsService.BarcodeLookupResult> result = analyticsService.lookupBarcode("6221234567890");

        assertThat(result).isPresent();
        assertThat(result.get().getItemCode()).isEqualTo("MAT-CEM");
        assertThat(result.get().getTrackingType()).isEqualTo("LOT");
        assertThat(result.get().getShelfLifeDays()).isEqualTo(180);
        assertThat(result.get().getOnHandQuantity()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }
}
