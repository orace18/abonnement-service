package com.clubchretien.abonnement.abonnement.schedulers;

import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.RelanceService;
import com.clubchretien.abonnement.services.NotificationService;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserProfileWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.User;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.mobileService.MobileServiceClient;
import com.owomi.rest.client.notif.NotificationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.owomi.commons.constants.MessagesConstants.EXPIRATION_WARNING_MESSAGE_TEMPLATE;
import static com.owomi.commons.constants.MessagesConstants.EXPIRATION_WARNING_TITLE;

@Component
public class RelanceScheduler{

    @Autowired
    RelanceService relanceService;
    @Autowired
    NotificationUtils notificationService;
    @Autowired
    private UserInfosClient userInfosClient;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private MobileServiceClient mobileServiceClient;

    private static Logger logger = LogManager.getLogger(AbonnementService.class);


    private static final int BATCH_SIZE = 50; // traiter 50 utilisateurs à la fois

    public RelanceScheduler(RelanceService relanceService, NotificationUtils notificationService) {
        this.relanceService = relanceService;
        this.notificationService = notificationService;
    }

        @Scheduled(cron = "0 0 8 * * ?")//  -> tous les jours à 8h
        public void sendRelanceNotifications() {

            logger.info("========== 🚀 DEBUT CRON RELANCE ==========");

            LocalDateTime today = LocalDateTime.now();
            logger.info("Date d'exécution du cron : {}", today);

            // J-x pour les relances
            List<Integer> relanceDays = List.of(3, 7, 10);
            logger.info("Jours de relance configurés : {}", relanceDays);

            for (Integer j : relanceDays) {

                logger.info("------ 🔔 Traitement des relances J-{} ------", j);

                LocalDateTime targetDate = today.plusDays(j);
                logger.info("Date cible pour J-{} : {}", j, targetDate);

                int offset = 0;
                List<Abonnement> batch;

                do {
                    logger.info("Recherche des abonnements | J-{} | offset={} | batchSize={}",
                            j, offset, BATCH_SIZE);

                    batch = relanceService.findAbonnementsToRelance(targetDate, j, offset, BATCH_SIZE);

                    logger.info("Nombre d'abonnements trouvés : {}", batch.size());

                    batch.forEach(abonnement -> {
                        Long userId = abonnement.getUserId();

                        logger.info("➡️ Début traitement utilisateur ID={} pour relance J-{}", userId, j);

                        try {
                            sendNotificationWithRetry(userId, j);
                            logger.info("✅ Traitement terminé pour utilisateur ID={} J-{}", userId, j);

                        } catch (Exception e) {
                            logger.error("❌ Erreur globale lors du traitement utilisateur ID={} J-{} : {}",
                                    userId, j, e.getMessage(), e);
                        }
                    });

                    offset += BATCH_SIZE;

                } while (!batch.isEmpty());

                logger.info("------ ✅ Fin traitement J-{} ------", j);
            }

            logger.info("========== 🎯 FIN COMPLETE CRON RELANCE ==========");
        }

    private void sendNotificationWithRetry(Long userId, int daysBeforeExpiry) throws OwomiException {

        int maxRetries = 2;
        int attempt = 0;
        boolean success = false;

        logger.info("Préparation envoi notification pour utilisateur {} | J-{}", userId, daysBeforeExpiry);

        // récupérer l'utilisateur
        String token = httpServletRequest.getHeader("access-token");

        logger.info("Récupération infos utilisateur ID={}", userId);
        CommonResponseWs userRes = userInfosClient.getUserInfos(userId, token);
        UserProfileWs profile = (UserProfileWs) userRes.getData();

        if (profile == null) {
            logger.warn("⚠️ Profil utilisateur NULL pour ID={}", userId);
            return;
        }

        logger.info("Récupération Firebase UID pour téléphone {}", profile.getPhoneFullNumber());

        CommonResponseWs firebaseUID = mobileServiceClient.getFirebaseUID(
                profile.getPhoneFullNumber(), token);

        String userFirebaseUID = (String) firebaseUID.getData();

        if (userFirebaseUID == null) {
            logger.warn("⚠️ Firebase UID NULL pour utilisateur ID={}", userId);
            return;
        }

        logger.info("Firebase UID trouvé : {}", userFirebaseUID);

        String message = String.format(EXPIRATION_WARNING_MESSAGE_TEMPLATE, daysBeforeExpiry);

        while (attempt <= maxRetries && !success) {

            try {
                logger.info("Tentative {} envoi notification | utilisateur={} | J-{}",
                        attempt + 1, userId, daysBeforeExpiry);

                notificationService.notificationInCCapp(userFirebaseUID, EXPIRATION_WARNING_TITLE, message, token);

                relanceService.markRelanceSent(userId, daysBeforeExpiry);

                logger.info("🎉 SUCCESS - Relance J-{} envoyée à utilisateur {}",
                        daysBeforeExpiry, userId);

                success = true;

            } catch (Exception e) {

                attempt++;

                logger.error("❌ Erreur tentative {} | utilisateur={} | J-{} | message={}",
                        attempt, userId, daysBeforeExpiry, e.getMessage(), e);

                if (attempt > maxRetries) {
                    logger.error("🔥 ECHEC DEFINITIF utilisateur={} J-{} après {} tentatives",
                            userId, daysBeforeExpiry, maxRetries);
                }
            }
        }
    }
}
