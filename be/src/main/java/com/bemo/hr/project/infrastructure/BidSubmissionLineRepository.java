package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.BidSubmissionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidSubmissionLineRepository extends JpaRepository<BidSubmissionLine, String> {

    List<BidSubmissionLine> findByBidderId(String bidderId);

    void deleteByBidderId(String bidderId);
}
