package com.bemo.hr.product.subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan,String>{List<SubscriptionPlan> findAllByOrderByCodeAsc();}
