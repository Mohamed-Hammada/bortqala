package com.bemo.hr.esign.infrastructure;

import com.bemo.hr.esign.domain.SignaturePacket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignaturePacketRepository extends JpaRepository<SignaturePacket, String> {
    List<SignaturePacket> findAllByOrderByCreatedAtDesc();
    List<SignaturePacket> findByStatusOrderByCreatedAtDesc(com.bemo.hr.esign.domain.PacketStatus status);
}
