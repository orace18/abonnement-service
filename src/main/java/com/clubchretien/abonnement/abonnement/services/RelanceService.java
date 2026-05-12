package com.clubchretien.abonnement.abonnement.services;

import com.clubchretien.abonnement.abonnement.relance.RelanceHistory;
import com.clubchretien.abonnement.abonnement.repositories.RelanceHistoryRepository;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.UserRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RelanceService {

    @Autowired
    RelanceHistoryRepository historyRepository;
    @Autowired
    AbonnementRepository abonnementRepository;

    //Pageable pageable = PageRequest.of(0,10);
    private static Logger logger = LogManager.getLogger(AbonnementService.class);



   /* public RelanceService(AbonnementRepository abonnementRepository,
                          UserRepository userRepository,
                          RelanceHistoryRepository historyRepository) {
        this.abonnementRepository = abonnementRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }*/

    public List<Abonnement> findAbonnementsToRelance(LocalDateTime targetDate, int daysBeforeExpiry, int offset, int limit) {
        List<Abonnement> abonnements = abonnementRepository.findByDateFin(targetDate);
        logger.info("📊 {} abonnements en cours d'expiration à relancer", abonnements.size());

        // Filtrer ceux déjà relancés
        return abonnements.stream()
                .filter(a -> !historyRepository.existsByUserIdAndDaysBeforeExpiry(a.getUserId(), daysBeforeExpiry))
                .toList();

    }



    public void markRelanceSent(Long userId, int daysBeforeExpiry) {
        RelanceHistory history = new RelanceHistory();
        history.setUserId(userId);
        history.setDaysBeforeExpiry(daysBeforeExpiry);
        history.setRelanceDate(LocalDate.now());
        historyRepository.save(history);
        logger.info("📊 {} Relance hystory créer {}", history);

    }
}