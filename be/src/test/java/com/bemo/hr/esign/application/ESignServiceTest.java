package com.bemo.hr.esign.application;

import com.bemo.hr.esign.api.ESignApi;
import com.bemo.hr.esign.domain.*;
import com.bemo.hr.esign.infrastructure.SignaturePacketRepository;
import com.bemo.hr.esign.infrastructure.SignatureStepRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ESignServiceTest {

    @Mock private SignaturePacketRepository packetRepository;
    @Mock private SignatureStepRepository stepRepository;
    @InjectMocks private ESignService service;

    @Test
    void createPacketCreatesStepsAndReturnsPacket() {
        ESignApi.CreateStepRequest step1 = new ESignApi.CreateStepRequest(1, "Alice", "user1", "Manager");
        ESignApi.CreateStepRequest step2 = new ESignApi.CreateStepRequest(2, "Bob", "user2", "Director");
        ESignApi.CreatePacketRequest req = new ESignApi.CreatePacketRequest(
                "Contract", "contract.pdf", "abc123hash", List.of(step1, step2));

        when(packetRepository.save(any(SignaturePacket.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(stepRepository.save(any(SignatureStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(stepRepository.findByPacketIdOrderByStepOrderAsc(anyString()))
                .thenReturn(List.of());

        ESignApi.PacketResponse result = service.createPacket(req);

        assertThat(result.title()).isEqualTo("Contract");
        verify(stepRepository, times(2)).save(any(SignatureStep.class));
    }

    @Test
    void signStepRejectsWhenPreviousNotSigned() {
        SignaturePacket packet = new SignaturePacket("Test", "doc", "hash");
        packet.startRouting();
        when(packetRepository.findById("p1")).thenReturn(Optional.of(packet));

        SignatureStep prevStep = new SignatureStep("p1", 1, "Alice", "u1", "M");
        prevStep.sign("hash1", SignatureMethod.CLICK_TO_ACCEPT, "127.0.0.1");
        SignatureStep currStep = new SignatureStep("p1", 2, "Bob", "u2", "D");
        when(stepRepository.findByPacketIdAndStepOrder("p1", 2)).thenReturn(Optional.of(currStep));
        when(stepRepository.findByPacketIdAndStepOrder("p1", 1)).thenReturn(Optional.of(prevStep));

        // This should throw because step 1 is already signed — but wait, it IS signed
        // Let me test when step 1 is NOT signed
        SignatureStep pendingPrev = new SignatureStep("p1", 1, "Alice", "u1", "M");
        when(stepRepository.findByPacketIdAndStepOrder("p1", 1)).thenReturn(Optional.of(pendingPrev));

        assertThatThrownBy(() -> service.signStep("p1", 2,
                new ESignApi.SignRequest("hash2", SignatureMethod.CLICK_TO_ACCEPT, "127.0.0.1")))
                .hasMessageContaining("Previous step must be signed first");
    }

    @Test
    void signStepCompletesPacketWhenAllStepsSigned() {
        SignaturePacket packet = new SignaturePacket("Test", "doc", "hash");
        packet.startRouting();
        when(packetRepository.findById("p1")).thenReturn(Optional.of(packet));

        SignatureStep step1 = new SignatureStep("p1", 1, "Alice", "u1", "M");
        step1.sign("hash1", SignatureMethod.CLICK_TO_ACCEPT, "127.0.0.1");
        when(stepRepository.findByPacketIdAndStepOrder("p1", 1)).thenReturn(Optional.of(step1));

        SignatureStep currStep = new SignatureStep("p1", 1, "Alice", "u1", "M");
        when(stepRepository.findByPacketIdAndStepOrder("p1", 1)).thenReturn(Optional.of(currStep));

        // Only 1 step, so after signing it completes
        when(stepRepository.findByPacketIdOrderByStepOrderAsc("p1"))
                .thenReturn(List.of(currStep));
        when(stepRepository.save(any(SignatureStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(packetRepository.save(any(SignaturePacket.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ESignApi.StepResponse result = service.signStep("p1", 1,
                new ESignApi.SignRequest("hash1", SignatureMethod.CLICK_TO_ACCEPT, "127.0.0.1"));

        assertThat(result.status()).isEqualTo(StepStatus.SIGNED);
        verify(packetRepository, atLeastOnce()).save(any(SignaturePacket.class));
    }

    @Test
    void declineStepRejectsPacket() {
        SignaturePacket packet = new SignaturePacket("Test", "doc", "hash");
        packet.startRouting();
        when(packetRepository.findById("p1")).thenReturn(Optional.of(packet));

        SignatureStep step = new SignatureStep("p1", 1, "Alice", "u1", "M");
        when(stepRepository.findByPacketIdAndStepOrder("p1", 1)).thenReturn(Optional.of(step));
        when(stepRepository.save(any(SignatureStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(packetRepository.save(any(SignaturePacket.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.declineStep("p1", 1, new ESignApi.DeclineRequest("I refuse"));

        assertThat(packet.getStatus()).isEqualTo(PacketStatus.REJECTED);
    }

    @Test
    void startRoutingRejectsNonDraftPacket() {
        SignaturePacket packet = new SignaturePacket("Test", "doc", "hash");
        packet.startRouting(); // now ROUTING
        when(packetRepository.findById("p1")).thenReturn(Optional.of(packet));

        assertThatThrownBy(() -> service.startRouting("p1"))
                .hasMessageContaining("Only DRAFT");
    }
}
