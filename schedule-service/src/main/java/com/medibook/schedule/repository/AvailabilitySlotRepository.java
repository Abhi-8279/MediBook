package com.medibook.schedule.repository;

import com.medibook.schedule.entity.AvailabilitySlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, String> {

    Optional<AvailabilitySlot> findBySlotId(String slotId);

    @Query("""
            select s from AvailabilitySlot s
            where s.providerId = :providerId
              and (:date is null or s.slotDate = :date)
              and (:dateFrom is null or s.slotDate >= :dateFrom)
              and (:dateTo is null or s.slotDate <= :dateTo)
              and (:includeBooked = true or s.booked = false)
              and (:includeBlocked = true or s.blocked = false)
            order by s.slotDate asc, s.startTime asc
            """)
    List<AvailabilitySlot> findSlots(
            @Param("providerId") String providerId,
            @Param("date") LocalDate date,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("includeBooked") boolean includeBooked,
            @Param("includeBlocked") boolean includeBlocked);

    @Query("""
            select count(s) > 0 from AvailabilitySlot s
            where s.providerId = :providerId
              and s.slotDate = :slotDate
              and (:excludeSlotId is null or s.slotId <> :excludeSlotId)
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsOverlappingSlot(
            @Param("providerId") String providerId,
            @Param("slotDate") LocalDate slotDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeSlotId") String excludeSlotId);
}
