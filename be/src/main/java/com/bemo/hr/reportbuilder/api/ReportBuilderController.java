package com.bemo.hr.reportbuilder.api;

import com.bemo.hr.reportbuilder.application.ReportBuilderService;
import com.bemo.hr.reportbuilder.domain.SavedReport;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report-builder")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReportBuilderController {

    private final ReportBuilderService service;

    private String resolveAppId(Authentication auth) {
        if (auth.getDetails() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)
            return jwt.getClaimAsString("appId");
        return TenantContext.require();
    }

    @GetMapping("/datasets")
    public List<ReportBuilderApi.DatasetSummary> listDatasets() {
        return service.listDatasets().stream()
            .map(d -> new ReportBuilderApi.DatasetSummary(d.get("code"), d.get("labelAr"),
                    d.get("labelEn"), Integer.parseInt(d.get("version"))))
            .toList();
    }

    @GetMapping("/datasets/{code}")
    public Map<String, Object> getDatasetDescriptor(@PathVariable String code) {
        return service.getDatasetDescriptor(code);
    }

    @PostMapping("/query")
    @SuppressWarnings("unchecked")
    public ReportBuilderApi.QueryResult executeQuery(@Valid @RequestBody ReportBuilderApi.QueryPayload p) {
        Map<String, Object> result = service.executeQuery(
                p.datasetCode(), p.dimensions(), p.measures(),
                p.filters(), p.sort(), p.limit());
        return new ReportBuilderApi.QueryResult(
                (List<Map<String, String>>) result.get("columns"),
                (List<Map<String, Object>>) result.get("rows"),
                (int) result.get("totalRows"), (int) result.get("limit"),
                (boolean) result.get("versionDrift"));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportBuilderApi.SavedReportResponse saveReport(
            @Valid @RequestBody ReportBuilderApi.SaveReportPayload p, Authentication auth) {
        SavedReport r = service.saveReport(resolveAppId(auth), p.name(), p.datasetCode(),
                p.definition(), auth.getName());
        return toResp(r);
    }

    @GetMapping("/reports")
    public List<ReportBuilderApi.SavedReportResponse> listSavedReports(Authentication auth) {
        return service.listSavedReports(resolveAppId(auth)).stream().map(this::toResp).toList();
    }

    @GetMapping("/reports/{id}")
    public ReportBuilderApi.SavedReportResponse getSavedReport(@PathVariable String id, Authentication auth) {
        return toResp(service.getSavedReport(resolveAppId(auth), id));
    }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavedReport(@PathVariable String id, Authentication auth) {
        service.deleteSavedReport(resolveAppId(auth), id);
    }

    private ReportBuilderApi.SavedReportResponse toResp(SavedReport r) {
        return new ReportBuilderApi.SavedReportResponse(r.getId(), r.getName(), r.getDatasetCode(),
                r.getDatasetVersion(), r.getDefinition(), r.getOwnerUserId(),
                r.getCreatedAt(), r.getVersion());
    }
}
