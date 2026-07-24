package com.bemo.hr.reporting.application;

import com.bemo.hr.reporting.api.ReportingApi;

public interface ReportExporter {
    byte[] export(ReportingApi.Details report, ExcelExportOptions options);
}
