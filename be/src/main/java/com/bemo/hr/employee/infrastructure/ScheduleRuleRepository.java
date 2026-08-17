package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.ScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, String> {
    List<ScheduleRule> findByCategoryIdOrderByEffectiveFromAsc(String categoryId);

    void deleteByCategoryId(String categoryId);

    Optional<ScheduleRule> findFirstByCategoryIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
            String categoryId, LocalDate from, LocalDate to);
}
