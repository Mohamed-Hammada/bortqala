package com.bemo.hr.product.pack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndustryPackRepository extends JpaRepository<IndustryPack, String> {
    Optional<IndustryPack> findByCodeAndStatus(String code, String status);

    List<IndustryPack> findAllByStatusOrderByCode(String status);
}
