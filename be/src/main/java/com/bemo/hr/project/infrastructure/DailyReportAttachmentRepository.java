package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.DailyReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyReportAttachmentRepository extends JpaRepository<DailyReportAttachment, String> {

    List<DailyReportAttachment> findByDailyReportIdOrderByUploadedAtDesc(String dailyReportId);

    void deleteByDailyReportId(String dailyReportId);
}
