package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.BookableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookableResourceRepository extends JpaRepository<BookableResource, String> {
    List<BookableResource> findByAppIdOrderByCreatedAtDesc(String appId);
    List<BookableResource> findByAppIdAndActive(String appId, boolean active);
    List<BookableResource> findByAppIdAndKind(String appId, BookableResource.Kind kind);
    Optional<BookableResource> findByAppIdAndId(String appId, String id);
    Optional<BookableResource> findByAppIdAndCode(String appId, String code);
    long countByAppId(String appId);
}
