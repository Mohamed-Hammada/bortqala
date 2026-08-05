package com.bemo.hr.operations;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.*;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.reporting.application.ExcelExportOptions;
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
    private final AuthService authService;

    @GetMapping OperationsApi.Snapshot snapshot() { return operationsService.snapshot(); }
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemView createItem(@Valid @RequestBody OperationsApi.ItemRequest request) { return operationsService.createItem(request); }
    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemView updateItem(@PathVariable String id, @Valid @RequestBody OperationsApi.ItemRequest request) {
        return operationsService.updateItem(id, request);
    }
    @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot transaction(@Valid @RequestBody OperationsApi.TransactionRequest request, Authentication authentication) {
        return operationsService.recordTransaction(request, authentication.getName());
    }
    @PostMapping("/advances") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot advance(@Valid @RequestBody OperationsApi.AdvanceRequest request, Authentication authentication) {
        return operationsService.recordAdvance(request, authentication.getName());
    }

    @GetMapping("/item-categories")
    List<OperationsApi.ItemCategoryView> listCategories() { return operationsService.listItemCategories(); }

    @PostMapping("/item-categories") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.ItemCategoryView createCategory(@Valid @RequestBody OperationsApi.ItemCategoryRequest request) {
        return operationsService.createItemCategory(request);
    }

    @GetMapping("/uoms")
    List<OperationsApi.UnitOfMeasureView> listUoms() { return operationsService.listUnitOfMeasures(); }

    @PostMapping("/uoms") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.UnitOfMeasureView createUom(@Valid @RequestBody OperationsApi.UnitOfMeasureRequest request) {
        return operationsService.createUnitOfMeasure(request);
    }

    @GetMapping("/uom-conversions")
    List<OperationsApi.UnitConversionView> listUnitConversions() { return operationsService.listUnitConversions(); }

    @PostMapping("/uom-conversions") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.UnitConversionView createUnitConversion(@Valid @RequestBody OperationsApi.UnitConversionRequest request, Authentication authentication) {
        return operationsService.createUnitConversion(request, authentication.getName());
    }

    @GetMapping("/negative-balances")
    List<OperationsApi.NegativeBalanceView> negativeBalances() { return operationsService.getNegativeBalances(); }

    @PostMapping("/adjustments") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    OperationsApi.Snapshot adjustment(@Valid @RequestBody OperationsApi.AdjustmentRequest request, Authentication authentication) {
        return operationsService.createStockAdjustment(request, authentication.getName());
    }

    @GetMapping("/export.xlsx")
    ResponseEntity<byte[]> export(Authentication authentication) {
        var preference=authService.currentPreferences(authentication.getName());
        var body=operationsService.export(new ExcelExportOptions(preference.locale(),preference.excelTableStyle()));
        var headers=new HttpHeaders();headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String name=(preference.locale().startsWith("ar")?"المخزون-والحسابات":"inventory-and-ledgers")+"-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))+".xlsx";
        headers.setContentDisposition(ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
