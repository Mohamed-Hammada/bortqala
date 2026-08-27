package com.bemo.hr.reportbuilder.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class ReportBuilderApi {

    public record DatasetSummary(String code, String labelAr, String labelEn, int version) {}

    public record DatasetDescriptor(String code, String labelAr, String labelEn, int version,
                                     List<Map<String, Object>> fields, int maxLimit) {}

    public record QueryPayload(
            @NotBlank String datasetCode,
            List<String> dimensions,
            List<String> measures,
            List<Map<String, String>> filters,
            List<Map<String, String>> sort,
            int limit) {}

    public record QueryResult(List<Map<String, String>> columns, List<Map<String, Object>> rows,
                               int totalRows, int limit, boolean versionDrift) {}

    public record SaveReportPayload(
            @NotBlank String name,
            @NotBlank String datasetCode,
            @NotBlank String definition) {}

    public record SavedReportResponse(String id, String name, String datasetCode,
                                       int datasetVersion, String definition,
                                       String ownerUserId, Long createdAtEpochMs, Long version) {}
}
