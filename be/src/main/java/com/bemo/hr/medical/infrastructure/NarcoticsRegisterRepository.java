package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.NarcoticsRegisterEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NarcoticsRegisterRepository extends JpaRepository<NarcoticsRegisterEntry, String> {

    List<NarcoticsRegisterEntry> findAllByAppIdOrderBySignedAtDesc(String appId);

    @Query("SELECT n FROM NarcoticsRegisterEntry n WHERE n.appId = :appId AND n.signedAt >= :fromEpoch AND n.signedAt <= :toEpoch ORDER BY n.signedAt DESC")
    List<NarcoticsRegisterEntry> findAllInPeriod(@Param("appId") String appId, @Param("fromEpoch") long fromEpoch, @Param("toEpoch") long toEpoch);
}
