package com.bemo.hr.finance.domain.posting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostingProfileRepository extends JpaRepository<PostingProfile, String> {
    List<PostingProfile> findByBusinessEventAndActiveTrueOrderByEffectiveFromDesc(String businessEvent);

    Optional<PostingProfile> findByCode(String code);
}
