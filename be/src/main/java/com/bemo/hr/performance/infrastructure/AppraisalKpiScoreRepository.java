package com.bemo.hr.performance.infrastructure;

import com.bemo.hr.performance.domain.AppraisalKpiScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppraisalKpiScoreRepository extends JpaRepository<AppraisalKpiScore, String> {

    List<AppraisalKpiScore> findByAppraisalId(String appraisalId);

    void deleteByAppraisalId(String appraisalId);
}
