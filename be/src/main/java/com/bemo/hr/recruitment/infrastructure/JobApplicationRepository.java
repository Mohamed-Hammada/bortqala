package com.bemo.hr.recruitment.infrastructure;

import com.bemo.hr.recruitment.domain.ApplicationStage;
import com.bemo.hr.recruitment.domain.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, String> {
    List<JobApplication> findByOpeningIdOrderByCreatedAtDesc(String openingId);
    List<JobApplication> findByStageOrderByCreatedAtDesc(ApplicationStage stage);
    List<JobApplication> findAllByOrderByCreatedAtDesc();
    Optional<JobApplication> findByPhoneAndOpeningId(String phone, String openingId);
    Optional<JobApplication> findByEmailAndOpeningId(String email, String openingId);
    boolean existsByPhoneOrEmail(String phone, String email);
}
