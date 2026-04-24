package com.medibook.record.repository;

import com.medibook.record.entity.MedicalRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<MedicalRecord, String> {

    Optional<MedicalRecord> findByRecordId(String recordId);

    Optional<MedicalRecord> findByAppointmentId(String appointmentId);

    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(String patientId);

    List<MedicalRecord> findByProviderIdOrderByCreatedAtDesc(String providerId);

    List<MedicalRecord> findByFollowUpDateOrderByCreatedAtAsc(LocalDate followUpDate);

    List<MedicalRecord> findByFollowUpDateAndFollowUpReminderSentAtIsNullOrderByCreatedAtAsc(LocalDate followUpDate);

    long countByPatientId(String patientId);

    void deleteByRecordId(String recordId);
}
