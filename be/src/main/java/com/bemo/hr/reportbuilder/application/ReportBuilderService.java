package com.bemo.hr.reportbuilder.application;

import com.bemo.hr.reportbuilder.domain.ReportDataset;
import com.bemo.hr.reportbuilder.domain.SavedReport;
import com.bemo.hr.reportbuilder.domain.SavedReportRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportBuilderService {

    private final SavedReportRepository savedReportRepo;

    public List<Map<String, String>> listDatasets() {
        return ReportDataset.REGISTRY.values().stream()
            .map(ds -> Map.of("code", ds.code(), "labelAr", ds.labelAr(),
                    "labelEn", ds.labelEn(), "version", String.valueOf(ds.version())))
            .toList();
    }

    public Map<String, Object> getDatasetDescriptor(String code) {
        ReportDataset ds = ReportDataset.resolve(code);
        return Map.of("code", ds.code(), "labelAr", ds.labelAr(), "labelEn", ds.labelEn(),
                "version", ds.version(), "fields", ds.fields(), "maxLimit", ds.maxLimit());
    }

    public Map<String, Object> executeQuery(String datasetCode, List<String> dimensions,
                                            List<String> measures, List<Map<String, String>> filters,
                                            List<Map<String, String>> sort, int limit) {
        ReportDataset ds = ReportDataset.resolve(datasetCode);

        validateFields(ds, dimensions, measures);

        int effectiveLimit = Math.min(limit > 0 ? limit : 1000, ds.maxLimit());
        List<Map<String, Object>> rows = generateSampleData(ds, dimensions, measures, effectiveLimit);

        List<Map<String, String>> columns = new ArrayList<>();
        for (String d : dimensions) {
            ReportDataset.FieldDef f = ds.fields().stream()
                    .filter(x -> x.name().equals(d)).findFirst().orElseThrow();
            columns.add(Map.of("name", d, "label", f.labelEn(), "labelAr", f.labelAr(),
                    "type", "STRING", "role", "dimension"));
        }
        for (String m : measures) {
            ReportDataset.FieldDef f = ds.fields().stream()
                    .filter(x -> x.name().equals(m)).findFirst().orElseThrow();
            columns.add(Map.of("name", m, "label", f.labelEn(), "labelAr", f.labelAr(),
                    "type", f.type(), "role", "measure", "aggregate", f.measureAggregate() != null ? f.measureAggregate() : ""));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("totalRows", rows.size());
        result.put("limit", effectiveLimit);
        result.put("versionDrift", false);
        return result;
    }

    private void validateFields(ReportDataset ds, List<String> dimensions, List<String> measures) {
        List<String> allowedNames = ds.fields().stream().map(ReportDataset.FieldDef::name).toList();
        List<String> unknown = new ArrayList<>();
        for (String d : dimensions) {
            ReportDataset.FieldDef f = ds.fields().stream().filter(x -> x.name().equals(d)).findFirst().orElse(null);
            if (f == null) unknown.add(d);
            else if (!f.dimension()) unknown.add(d + " (not a dimension)");
        }
        for (String m : measures) {
            ReportDataset.FieldDef f = ds.fields().stream().filter(x -> x.name().equals(m)).findFirst().orElse(null);
            if (f == null) unknown.add(m);
            else if (f.dimension()) unknown.add(m + " (not a measure)");
        }
        if (!unknown.isEmpty()) {
            throw new BusinessRuleException("Unknown/disallowed fields: " + String.join(", ", unknown)
                    + ". Allowed: " + String.join(", ", allowedNames),
                    "RB_FIELD_UNKNOWN", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Map<String, Object>> generateSampleData(ReportDataset ds, List<String> dimensions,
                                                         List<String> measures, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 20); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String d : dimensions) {
                row.put(d, "Dim-" + d + "-" + (i + 1));
            }
            for (String m : measures) {
                row.put(m, Math.round(Math.random() * 10000.0) / 100.0);
            }
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public SavedReport saveReport(String appId, String name, String datasetCode,
                                  String definition, String ownerUserId) {
        ReportDataset ds = ReportDataset.resolve(datasetCode);
        SavedReport report = new SavedReport(appId, name, datasetCode, ds.version(), definition, ownerUserId);
        return savedReportRepo.save(report);
    }

    @Transactional(readOnly = true)
    public List<SavedReport> listSavedReports(String appId) {
        return savedReportRepo.findByAppIdOrderByCreatedAtDesc(appId);
    }

    @Transactional(readOnly = true)
    public SavedReport getSavedReport(String appId, String reportId) {
        return savedReportRepo.findById(reportId)
                .filter(r -> r.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Saved report not found.",
                        "RB_REPORT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void deleteSavedReport(String appId, String reportId) {
        SavedReport r = getSavedReport(appId, reportId);
        savedReportRepo.delete(r);
    }
}
