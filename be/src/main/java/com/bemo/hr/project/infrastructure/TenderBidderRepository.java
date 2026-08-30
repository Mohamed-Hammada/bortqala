package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.BidderStatus;
import com.bemo.hr.project.domain.TenderBidder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenderBidderRepository extends JpaRepository<TenderBidder, String> {

    List<TenderBidder> findByTenderIdOrderByRankOrderAsc(String tenderId);

    List<TenderBidder> findByTenderIdAndStatus(String tenderId, BidderStatus status);

    void deleteByTenderId(String tenderId);
}
