package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, String> {

    Optional<ExamAnswer> findByAppIdAndId(String appId, String id);

    List<ExamAnswer> findAllByAppIdAndVisitIdOrderByRecordedAtDesc(String appId, String visitId);
}
