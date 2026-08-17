package com.bemo.hr.product.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, String> {
    Optional<FeedbackItem> findByOperationId(String operationId);

    List<FeedbackItem> findAllByOrderByCreatedAtDesc();
}
