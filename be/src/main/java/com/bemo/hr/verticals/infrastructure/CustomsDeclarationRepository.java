package com.bemo.hr.verticals.infrastructure;

import com.bemo.hr.verticals.domain.CustomsDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomsDeclarationRepository extends JpaRepository<CustomsDeclaration, String> {
    List<CustomsDeclaration> findAllByOrderByCreatedAtDesc();
    Optional<CustomsDeclaration> findByFileNumber(String fileNumber);
}
