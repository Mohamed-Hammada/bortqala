package com.bemo.hr.esign.api;

import com.bemo.hr.esign.domain.PacketStatus;
import com.bemo.hr.esign.domain.SignatureMethod;
import com.bemo.hr.esign.domain.StepStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class ESignApi {

    private ESignApi() {
    }

    public record PacketResponse(
            String id,
            String title,
            String documentName,
            String contentHash,
            PacketStatus status,
            String manifestJson,
            List<StepResponse> steps,
            long createdAt,
            long updatedAt
    ) {
    }

    public record CreatePacketRequest(
            @NotBlank String title,
            String documentName,
            @NotBlank String contentHash,
            List<CreateStepRequest> steps
    ) {
    }

    public record CreateStepRequest(
            @NotNull Integer stepOrder,
            @NotBlank String signerName,
            String signerUserId,
            String roleLabel
    ) {
    }

    public record StepResponse(
            String id,
            int stepOrder,
            String signerName,
            String signerUserId,
            String roleLabel,
            StepStatus status,
            Long signedAt,
            String ipAddress,
            String contentSha256,
            SignatureMethod method,
            String declineReason
    ) {
    }

    public record SignRequest(
            @NotBlank String contentSha256,
            @NotNull SignatureMethod method,
            String ipAddress
    ) {
    }

    public record DeclineRequest(
            @NotBlank String reason
    ) {
    }

    public record ManifestExport(
            String packetId,
            String title,
            String documentName,
            String contentHash,
            PacketStatus status,
            List<StepResponse> steps,
            long exportedAt
    ) {
    }
}
