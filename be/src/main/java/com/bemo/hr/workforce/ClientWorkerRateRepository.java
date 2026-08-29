package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientWorkerRateRepository extends JpaRepository<ClientWorkerRate, String> {

    List<ClientWorkerRate> findByClientPartyIdAndWorkerCategoryId(String clientPartyId, String workerCategoryId);

    Optional<ClientWorkerRate> findFirstByClientPartyIdAndWorkerCategoryIdAndEffectiveFrom(String clientPartyId,
                                                                                          String workerCategoryId,
                                                                                          String effectiveFrom);
}