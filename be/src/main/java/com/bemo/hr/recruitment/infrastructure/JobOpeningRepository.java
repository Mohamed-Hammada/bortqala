package com.bemo.hr.recruitment.infrastructure;

import com.bemo.hr.recruitment.domain.JobOpening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobOpeningRepository extends JpaRepository<JobOpening, String> {
    List<JobOpening> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM JobOpening o WHERE o.status = com.bemo.hr.recruitment.domain.OpeningStatus.OPEN ORDER BY o.createdAt DESC")
    List<JobOpening> findOpenings();
}
