package com.clubchretien.abonnement.abonnement.repositories;

import com.clubchretien.abonnement.abonnement.relance.RelanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelanceHistoryRepository extends JpaRepository<RelanceHistory, Long> {

    boolean existsByUserIdAndDaysBeforeExpiry(Long userId, int daysBeforeExpiry);

    //List<Partenaire> findBySubscriptionEndDatePaged(LocalDate date, int offset, int limit);


}