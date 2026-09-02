package com.bemo.hr.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BusinessPartyRepository extends JpaRepository<BusinessParty, String> {
    List<BusinessParty> findAllByOrderByNameAsc();

    List<BusinessParty> findTop10ByNameContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(
            String name, String nameEn, String code);

    List<BusinessParty> findByPartyTypeOrderByNameAsc(String partyType);

    Optional<BusinessParty> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

    List<BusinessParty> findByTaxIdIgnoreCase(String taxId);

    long countByAppIdAndCreatedAtBefore(String appId, Instant before);
}
