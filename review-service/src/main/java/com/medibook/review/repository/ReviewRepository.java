package com.medibook.review.repository;

import com.medibook.review.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, String> {

    Optional<Review> findByReviewId(String reviewId);

    Optional<Review> findByAppointmentId(String appointmentId);

    List<Review> findByProviderIdOrderByReviewDateDesc(String providerId);

    List<Review> findByPatientIdOrderByReviewDateDesc(String patientId);

    boolean existsByAppointmentId(String appointmentId);

    long countByProviderId(String providerId);

    void deleteByReviewId(String reviewId);

    @Query("select avg(r.rating) from Review r where r.providerId = :providerId")
    Double avgRatingByProviderId(@Param("providerId") String providerId);

    @Query("""
            select r from Review r
            where (:providerId is null or r.providerId = :providerId)
              and (:patientId is null or r.patientId = :patientId)
              and (:rating is null or r.rating = :rating)
              and (:flagged is null or r.flagged = :flagged)
            order by r.reviewDate desc
            """)
    List<Review> searchReviews(
            @Param("providerId") String providerId,
            @Param("patientId") String patientId,
            @Param("rating") Integer rating,
            @Param("flagged") Boolean flagged);
}
