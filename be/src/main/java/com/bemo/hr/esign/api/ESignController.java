package com.bemo.hr.esign.api;

import com.bemo.hr.esign.application.ESignService;
import com.bemo.hr.esign.domain.PacketStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/signatures")
public class ESignController {

    private final ESignService service;

    public ESignController(ESignService service) {
        this.service = service;
    }

    @GetMapping("/packets")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<List<ESignApi.PacketResponse>> listPackets(
            @RequestParam(required = false) PacketStatus status) {
        return ResponseEntity.ok(service.listPackets(status));
    }

    @PostMapping("/packets")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ESignApi.PacketResponse> createPacket(
            @RequestBody @Valid ESignApi.CreatePacketRequest request) {
        return ResponseEntity.ok(service.createPacket(request));
    }

    @PostMapping("/packets/{id}/start-routing")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<Void> startRouting(@PathVariable String id) {
        service.startRouting(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/packets/{id}/steps/{stepOrder}/sign")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<ESignApi.StepResponse> signStep(
            @PathVariable String id, @PathVariable int stepOrder,
            @RequestBody @Valid ESignApi.SignRequest request) {
        return ResponseEntity.ok(service.signStep(id, stepOrder, request));
    }

    @PostMapping("/packets/{id}/steps/{stepOrder}/decline")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<Void> declineStep(
            @PathVariable String id, @PathVariable int stepOrder,
            @RequestBody @Valid ESignApi.DeclineRequest request) {
        service.declineStep(id, stepOrder, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/packets/{id}/manifest")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR_MANAGER','HR_REVIEWER')")
    public ResponseEntity<ESignApi.ManifestExport> exportManifest(@PathVariable String id) {
        return ResponseEntity.ok(service.exportManifest(id));
    }
}
