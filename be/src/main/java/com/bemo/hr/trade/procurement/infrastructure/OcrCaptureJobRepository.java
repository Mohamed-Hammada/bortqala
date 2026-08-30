package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.OcrCaptureJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OcrCaptureJobRepository extends JpaRepository<OcrCaptureJob, String> {
    List<OcrCaptureJob> findByAppIdOrderByCreatedAtDesc(String appId);
}
