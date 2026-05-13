package com.medibook.payment.repository;

import com.medibook.payment.entity.Payment;
import com.medibook.payment.enums.PaymentMode;
import com.medibook.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByAppointmentId(String appointmentId);

    List<Payment> findByPatientIdOrderByCreatedAtDesc(String patientId);

    List<Payment> findByProviderIdOrderByCreatedAtDesc(String providerId);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    @Query("""
            select p from Payment p
            where (:status is null or p.status = :status)
              and (:mode is null or p.mode = :mode)
              and (:patientId is null or p.patientId = :patientId)
              and (:providerId is null or p.providerId = :providerId)
              and (:paidFrom is null or p.paidAt >= :paidFrom)
              and (:paidTo is null or p.paidAt <= :paidTo)
            order by p.createdAt desc
            """)
    List<Payment> searchPayments(
            @Param("status") PaymentStatus status,
            @Param("mode") PaymentMode mode,
            @Param("patientId") String patientId,
            @Param("providerId") String providerId,
            @Param("paidFrom") Instant paidFrom,
            @Param("paidTo") Instant paidTo);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.providerId = :providerId
              and p.status = :status
              and (:paidFrom is null or p.paidAt >= :paidFrom)
              and (:paidTo is null or p.paidAt <= :paidTo)
            """)
    BigDecimal sumAmountByProviderIdAndStatusBetween(
            @Param("providerId") String providerId,
            @Param("status") PaymentStatus status,
            @Param("paidFrom") Instant paidFrom,
            @Param("paidTo") Instant paidTo);

    @Query("""
            select count(p)
            from Payment p
            where p.providerId = :providerId
              and p.status = :status
              and (:paidFrom is null or p.paidAt >= :paidFrom)
              and (:paidTo is null or p.paidAt <= :paidTo)
            """)
    long countByProviderIdAndStatusBetween(
            @Param("providerId") String providerId,
            @Param("status") PaymentStatus status,
            @Param("paidFrom") Instant paidFrom,
            @Param("paidTo") Instant paidTo);

    @Query(value = """
            select date_format(paid_at, '%Y-%m') as revenueMonth,
                   coalesce(sum(amount), 0) as totalRevenue,
                   count(*) as paidTransactionCount
            from payments
            where provider_id = :providerId
              and status = 'PAID'
              and paid_at is not null
              and (:paidFrom is null or paid_at >= :paidFrom)
              and (:paidTo is null or paid_at <= :paidTo)
            group by date_format(paid_at, '%Y-%m')
            order by revenueMonth asc
            """, nativeQuery = true)
    List<MonthlyRevenueView> summarizeMonthlyRevenueByProviderId(
            @Param("providerId") String providerId,
            @Param("paidFrom") Instant paidFrom,
            @Param("paidTo") Instant paidTo);
}
