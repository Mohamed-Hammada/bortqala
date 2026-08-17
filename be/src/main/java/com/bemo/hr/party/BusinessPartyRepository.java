package com.bemo.hr.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessPartyRepository extends JpaRepository<BusinessParty, String> {
    List<BusinessParty> findAllByOrderByNameAsc();

    List<BusinessParty> findByPartyTypeOrderByNameAsc(String partyType);

    Optional<BusinessParty> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

    List<BusinessParty> findByTaxIdIgnoreCase(String taxId);
}
