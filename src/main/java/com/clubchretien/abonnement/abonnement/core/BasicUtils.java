package com.clubchretien.abonnement.abonnement.core;

import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.Partenaire;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.mobileService.MobileAuthenticationServiceClient;
import com.owomi.rest.client.mobileService.MobileServiceClient;
import com.owomi.rest.client.notif.NotificationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import static com.owomi.commons.constants.MessagesConstants.*;

@Service
public class BasicUtils {
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    MobileAuthenticationServiceClient mobileAuthenticationServiceClient;
    @Autowired
    MobileServiceClient mobileServiceClient;
    @Autowired
    private UserInfosClient userInfosClient;

    private static Logger logger = LogManager.getLogger(BasicUtils.class);


    public void setNotificationUtils(String titre, String message, String firebaseUId) throws OwomiException {
        this.notificationUtils.notificationInCCapp(firebaseUId,titre, message, CustomJwtUtils.getAccessToken(httpServletRequest));
    }

    /**
     * Envoie une notification de succès de paiement à l'utilisateur
     *
     * @param firebaseUId Firebase UID de l'utilisateur
     * @param abonnement L'abonnement concerné
     * @param tokenCcapp Token d'authentification
     * @return true si la notification a été envoyée avec succès, false sinon
     */
    private boolean envoyerNotificationSuccesPaiement(String firebaseUId, Abonnement abonnement, String tokenCcapp) {
        if (firebaseUId == null || firebaseUId.isEmpty()) {
            logger.warn("⚠️ FirebaseUID null, notification de succès de paiement ignorée");
            return false;
        }

        if (abonnement == null) {
            logger.warn("⚠️ Abonnement null, notification de succès de paiement ignorée");
            return false;
        }

        try {
            String message = String.format(
                    PAYMENT_SUCCESS_MESSAGE_TEMPLATE,
                    abonnement.getPrixAbonnement(),
                    abonnement.getDateDebut(),
                    abonnement.getDateFin(),
                    abonnement.getPaymentId()
            );

            notificationUtils.notificationInCCapp(firebaseUId, PAYMENT_SUCCESS_TITLE, message, tokenCcapp);
            logger.info("✅ Notification de succès de paiement envoyée à l'utilisateur {}", firebaseUId);
            return true;

        } catch (Exception e) {
            logger.warn("⚠️ Échec envoi notification paiement réussi (non bloquant): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une notification de bienvenue à l'utilisateur
     *
     * @param firebaseUId Firebase UID de l'utilisateur
     * @param displayName Nom d'affichage de l'utilisateur
     * @param abonnement L'abonnement concerné
     * @param tokenCcapp Token d'authentification
     * @return true si la notification a été envoyée avec succès, false sinon
     */
    private boolean envoyerNotificationBienvenue(String firebaseUId, String displayName, Abonnement abonnement, String tokenCcapp) {
        if (firebaseUId == null || firebaseUId.isEmpty()) {
            logger.warn("⚠️ FirebaseUID null, notification de bienvenue ignorée");
            return false;
        }

        if (abonnement == null || displayName == null) {
            logger.warn("⚠️ Données manquantes pour notification de bienvenue");
            return false;
        }

        try {
            String messageWelcome = String.format(
                    WELCOME_MESSAGE_TEMPLATE,
                    displayName,
                    abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle()
            );

            notificationUtils.notificationInCCapp(firebaseUId, WELCOME_TITLE, messageWelcome, tokenCcapp);
            logger.info("✅ Notification de bienvenue envoyée à l'utilisateur {}", firebaseUId);
            return true;

        } catch (Exception e) {
            logger.warn("⚠️ Échec envoi notification bienvenue (non bloquant): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une notification au parrain lorsqu'un nouveau membre rejoint son réseau
     *
     * @param parentId ID du parrain
     * @param nouveauMembreDisplayName Nom du nouveau membre
     * @param tokenCcapp Token d'authentification
     * @return true si la notification a été envoyée avec succès, false sinon
     */
    private boolean envoyerNotificationAuParrain(Long parentId, String nouveauMembreDisplayName, String tokenCcapp) {
        if (parentId == null) {
            logger.warn("⚠️ ParentId null, notification au parrain ignorée");
            return false;
        }

        if (tokenCcapp == null || tokenCcapp.isEmpty()) {
            logger.warn("⚠️ Token manquant, impossible d'envoyer notification au parrain");
            return false;
        }

        try {
            logger.debug("🔔 Démarrage envoi notification au parrain ID: {}", parentId);

            // Étape 1: Récupérer les infos du parent
            CommonResponseWs userRes = userInfosClient.getUserInfos(parentId, tokenCcapp);

            String parentPhoneNumber = null;
            String parentName = "";

            if (userRes != null && userRes.getData() != null && userRes.getData() instanceof Map) {
                Map<String, Object> parentData = (Map<String, Object>) userRes.getData();
                parentPhoneNumber = (String) parentData.get("phoneNumber");
                if (parentPhoneNumber == null) {
                    parentPhoneNumber = (String) parentData.get("phoneFullNumber");
                }
                parentName = (String) parentData.getOrDefault("displayName", "");
                logger.debug("Informations parent récupérées: phoneNumber={}, displayName={}", parentPhoneNumber, parentName);
            }

            if (parentPhoneNumber == null || parentPhoneNumber.isEmpty()) {
                logger.warn("⚠️ Pas de numéro de téléphone pour le parrain ID: {}, notification ignorée", parentId);
                return false;
            }

            // Étape 2: Récupérer le Firebase UID du parent
            CommonResponseWs firebaseUIDResponse = mobileServiceClient.getFirebaseUID(parentPhoneNumber, tokenCcapp);
            String parentFirebaseUID = (String) firebaseUIDResponse.getData();

            if (parentFirebaseUID == null || parentFirebaseUID.isEmpty()) {
                logger.warn("⚠️ Pas de firebaseUID pour le parrain, notification ignorée");
                return false;
            }

            logger.debug("Firebase UID du parent: {}", parentFirebaseUID);

            // Étape 3: Récupérer le firebase token du parent
            CommonResponseWs partenaireResponse = mobileAuthenticationServiceClient.getPartenaire(parentFirebaseUID, tokenCcapp);

            String firebaseToken = null;
            if (partenaireResponse != null && partenaireResponse.getData() != null) {
                if (partenaireResponse.getData() instanceof Map) {
                    Map<String, Object> partenaireData = (Map<String, Object>) partenaireResponse.getData();
                    firebaseToken = (String) partenaireData.get("firebasetoken");
                } else if (partenaireResponse.getData() instanceof Partenaire) {
                    firebaseToken = ((Partenaire) partenaireResponse.getData()).getFirebasetoken();
                }
            }

            if (firebaseToken == null || firebaseToken.isEmpty()) {
                logger.warn("⚠️ Pas de firebaseToken pour le parrain, notification ignorée");
                return false;
            }

            // Étape 4: Envoyer la notification
            String messageParent = String.format(
                    NEW_MEMBER_PARENT_MESSAGE_TEMPLATE,
                    nouveauMembreDisplayName,
                    parentPhoneNumber,
                    parentName
            );

            notificationUtils.notificationInCCapp(
                    parentFirebaseUID,
                    NEW_MEMBER_PARENT_TITLE,
                    messageParent,
                    firebaseToken
            );

            logger.info("✅ Notification envoyée au parrain ID: {} (firebaseUID: {})", parentId, parentFirebaseUID);
            return true;

        } catch (Exception e) {
            logger.warn("⚠️ Erreur envoi notification parrain ID: {} (non bloquant): {}", parentId, e.getMessage(), e);
            return false;
        }
    }
}
