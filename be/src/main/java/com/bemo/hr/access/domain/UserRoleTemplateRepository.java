package com.bemo.hr.access.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserRoleTemplateRepository extends JpaRepository<UserRoleTemplate, String> {

    @Query("""
            select t from UserRoleTemplate t
            where t.vertical in :verticals
              and (t.appId is null or t.appId = :appId)
            order by t.sortOrder asc, t.code asc
            """)
    List<UserRoleTemplate> findForTenant(@Param("appId") String appId,
                                         @Param("verticals") Collection<String> verticals);

    List<UserRoleTemplate> findByAppIdIsNullAndVerticalAndCode(String vertical, String code);
}
