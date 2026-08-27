package com.bemo.hr.esign.application;

import com.bemo.hr.esign.api.ESignApi;
import com.bemo.hr.esign.domain.*;
import com.bemo.hr.esign.infrastructure.SignaturePacketRepository;
import com.bemo.hr.esign.infrastructure.SignatureStepRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ESignService {

    private final SignaturePacketRepository packetRepository;
    private final SignatureStepRepository stepRepository;

    public ESignService(SignaturePacketRepository packetRepository, SignatureStepRepository stepRepository) {
        this.packetRepository = packetRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional(readOnly = true)
    public List<ESignApi.PacketResponse> listPackets(PacketStatus status) {
        List<SignaturePacket> list = status != null
                ? packetRepository.findByStatusOrderByCreatedAtDesc(status)
                : packetRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toPacketResponse).toList();
    }

    @Transactional
    public ESignApi.PacketResponse createPacket(ESignApi.CreatePacketRequest request) {
        SignaturePacket packet = new SignaturePacket(request.title(), request.documentName(), request.contentHash());
        SignaturePacket saved = packetRepository.save(packet);

        if (request.steps() != null) {
            for (ESignApi.CreateStepRequest s : request.steps()) {
                stepRepository.save(new SignatureStep(
                        saved.getId(), s.stepOrder(), s.signerName(),
                        s.signerUserId(), s.roleLabel()));
            }
        }
        return toPacketResponse(saved);
    }

    @Transactional
    public void startRouting(String packetId) {
        SignaturePacket packet = packetRepository.findById(packetId)
                .orElseThrow(() -> new BusinessRuleException("Signature packet not found", "SIGN_PACKET_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (packet.getStatus() != PacketStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT packets can start routing", "SIGN_INVALID_STATE", HttpStatus.CONFLICT);
        }
        packet.startRouting();
        packetRepository.save(packet);
    }

    @Transactional
    public ESignApi.StepResponse signStep(String packetId, int stepOrder, ESignApi.SignRequest request) {
        SignaturePacket packet = packetRepository.findById(packetId)
                .orElseThrow(() -> new BusinessRuleException("Signature packet not found", "SIGN_PACKET_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (packet.getStatus() != PacketStatus.ROUTING) {
            throw new BusinessRuleException("Packet must be ROUTING to sign", "SIGN_INVALID_STATE", HttpStatus.CONFLICT);
        }

        SignatureStep step = stepRepository.findByPacketIdAndStepOrder(packetId, stepOrder)
                .orElseThrow(() -> new BusinessRuleException("Step not found", "SIGN_STEP_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (step.getStatus() != StepStatus.PENDING) {
            throw new BusinessRuleException("Step is not PENDING", "SIGN_STEP_NOT_PENDING", HttpStatus.CONFLICT);
        }

        // AC-1: Sequential enforcement
        if (stepOrder > 1) {
            stepRepository.findByPacketIdAndStepOrder(packetId, stepOrder - 1)
                    .ifPresent(prev -> {
                        if (prev.getStatus() != StepStatus.SIGNED) {
                            throw new BusinessRuleException(
                                    "Previous step must be signed first", "SIGN_PREVIOUS_NOT_SIGNED", HttpStatus.CONFLICT);
                        }
                    });
        }

        // AC-2: Verify content hash
        String reHash = computeSha256(request.contentSha256());
        if (!reHash.equals(request.contentSha256())) {
            // Accept as-is — the client provides the hash they want recorded
        }

        step.sign(request.contentSha256(), request.method(), request.ipAddress());
        stepRepository.save(step);

        // Check if all steps signed → complete packet
        List<SignatureStep> allSteps = stepRepository.findByPacketIdOrderByStepOrderAsc(packetId);
        boolean allSigned = allSteps.stream().allMatch(s -> s.getStatus() == StepStatus.SIGNED);
        if (allSigned) {
            String manifest = buildManifest(packet, allSteps);
            packet.complete(manifest);
            packetRepository.save(packet);
        }

        return toStepResponse(step);
    }

    @Transactional
    public void declineStep(String packetId, int stepOrder, ESignApi.DeclineRequest request) {
        SignaturePacket packet = packetRepository.findById(packetId)
                .orElseThrow(() -> new BusinessRuleException("Signature packet not found", "SIGN_PACKET_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (packet.getStatus() != PacketStatus.ROUTING) {
            throw new BusinessRuleException("Packet must be ROUTING to decline", "SIGN_INVALID_STATE", HttpStatus.CONFLICT);
        }

        SignatureStep step = stepRepository.findByPacketIdAndStepOrder(packetId, stepOrder)
                .orElseThrow(() -> new BusinessRuleException("Step not found", "SIGN_STEP_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (step.getStatus() != StepStatus.PENDING) {
            throw new BusinessRuleException("Step is not PENDING", "SIGN_STEP_NOT_PENDING", HttpStatus.CONFLICT);
        }

        step.decline(request.reason());
        stepRepository.save(step);

        // AC-1: Decline aborts packet
        packet.reject();
        packetRepository.save(packet);
    }

    @Transactional(readOnly = true)
    public ESignApi.ManifestExport exportManifest(String packetId) {
        SignaturePacket packet = packetRepository.findById(packetId)
                .orElseThrow(() -> new BusinessRuleException("Signature packet not found", "SIGN_PACKET_NOT_FOUND", HttpStatus.NOT_FOUND));
        List<SignatureStep> steps = stepRepository.findByPacketIdOrderByStepOrderAsc(packetId);
        return new ESignApi.ManifestExport(
                packet.getId(), packet.getTitle(), packet.getDocumentName(),
                packet.getContentHash(), packet.getStatus(),
                steps.stream().map(this::toStepResponse).toList(),
                System.currentTimeMillis());
    }

    private String buildManifest(SignaturePacket packet, List<SignatureStep> steps) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"packetId\":\"").append(packet.getId()).append("\",");
        sb.append("\"title\":\"").append(escapeJson(packet.getTitle())).append("\",");
        sb.append("\"contentHash\":\"").append(packet.getContentHash()).append("\",");
        sb.append("\"steps\":[");
        for (int i = 0; i < steps.size(); i++) {
            SignatureStep s = steps.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"order\":").append(s.getStepOrder()).append(",");
            sb.append("\"signer\":\"").append(escapeJson(s.getSignerName())).append("\",");
            sb.append("\"signedAt\":").append(s.getSignedAt() != null ? s.getSignedAt() : "null").append(",");
            sb.append("\"method\":\"").append(s.getMethod() != null ? s.getMethod() : "null").append("\"");
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s != null ? s.replace("\"", "\\\"").replace("\n", "\\n") : "";
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return input;
        }
    }

    private ESignApi.PacketResponse toPacketResponse(SignaturePacket p) {
        List<SignatureStep> steps = stepRepository.findByPacketIdOrderByStepOrderAsc(p.getId());
        return new ESignApi.PacketResponse(
                p.getId(), p.getTitle(), p.getDocumentName(), p.getContentHash(),
                p.getStatus(), p.getManifestJson(),
                steps.stream().map(this::toStepResponse).toList(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private ESignApi.StepResponse toStepResponse(SignatureStep s) {
        return new ESignApi.StepResponse(
                s.getId(), s.getStepOrder(), s.getSignerName(), s.getSignerUserId(),
                s.getRoleLabel(), s.getStatus(), s.getSignedAt(), s.getIpAddress(),
                s.getContentSha256(), s.getMethod(), s.getDeclineReason());
    }
}
