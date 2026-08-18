package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerCreditProfile;
import com.bemo.hr.trade.sales.infrastructure.CustomerCreditProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class CustomerCreditService {

    private final CustomerCreditProfileRepository repository;

    public CustomerCreditService(CustomerCreditProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerCreditProfile setCreditLimit(String customerId, BigDecimal creditLimit) {
        log.debug("setCreditLimit called with customerId={}, creditLimit={}", customerId, creditLimit);
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseGet(() -> new CustomerCreditProfile(customerId, creditLimit));
        profile.setCreditLimit(creditLimit);
        CustomerCreditProfile saved = repository.save(profile);
        log.info("CustomerCreditProfile for customer {} credit limit set to {} successfully", customerId, creditLimit);
        return saved;
    }

    @Transactional
    public CustomerCreditProfile checkAndIncreaseExposure(String customerId, BigDecimal amount) {
        log.debug("checkAndIncreaseExposure called with customerId={}, amount={}", customerId, amount);
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer credit profile not found for customerId={}", customerId);
                    return new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
                });

        if (profile.getCurrentExposure().add(amount).compareTo(profile.getCreditLimit()) > 0) {
            log.warn("Validation failed: Order for customer {} exceeds credit limit (exposure={}, amount={}, limit={})", customerId, profile.getCurrentExposure(), amount, profile.getCreditLimit());
            throw new BusinessRuleException("Order exceeds customer credit limit", "CREDIT_LIMIT_EXCEEDED", HttpStatus.CONFLICT);
        }
        profile.increaseExposure(amount);
        CustomerCreditProfile saved = repository.save(profile);
        log.info("CustomerCreditProfile for customer {} exposure increased by {} successfully", customerId, amount);
        return saved;
    }

    @Transactional
    public CustomerCreditProfile reduceExposure(String customerId, BigDecimal amount) {
        log.debug("reduceExposure called with customerId={}, amount={}", customerId, amount);
        CustomerCreditProfile profile = repository.findByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer credit profile not found for customerId={}", customerId);
                    return new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
        profile.reduceExposure(amount);
        CustomerCreditProfile saved = repository.save(profile);
        log.info("CustomerCreditProfile for customer {} exposure reduced by {} successfully", customerId, amount);
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerCreditProfile getCreditProfile(String customerId) {
        log.debug("getCreditProfile called with customerId={}", customerId);
        return repository.findByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer credit profile not found for customerId={}", customerId);
                    return new BusinessRuleException("Customer credit profile not found", "CREDIT_PROFILE_NOT_FOUND", HttpStatus.NOT_FOUND);
                });
    }
}
