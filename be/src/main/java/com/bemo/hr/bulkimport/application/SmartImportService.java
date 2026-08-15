package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.CellError;
import com.bemo.hr.bulkimport.domain.SmartImportModels.CommitRequest;
import com.bemo.hr.bulkimport.domain.SmartImportModels.CommitResult;
import com.bemo.hr.bulkimport.domain.SmartImportModels.EditedRow;
import com.bemo.hr.bulkimport.domain.SmartImportModels.HandlerOutcome;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Preview;
import com.bemo.hr.bulkimport.domain.SmartImportModels.PreviewRow;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmartImportService {
    private final SmartImportCatalog catalog;
    private final SmartImportWorkbookService workbook;
    private final SmartImportValidator validator;
    private final List<SmartImportHandler> handlers;
    private final Map<UUID, Preview> previews = new ConcurrentHashMap<>();
    private final Map<UUID, RejectedBatch> rejectedBatches = new ConcurrentHashMap<>();

    public SmartImportService(SmartImportCatalog catalog, SmartImportWorkbookService workbook,
                              SmartImportValidator validator, List<SmartImportHandler> handlers) {
        this.catalog = catalog;
        this.workbook = workbook;
        this.validator = validator;
        this.handlers = List.copyOf(handlers);
    }

    public List<Workflow> workflows() { return catalog.list(); }
    public Workflow workflow(String key) { return catalog.require(key); }
    public byte[] template(String key, boolean sample) { return workbook.buildTemplate(catalog.require(key), sample); }

    public Preview preview(String key, MultipartFile file) {
        var workflow = catalog.require(key);
        var rawRows = workbook.parse(workflow, file);
        var validated = validator.validate(workflow, rawRows);
        var previewId = UUID.randomUUID();
        long valid = validated.rows().stream().filter(row -> row.errors().isEmpty()).count();
        var preview = new Preview(previewId, workflow,
                file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename(),
                validated.rows().size(), (int) valid, validated.rows().size() - (int) valid,
                validated.rows(), validated.errors());
        previews.put(previewId, preview);
        return preview;
    }

    public CommitResult commit(String key, CommitRequest request) {
        var workflow = catalog.require(key);
        var original = previews.get(request.previewId());
        if (original == null || !original.workflow().key().equals(workflow.key())) {
            throw new IllegalArgumentException("Preview expired or does not belong to this workflow. Please preview the file again.");
        }
        var candidateRows = request.rows().isEmpty() ? original.rows() : editedRows(request.rows());
        var validated = validator.validate(workflow, candidateRows);
        var invalidRows = validated.rows().stream().filter(row -> !row.errors().isEmpty()).toList();
        if (!request.skipInvalid() && !invalidRows.isEmpty()) {
            throw new IllegalArgumentException("Strict mode blocked commit because validation errors remain.");
        }
        var commitRows = request.skipInvalid()
                ? validated.rows().stream().filter(row -> row.errors().isEmpty()).toList()
                : validated.rows();

        HandlerOutcome outcome = handlers.stream().filter(handler -> handler.supports(workflow.key())).findFirst()
                .map(handler -> handler.commit(workflow, commitRows, request.skipInvalid()))
                .orElseGet(() -> new HandlerOutcome(false, 0, 0,
                        "Validation and annotated error export are implemented for this workflow, but the current branch has no safe domain commit adapter for it yet. No business data was written.",
                        "تم تنفيذ التحقق وتصدير الأخطاء المعلّق لهذا المسار، لكن الفرع الحالي لا يحتوي بعد على محول حفظ آمن إلى نموذج المجال. لم تتم كتابة بيانات أعمال.",
                        List.of()));

        var allErrors = new ArrayList<CellError>(validated.errors());
        allErrors.addAll(outcome.errors());
        var batchId = UUID.randomUUID();
        var rowsWithRuntimeErrors = attachRuntimeErrors(validated.rows(), outcome.errors());
        rejectedBatches.put(batchId, new RejectedBatch(workflow, rowsWithRuntimeErrors, allErrors));
        int rejected = (int) rowsWithRuntimeErrors.stream().filter(row -> !row.errors().isEmpty()).count();
        String status = outcome.persisted()
                ? (rejected > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED")
                : "VALIDATED_ONLY";
        return new CommitResult(batchId, workflow.key(), status, outcome.persisted(), outcome.committedRows(), rejected,
                outcome.messageEn(), outcome.messageAr(), allErrors);
    }

    public byte[] rejectedWorkbook(UUID batchId) {
        var batch = rejectedBatches.get(batchId);
        if (batch == null) throw new IllegalArgumentException("Rejected-row export is no longer available for this batch.");
        return workbook.rejectedWorkbook(batch.workflow(), batch.rows(), batch.errors());
    }

    private List<PreviewRow> editedRows(List<EditedRow> edited) {
        return edited.stream().map(row -> new PreviewRow(row.rowNumber(), row.sheet(), row.values(), List.of())).toList();
    }

    private List<PreviewRow> attachRuntimeErrors(List<PreviewRow> rows, List<CellError> runtimeErrors) {
        if (runtimeErrors.isEmpty()) return rows;
        return rows.stream().map(row -> {
            var errors = new ArrayList<>(row.errors());
            runtimeErrors.stream().filter(error -> error.rowNumber() == row.rowNumber() && error.sheet().equals(row.sheet())).forEach(errors::add);
            return new PreviewRow(row.rowNumber(), row.sheet(), row.values(), errors);
        }).toList();
    }

    private record RejectedBatch(Workflow workflow, List<PreviewRow> rows, List<CellError> errors) {}
}
