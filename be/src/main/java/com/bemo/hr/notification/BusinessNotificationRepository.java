package com.bemo.hr.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BusinessNotificationRepository extends JpaRepository<BusinessNotification, String> {
    List<BusinessNotification> findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(String recipientUsername);

    long countByRecipientUsernameIgnoreCaseAndIsReadFalse(String recipientUsername);

    @Modifying
    @Query("UPDATE BusinessNotification n SET n.isRead = true, n.readAt = :now WHERE LOWER(n.recipientUsername) = LOWER(:username) AND n.isRead = false")
    int markAllAsRead(@Param("username") String username, @Param("now") java.time.Instant now);
}
