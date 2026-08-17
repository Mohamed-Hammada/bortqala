package com.bemo.hr.product.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductEventDailyAggregateRepository extends JpaRepository<ProductEventDailyAggregate, String> {
    Optional<ProductEventDailyAggregate> findByEventDateAndEventNameAndFeatureKey(LocalDate date, String name, String feature);

    List<ProductEventDailyAggregate> findAllByOrderByEventDateDesc();
}
