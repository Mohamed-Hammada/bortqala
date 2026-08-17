package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FxPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxPostingRepository extends JpaRepository<FxPosting, String> {
    Optional<FxPosting> findByOperationId(String operationId);
}
