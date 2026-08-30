package com.bemo.hr.trade.procurement.api;

import com.bemo.hr.trade.procurement.application.OcrCaptureService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/procurement/ocr-capture")
@PreAuthorize("@auth.hasPermission('procurement.read')")
public class OcrCaptureController {

    private final OcrCaptureService ocrCaptureService;

    public OcrCaptureController(OcrCaptureService ocrCaptureService) {
        this.ocrCaptureService = ocrCaptureService;
    }

    @GetMapping("/status")
    public OcrCaptureApi.OcrProviderStatus providerStatus() {
        return ocrCaptureService.providerStatus();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public OcrCaptureApi.OcrJobResponse upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return ocrCaptureService.upload(file, authentication.getName());
    }

    @GetMapping
    public List<OcrCaptureApi.OcrJobResponse> listJobs() {
        return ocrCaptureService.listJobs();
    }

    @GetMapping("/{id}")
    public OcrCaptureApi.OcrJobResponse getJob(@PathVariable String id) {
        return ocrCaptureService.getJob(id);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public Map<String, Object> convertToGrn(
            @PathVariable String id,
            @Valid @RequestBody OcrCaptureApi.ConvertOcrPayload payload) {
        return ocrCaptureService.convertToGrn(id, payload);
    }
}
