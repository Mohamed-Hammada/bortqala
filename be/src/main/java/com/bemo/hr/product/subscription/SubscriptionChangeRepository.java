package com.bemo.hr.product.subscription;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface SubscriptionChangeRepository extends JpaRepository<SubscriptionChange,String>{Optional<SubscriptionChange> findByOperationId(String operationId);List<SubscriptionChange> findAllByOrderByChangedAtDesc();}
