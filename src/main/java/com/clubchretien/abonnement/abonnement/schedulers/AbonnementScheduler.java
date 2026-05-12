package com.clubchretien.abonnement.abonnement.schedulers;

import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AbonnementScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AbonnementScheduler.class);

    @Autowired
    private AbonnementService abonnementService;

    /**
     * Tâche quotidienne pour désactiver les membres dont l'abonnement est expiré.
     * S'exécute tous les jours à 01:00 du matin.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void verificationAbonnementsExpires() {
        logger.info("⏰ Cron Job: Lancement de la vérification des abonnements expirés");
        try {
            abonnementService.traiterAbonnementsExpires();
        } catch (Exception e) {
            logger.error("❌ Erreur critique lors du Cron Job d'expiration", e);
        }
        logger.info("⏰ Cron Job: Vérification terminée");
    }
}
