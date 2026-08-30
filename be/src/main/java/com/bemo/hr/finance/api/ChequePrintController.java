package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.ChequePrintService;
import com.bemo.hr.finance.domain.treasury.ChequeLayout;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/treasury/cheques")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE_MANAGER','ACCOUNTANT','TREASURY_USER')")
public class ChequePrintController {

    private final ChequePrintService chequePrintService;

    public ChequePrintController(ChequePrintService chequePrintService) {
        this.chequePrintService = chequePrintService;
    }

    @GetMapping("/{id}/print")
    public String printCheque(@PathVariable String id) {
        return chequePrintService.renderPrintView(id);
    }

    @GetMapping("/{id}/print-data")
    public ChequePrintService.ChequePrintData getPrintData(@PathVariable String id) {
        return chequePrintService.getPrintData(id);
    }

    @GetMapping("/layouts")
    public List<ChequeLayout> listLayouts() {
        return chequePrintService.listLayouts();
    }
}
