package com.bemo.hr.operations;

import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
public class OperationsController {
    private final OperationsService operationsService;
    private final InventoryValuationService inventoryValuationService;
    private final AuthService authService;

    @GetMapping
    OperationsApi.Snapshot snapshot() {
        return operationsService.snapshot();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemView createItem(@Valid @RequestBody OperationsApi.ItemRequest request) {
        return operationsService.createItem(request);
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemView updateItem(@PathVariable String id, @Valid @RequestBody OperationsApi.ItemRequest request) {
        return operationsService.updateItem(id, request);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot transaction(@Valid @RequestBody OperationsApi.TransactionRequest request, Authentication authentication) {
        return operationsService.recordTransaction(request, authentication.getName());
    }

    @PostMapping("/advances")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot advance(@Valid @RequestBody OperationsApi.AdvanceRequest request, Authentication authentication) {
        return operationsService.recordAdvance(request, authentication.getName());
    }

    @GetMapping("/item-categories")
    List<OperationsApi.ItemCategoryView> listCategories() {
        return operationsService.listItemCategories();
    }

    @PostMapping("/item-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemCategoryView createCategory(@Valid @RequestBody OperationsApi.ItemCategoryRequest request) {
        return operationsService.createItemCategory(request);
    }

    @GetMapping("/uoms")
    List<OperationsApi.UnitOfMeasureView> listUoms() {
        return operationsService.listUnitOfMeasures();
    }

    @PostMapping("/uoms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.UnitOfMeasureView createUom(@Valid @RequestBody OperationsApi.UnitOfMeasureRequest request) {
        return operationsService.createUnitOfMeasure(request);
    }

    @GetMapping("/uom-conversions")
    List<OperationsApi.UnitConversionView> listUnitConversions() {
        return operationsService.listUnitConversions();
    }

    @PostMapping("/uom-conversions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.UnitConversionView createUnitConversion(@Valid @RequestBody OperationsApi.UnitConversionRequest request, Authentication authentication) {
        return operationsService.createUnitConversion(request, authentication.getName());
    }

    @GetMapping("/negative-balances")
    List<OperationsApi.NegativeBalanceView> negativeBalances() {
        return operationsService.getNegativeBalances();
    }

    @GetMapping("/reorder-alerts")
    List<OperationsApi.ReorderAlertView> reorderAlerts() {
        return operationsService.reorderAlerts();
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot adjustment(@Valid @RequestBody OperationsApi.AdjustmentRequest request, Authentication authentication) {
        return operationsService.createStockAdjustment(request, authentication.getName());
    }

    @GetMapping("/valuation/settings")
    OperationsApi.ValuationPolicyView valuationSettings() {
        return inventoryValuationService.policy();
    }

    @PutMapping("/valuation/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ValuationPolicyView updateValuationSettings(
            @Valid @RequestBody OperationsApi.ValuationPolicyRequest request, Authentication authentication) {
        return inventoryValuationService.updatePolicy(request, authentication.getName());
    }

    @GetMapping("/valuation/report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'INVENTORY_MANAGER', 'FINANCE_MANAGER', 'AUDITOR')")
    OperationsApi.ValuationReport valuationReport(@RequestParam(required = false) Long asOf,
                                                  @RequestParam(name = "warehouseId", required = false) String warehouseId,
                                                  @RequestParam(required = false) String itemId) {
        return inventoryValuationService.report(asOf, warehouseId, itemId);
    }

    @GetMapping("/valuation/movements/{movementId}")
    OperationsApi.MovementCostView movementCost(@PathVariable String movementId) {
        return inventoryValuationService.movementCost(movementId);
    }

    @PostMapping("/valuation/revaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.RevaluationView revalue(@Valid @RequestBody OperationsApi.RevaluationRequest request,
                                          Authentication authentication) {
        return inventoryValuationService.revalue(request, authentication.getName());
    }

    @GetMapping("/export.xlsx")
    ResponseEntity<byte[]> export(Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var body = operationsService.export(new ExcelExportOptions(preference.locale(), preference.excelTableStyle()));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String name = (preference.locale().startsWith("ar") ? "المخزون-والحسابات" : "inventory-and-ledgers") + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx";
        headers.setContentDisposition(ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
