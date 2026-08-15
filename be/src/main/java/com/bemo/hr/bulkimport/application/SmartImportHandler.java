package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.HandlerOutcome;
import com.bemo.hr.bulkimport.domain.SmartImportModels.PreviewRow;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Workflow;

import java.util.List;

public interface SmartImportHandler {
    boolean supports(String workflowKey);
    HandlerOutcome commit(Workflow workflow, List<PreviewRow> rows, boolean skipInvalid);
}
