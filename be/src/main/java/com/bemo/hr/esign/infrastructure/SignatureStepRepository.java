package com.bemo.hr.esign.infrastructure;

import com.bemo.hr.esign.domain.SignatureStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignatureStepRepository extends JpaRepository<SignatureStep, String> {
    List<SignatureStep> findByPacketIdOrderByStepOrderAsc(String packetId);
    Optional<SignatureStep> findByPacketIdAndStepOrder(String packetId, int stepOrder);
}
