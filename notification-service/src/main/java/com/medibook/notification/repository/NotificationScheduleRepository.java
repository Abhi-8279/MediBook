package com.medibook.notification.repository;

import com.medibook.notification.entity.NotificationSchedule;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, String> {

    List<NotificationSchedule> findByProcessedFalseAndTriggerAtLessThanEqualOrderByTriggerAtAsc(Instant triggerAt);

    long deleteByRelatedIdAndRelatedTypeAndProcessedFalse(String relatedId, String relatedType);
}
