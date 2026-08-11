package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.CustomerCreditProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCreditProfileRepository extends JpaRepository<CustomerCreditProfile, String> {
    Optional<CustomerCreditProfile> findByCustomerId(String customerId);
}
