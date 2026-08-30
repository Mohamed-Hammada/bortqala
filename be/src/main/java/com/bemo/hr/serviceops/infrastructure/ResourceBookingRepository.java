package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.ResourceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceBookingRepository extends JpaRepository<ResourceBooking, String> {
    List<ResourceBooking> findByAppIdOrderByStartTimeDesc(String appId);
    List<ResourceBooking> findByAppIdAndResourceIdOrderByStartTimeAsc(String appId, String resourceId);
    List<ResourceBooking> findByAppIdAndStatus(String appId, ResourceBooking.Status status);
    Optional<ResourceBooking> findByAppIdAndId(String appId, String id);

    @Query("SELECT b FROM ResourceBooking b WHERE b.appId = :appId AND b.resourceId = :resourceId AND b.status = 'CONFIRMED' AND b.startTime < :endTime AND b.endTime > :startTime")
    List<ResourceBooking> findConflictingBookings(@Param("appId") String appId,
                                                 @Param("resourceId") String resourceId,
                                                 @Param("startTime") long startTime,
                                                 @Param("endTime") long endTime);

    @Query("SELECT b FROM ResourceBooking b WHERE b.appId = :appId AND b.resourceId = :resourceId AND b.status = 'CONFIRMED' AND b.id <> :excludeId AND b.startTime < :endTime AND b.endTime > :startTime")
    List<ResourceBooking> findConflictingBookingsExcludingId(@Param("appId") String appId,
                                                            @Param("resourceId") String resourceId,
                                                            @Param("excludeId") String excludeId,
                                                            @Param("startTime") long startTime,
                                                            @Param("endTime") long endTime);
}
