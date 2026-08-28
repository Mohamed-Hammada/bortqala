package com.bemo.hr.recruitment.infrastructure;

import com.bemo.hr.recruitment.domain.RecruitmentCvFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruitmentCvFileRepository extends JpaRepository<RecruitmentCvFile, String> {

    Optional<RecruitmentCvFile> findByIdAndAppId(String id, String appId);
}