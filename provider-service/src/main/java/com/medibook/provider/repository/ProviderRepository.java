package com.medibook.provider.repository;

import com.medibook.provider.entity.Provider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<Provider, String> {

    Optional<Provider> findByProviderId(String providerId);

    Optional<Provider> findByUserId(String userId);

    boolean existsByUserId(String userId);

    @Query("""
            select p from Provider p
            where (:verified is null or p.verified = :verified)
              and (:available is null or p.available = :available)
              and (:specialization is null or lower(p.specialization) = lower(:specialization))
              and (:location is null or lower(p.clinicAddress) like lower(concat('%', :location, '%')))
              and (:search is null
                or lower(concat(
                    coalesce(p.fullName, ''),
                    ' ',
                    coalesce(p.specialization, ''),
                    ' ',
                    coalesce(p.clinicName, ''),
                    ' ',
                    coalesce(p.clinicAddress, '')
                )) like lower(concat('%', :search, '%')))
            order by p.avgRating desc, p.reviewCount desc, p.createdAt desc
            """)
    List<Provider> searchProviders(
            @Param("search") String search,
            @Param("specialization") String specialization,
            @Param("location") String location,
            @Param("available") Boolean available,
            @Param("verified") Boolean verified);

    @Query("""
            select p.specialization as specialization, count(p) as providerCount
            from Provider p
            where p.verified = true
            group by p.specialization
            order by count(p) desc, p.specialization asc
            """)
    List<SpecializationCountView> countVerifiedProvidersBySpecialization();
}
