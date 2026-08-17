package com.bemo.hr.finance.domain.posting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostingProfileLineRepository extends JpaRepository<PostingProfileLine, String> {
    List<PostingProfileLine> findByProfileIdOrderByLineNoAsc(String profileId);
}
