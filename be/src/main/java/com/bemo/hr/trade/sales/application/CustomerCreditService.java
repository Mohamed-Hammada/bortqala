package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerCreditProfile;
import com.bemo.hr.trade.sales.infrastructure.CustomerCreditProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CustomerCreditService {

    private final CustomerCreditProfileRepository repository;

    public CustomerCreditService(CustomerCreditProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerCreditProfile setCreditLimit(String customerId, BigDecimal creditLimit) {
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseGet(() -> new CustomerCreditProfile(customerId, creditLimit));
        profile.setCreditLimit(creditLimit);
        return repository.save(profile);
    }

    @Transactional
    public CustomerCreditProfile checkAndIncreaseExposure(String customerId, BigDecimal amount) {
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (profile.getCurrentExposure().add(amount).compareTo(profile.getCreditLimit()) > 0) {
            throw new BusinessRuleException("Order exceeds customer credit limit", "CREDIT_LIMIT_EXCEEDED", HttpStatus.CONFLICT);
        }
        profile.increaseExposure(amount);
        return repository.save(profile);
    }

    @Transactional
    public CustomerCreditProfile reduceExposure(String customerId, BigDecimal amount) {
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND));
        profile.reduceExposure(amount);
        return repository.save(profile);
    }

    @Transactional(readOnly = true)
    public CustomerCreditProfile getCreditProfile(String customerId) {
        return repository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
