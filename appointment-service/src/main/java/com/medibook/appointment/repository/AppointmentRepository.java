package com.medibook.appointment.repository;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.enums.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    Optional<Appointment> findByAppointmentId(String appointmentId);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(String patientId);

    List<Appointment> findByProviderIdOrderByAppointmentDateDescStartTimeDesc(String providerId);

    List<Appointment> findByProviderIdAndAppointmentDateOrderByStartTimeAsc(String providerId, LocalDate appointmentDate);

    long countByProviderId(String providerId);

    boolean existsByPatientIdAndProviderIdAndStatus(String patientId, String providerId, AppointmentStatus status);

    @Query("""
            select a from Appointment a
            where a.patientId = :patientId
              and a.status = :status
              and (a.appointmentDate > :today
                or (a.appointmentDate = :today and a.startTime >= :currentTime))
            order by a.appointmentDate asc, a.startTime asc
            """)
    List<Appointment> findUpcomingByPatientId(
            @Param("patientId") String patientId,
            @Param("status") AppointmentStatus status,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime);

    @Query("""
            select a from Appointment a
            where a.providerId = :providerId
              and a.status = :status
              and (a.appointmentDate > :today
                or (a.appointmentDate = :today and a.startTime >= :currentTime))
            order by a.appointmentDate asc, a.startTime asc
            """)
    List<Appointment> findUpcomingByProviderId(
            @Param("providerId") String providerId,
            @Param("status") AppointmentStatus status,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime);

    @Query("""
            select a from Appointment a
            where (:status is null or a.status = :status)
              and (:patientId is null or a.patientId = :patientId)
              and (:providerId is null or a.providerId = :providerId)
              and (:appointmentDate is null or a.appointmentDate = :appointmentDate)
            order by a.appointmentDate desc, a.startTime desc, a.createdAt desc
            """)
    List<Appointment> searchAppointments(
            @Param("status") AppointmentStatus status,
            @Param("patientId") String patientId,
            @Param("providerId") String providerId,
            @Param("appointmentDate") LocalDate appointmentDate);
}
