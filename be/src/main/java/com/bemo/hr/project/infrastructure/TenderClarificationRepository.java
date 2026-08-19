package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.TenderClarification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderClarificationRepository extends JpaRepository<TenderClarification, String> {

    List<TenderClarification> findByTenderIdOrderByAskedAtDesc(String tenderId);

    List<TenderClarification> findByTenderIdAndPublicAddendumTrue(String tenderId);
}
