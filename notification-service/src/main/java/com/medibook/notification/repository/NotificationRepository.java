package com.medibook.notification.repository;

import com.medibook.notification.entity.Notification;
import com.medibook.notification.enums.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByNotificationId(String notificationId);

    List<Notification> findByRecipientIdOrderBySentAtDesc(String recipientId);

    List<Notification> findByRecipientIdAndReadOrderBySentAtDesc(String recipientId, boolean read);

    long countByRecipientIdAndRead(String recipientId, boolean read);

    List<Notification> findByTypeOrderBySentAtDesc(NotificationType type);

    List<Notification> findByRelatedIdOrderBySentAtDesc(String relatedId);

    void deleteByNotificationId(String notificationId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllReadByRecipientId(@Param("recipientId") String recipientId);

    @Query("""
            select n from Notification n
            where (:recipientId is null or n.recipientId = :recipientId)
              and (:type is null or n.type = :type)
              and (:channel is null or n.channel = :channel)
              and (:readState is null or n.read = :readState)
              and (:relatedId is null or n.relatedId = :relatedId)
            order by n.sentAt desc
            """)
    List<Notification> searchNotifications(
            @Param("recipientId") String recipientId,
            @Param("type") NotificationType type,
            @Param("channel") com.medibook.notification.enums.NotificationChannel channel,
            @Param("readState") Boolean readState,
            @Param("relatedId") String relatedId);
}
