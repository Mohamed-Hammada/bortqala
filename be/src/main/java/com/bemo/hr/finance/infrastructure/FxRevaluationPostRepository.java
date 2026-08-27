package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FxRevaluationPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FxRevaluationPostRepository extends JpaRepository<FxRevaluationPost, String> {
    boolean existsByCurrencyCodeAndYearMonth(String currencyCode, String yearMonth);

    List<FxRevaluationPost> findAllByOrderByPostedAtDesc();

    java.util.Optional<FxRevaluationPost> findFirstByCurrencyCodeOrderByYearMonthDesc(String currencyCode);
}
