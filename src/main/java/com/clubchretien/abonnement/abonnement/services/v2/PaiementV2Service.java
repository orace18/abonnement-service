package com.clubchretien.abonnement.abonnement.services.v2;

import com.clubchretien.abonnement.abonnement.core.BaseConstant;
import com.clubchretien.abonnement.abonnement.core.utils.SubscriptionStateMapper;
import com.clubchretien.abonnement.abonnement.dto.PartenaireResponseDto;
import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.LogisticService;
import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserProfileWs;
import com.owomi.commons.ws.UserUpdateRequestWs;
import com.owomi.commons.ws.v2.PartnerPaymentInitializerWs;
import com.owomi.database.entities.*;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.StepInSystem;
import com.owomi.database.repositories.ArchiveParentRepository;
import com.owomi.database.repositories.PartnerTokenRepository;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.auth.UserDataClient;
import com.owomi.rest.client.partnerAdminClient.PaiementV2ClientService;
import com.owomi.rest.client.partnerAdminClient.PartnerAdminClientService;
import com.sun.xml.bind.v2.TODO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

import static com.clubchretien.abonnement.abonnement.core.BaseConstant.*;
import static com.owomi.commons.constants.MessagesConstants.*;
import static com.owomi.commons.constants.MessagesConstants.NEW_MEMBER_PARENT_TITLE;
import static com.owomi.commons.constants.MessagesConstants.WELCOME_MESSAGE_TEMPLATE;
import static com.owomi.commons.constants.MessagesConstants.WELCOME_TITLE;

@Service
public class PaiementV2Service {

    @Value("${partner.api.key}")
    private String PATNER_API_KEY;
    @Autowired
    PartnerTokenRepository partnerTokenRepository;
    @Autowired
    PaiementV2ClientService paiementV2ClientService;
    @Autowired
    PartnerAdminClientService partnerAdminClientService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    AbonnementRepository abonnementRepository;
    @Autowired
    TransactionCcappRepository transactionCcappRepository;
    @Autowired
    CustomJwtUtils customJwtUtils;
    @Autowired
    ArchiveParentRepository archiveParentRepository;
    @Autowired
    ResponseStatusUtils responseStatusUtils;
    @Autowired
    UserInfosClient userInfosClient;
    @Autowired
    UserDataClient userDataClient;
    @Autowired
    SubscriptionStateMapper subscriptionStateMapper;
    @Autowired
    ParrainageService parrainageService;
    @Autowired
    AbonnementService abonnementService;

    private static Logger logger = LogManager.getLogger(PaiementV2Service.class);

    /**
     * La version de V2 de d'initialisation de paiement
     * 
     * @param ws est l'entrée
     * @return un commonResponseWs comme reponse
     */
  /*
    public CommonResponseWs initPaiement(PartnerPaymentInitializerWs ws) {
        logger.debug("======== On est dans la fonction d'initialisation de paiement ==========");
        CommonResponseWs commonResponseWs1;
        try {
            commonResponseWs1 = partnerAdminClientService.getLassToken(
                    CustomJwtUtils.getAccessToken(httpServletRequest));
            logger.debug("La reponse de la recuperation des dernier token et refresh: {}", commonResponseWs1);
            if (commonResponseWs1.getStatus().equals(WsOperationResponseStatus.failed))
                logger.info("Une erreur s'est produite: {}", commonResponseWs1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.debug("La reponse: {}", commonResponseWs1);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) commonResponseWs1.getData();
        if (data == null) {
            logger.debug("Pas de data trouvée");
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Token non disponible",
                    404,
                    null);
        }
        // String accessToken = (String) data.get("accessToken");
        BaseConstant.ACCESS_TOKEN = this.extractPartnerAccessToken(data);
        if (ACCESS_TOKEN == null) {
            logger.debug("Pas de token d'accès trouvé");
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Access token manquant",
                    404,
                    null);
        }
        if (PATNER_API_KEY == null) {
            logger.debug("Pas de api key trouvée");
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "API key manquante",
                    500,
                    null);
        }
        try {
            CommonResponseWs commonResponseWs = this.paiementV2ClientService.initPaiment(
                    ws,
                    PATNER_API_KEY,
                    ACCESS_TOKEN);

            logger.debug("La reponse de l'innitialisation: {}", commonResponseWs);
            if (commonResponseWs.getCode() == 401) {
                logger.debug("===== Le token a expiré on va alors rafraichir =========");
                logger.info("Voici le contenu de 'data' : {}", data);
                BaseConstant.REFRESH_ACCESS_TOKEN = this.extractPartnerRefresh(data);
                ACCESS_TOKEN = this.extractPartnerAccessToken(data);
                logger.info("Le refresh obtenu: {}", REFRESH_ACCESS_TOKEN);
                logger.info("Le token obtenu: {}", ACCESS_TOKEN);
                try {
                    CommonResponseWs responseWs = this.partnerAdminClientService.getPartnerRefreshTokenOnDataBase(ACCESS_TOKEN,REFRESH_ACCESS_TOKEN,
                            CustomJwtUtils.getAccessToken(httpServletRequest));
                    logger.info("La reponse obtenu du refresh: {}", responseWs);
                    if (responseWs.getStatus().equals(WsOperationResponseStatus.failed)
                            && (responseWs.getCode() == 400 || responseWs.getCode() == 401)) {
                        logger.error("Echec du rafraichissement du token: {}", responseWs);
                        logger.info("Atassaa !!! Quelque chose n'a pas marché");
                        ACCESS_TOKEN = this.doGetPartnerJwt();
                        logger.debug("Le access-token nouveau: {}",ACCESS_TOKEN);
                        try {
                            CommonResponseWs responseInit = this.paiementV2ClientService.initPaiment(
                                    ws,
                                    PATNER_API_KEY,
                                    ACCESS_TOKEN);
                            logger.debug("La reponse de l;innitialisation: {}", responseInit);

                            if (responseInit.getStatus().equals(WsOperationResponseStatus.failed))
                                return new CommonResponseWs(responseInit.getStatus(), responseInit.getData(),
                                        responseInit.getInfo(), responseInit.getMessage(), -1,
                                        responseInit.getReference(), responseInit.getCode());
                            return responseInit;
                        } catch (Exception e) {
                            logger.error("Erreur lors de l'initialisation du paiement", e);
                            throw new RuntimeException(e);
                        }
                    }
                    logger.debug("Le contenue de son 'data' :{}", responseWs.getData());
                    if (responseWs.getData() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataAfterRefresh = (Map<String, Object>) responseWs.getData();
                        BaseConstant.ACCESS_TOKEN = this.extractPartnerAccessToken(dataAfterRefresh);
                        try {
                            CommonResponseWs responseInit = this.paiementV2ClientService.initPaiment(
                                    ws,
                                    PATNER_API_KEY,
                                    ACCESS_TOKEN);
                            logger.debug("La reponse de la requete: {}", responseInit);
                            if (responseInit.getStatus().equals(WsOperationResponseStatus.failed))
                                return new CommonResponseWs(responseInit.getStatus(), responseInit.getData(),
                                        responseInit.getInfo(), responseInit.getMessage(), -1,
                                        responseInit.getReference(), responseInit.getCode());
                            return responseInit;
                        } catch (Exception e) {
                            logger.error("Erreur lors de l'initialisation du paiement", e);
                            throw new RuntimeException(e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de l'initialisation du paiement", e);
                    throw new RuntimeException(e);
                }
            } else if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.failed)
                    && (commonResponseWs.getCode() == 403)) {
                return new CommonResponseWs(WsOperationResponseStatus.failed, "Une erreur s'est produite", 400, null);
            }
            logger.debug("======== La reponse de l'initialisation de paiement: {} =========", commonResponseWs);
            return commonResponseWs;
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation du paiement", e);
            throw new RuntimeException(e);
        }
    }
*/

    public CommonResponseWs initPaiement(PartnerPaymentInitializerWs ws) {
        logger.debug("======== On est dans la fonction d'initialisation de paiement ==========");

        // 1. Récupération du dernier token partenaire en base
        CommonResponseWs lastTokenResponse;
        try {
            lastTokenResponse = partnerAdminClientService.getLassToken(
                    CustomJwtUtils.getAccessToken(httpServletRequest));
            logger.debug("La reponse de la recuperation du dernier token: {}", lastTokenResponse);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du token partenaire", e);
            throw new RuntimeException(e);
        }

        // 2. Vérification que data est bien une Map (pas null, pas une String d'erreur)
        if (!lastTokenResponse.getStatus().equals(WsOperationResponseStatus.success)
                || !(lastTokenResponse.getData() instanceof Map)) {
            logger.warn("Token partenaire non disponible ou invalide: {}", lastTokenResponse);
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Token partenaire non disponible", 404, null);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) lastTokenResponse.getData();

        ACCESS_TOKEN = this.extractPartnerAccessToken(data);
        if (ACCESS_TOKEN == null) {
            logger.warn("Access token manquant dans la réponse");
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Access token manquant", 404, null);
        }

        if (PATNER_API_KEY == null) {
            logger.warn("API key non configurée");
            return new CommonResponseWs(WsOperationResponseStatus.failed, "API key manquante", 500, null);
        }

        // 3. Tentative initiale de paiement
        CommonResponseWs initResponse;
        try {
            initResponse = this.paiementV2ClientService.initPaiment(ws, PATNER_API_KEY, ACCESS_TOKEN);
            logger.debug("La reponse de l'initialisation: {}", initResponse);
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation du paiement", e);
            throw new RuntimeException(e);
        }

        // 4. Si token expiré (401), on tente le refresh
        if (initResponse.getCode() == 401) {
            logger.debug("===== Token expiré, tentative de refresh =========");

            REFRESH_ACCESS_TOKEN = this.extractPartnerRefresh(data);
            ACCESS_TOKEN = this.extractPartnerAccessToken(data);
            logger.info("Refresh token: {}", REFRESH_ACCESS_TOKEN);
            logger.info("Access token: {}", ACCESS_TOKEN);

            CommonResponseWs refreshResponse;
            try {
                refreshResponse = this.partnerAdminClientService.getPartnerRefreshTokenOnDataBase(
                        ACCESS_TOKEN, REFRESH_ACCESS_TOKEN,
                        CustomJwtUtils.getAccessToken(httpServletRequest));

                logger.info("Reponse du refresh: {}", refreshResponse);
            } catch (Exception e) {
                logger.error("Erreur lors du refresh du token", e);
                throw new RuntimeException(e);
            }

            // 5. Refresh réussi → on extrait le nouveau token et on retente
            if (refreshResponse.getStatus().equals(WsOperationResponseStatus.success)
                    && refreshResponse.getData() instanceof Map) {
                logger.debug("Refresh réussi, nouvelle tentative d'initialisation");
                @SuppressWarnings("unchecked")
                Map<String, Object> dataAfterRefresh = (Map<String, Object>) refreshResponse.getData();
                ACCESS_TOKEN = this.extractPartnerAccessToken(dataAfterRefresh);

            } else {
                // 6. Refresh échoué (400, 401, 500, data null...) → on refait un get-jwt complet
                logger.warn("Refresh échoué (status={}, code={}), fallback sur doGetPartnerJwt",
                        refreshResponse.getStatus(), refreshResponse.getCode());
                ACCESS_TOKEN = this.doGetPartnerJwt();
                logger.debug("Nouveau access token via doGetPartnerJwt: {}", ACCESS_TOKEN);
            }

            // 7. Retry avec le token fraîchement obtenu
            try {
                CommonResponseWs retryResponse = this.paiementV2ClientService.initPaiment(
                        ws, PATNER_API_KEY, ACCESS_TOKEN);
                logger.debug("Reponse après retry: {}", retryResponse);
                return retryResponse;
            } catch (Exception e) {
                logger.error("Erreur lors du retry d'initialisation du paiement", e);
                throw new RuntimeException(e);
            }
        }

        // 8. Erreur 403
        if (initResponse.getStatus().equals(WsOperationResponseStatus.failed)
                && initResponse.getCode() == 403) {
            logger.warn("Accès refusé (403)");
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Accès refusé", 400, null);
        }

        logger.debug("======== Initialisation de paiement terminée: {} =========", initResponse);
        return initResponse;
    }

    /**
     * Cette fonction permet d'extraire le accessToken enregistré dans la base de
     * données
     * 
     * @param donneeServeur
     * @return
     */
    private String extractPartnerAccessToken(Map<String, Object> donneeServeur) {
        try {
            logger.debug("========== Voici la donnee recu: ===============> {}", donneeServeur);
            if (donneeServeur.isEmpty()) {
                return "La donnée serveur passée est vide";
            }
            BaseConstant.ACCESS_TOKEN = (String) donneeServeur.get(CONST_ACCESS_TOKEN);
            if (ACCESS_TOKEN == null) {
                return "Le champs 'accessToken' est vide";
            }
            return ACCESS_TOKEN;
        } catch (Exception e) {
            logger.warn("Erreur lors de l'extraction du token", (Object) e.getStackTrace());
            return "Une erreur s'est produite: " + e.getMessage();
        }
    }

    /**
     * Cette fonction permet d'extraire le refresh enregistré dans la base de
     * données
     * 
     * @param donneeServeur
     * @return
     *
     */
    private String extractPartnerRefresh(Map<String, Object> donneeServeur) {
        try {
            logger.debug("========== Voici la donnée recu: ===============> {}", donneeServeur);
            if (donneeServeur.isEmpty()) {
                return "La donnée serveur passée est vide";
            }
            BaseConstant.REFRESH_ACCESS_TOKEN = (String) donneeServeur.get(CONST_REFRESH_ACCESS_TOKEN);
            if (REFRESH_ACCESS_TOKEN == null) {
                return "Le champs 'refresh' est vide";
            }
            return REFRESH_ACCESS_TOKEN;
        } catch (Exception e) {
            logger.warn("Erreur lors de l'extraction du refresh token", (Object) e.getStackTrace());
            return "Une erreur s'est produite: " + e.getMessage();
        }
    }

    /**
     * Cette fonction a pour but de faire un nouveau get partner jwt au cas ou le
     * refresh expirait
     */
    private String doGetPartnerJwt() {
        CommonResponseWs responseWs = this.partnerAdminClientService
                .getLassToken(CustomJwtUtils.getAccessToken(httpServletRequest));
        logger.debug("La reponse: {}", responseWs);
        Map<String, Object> data = this.mapper(responseWs);
        BaseConstant.ACCESS_TOKEN = this.extractPartnerAccessToken(data);
        logger.debug("Le token dans doGetPartnerJwt: {}", ACCESS_TOKEN);
        CommonResponseWs commonResponseWs = this.partnerAdminClientService.getPartnerTokenOnDataBase(ACCESS_TOKEN, CustomJwtUtils.getAccessToken(httpServletRequest));
        logger.debug("La reponse de token dans la base: {}", commonResponseWs);
        if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) {
            logger.debug("La reponse: {}", responseWs);
            Map<String, Object> reponse = this.mapper(commonResponseWs);
            ACCESS_TOKEN = this.extractPartnerAccessToken(reponse);
            logger.debug("Le token extrait: {}", ACCESS_TOKEN);
            return ACCESS_TOKEN;
        }
        return "";
    }

    /**
     * Fonction qui mappe un commomResponse
     */
    public Map<String, Object> mapper(CommonResponseWs responseWs) {
        logger.info("Le commonResponse fourni:{}", responseWs);
        @SuppressWarnings("unchecked")
        Map<String, Object> reponse = (Map<String, Object>) responseWs.getData();
        return reponse;
    }

    /**
     * Cette fonction est le callback apres paiement
     * 
     * @param idPaiement
     * @return
     */
    public CommonResponseWs callback(Long idPaiement) throws OwomiException {
        logger.debug("=========== Nous somme dans la fonction de callback =========");
        ACCESS_TOKEN = this.doGetPartnerJwt();
        String tokenJawupay = ACCESS_TOKEN;
        Optional<Abonnement> ab = abonnementRepository.findByPaymentId(idPaiement);
        if (ab.isEmpty()) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Abonnement non trouvee", 404, null);
        }
        Abonnement abonnement = ab.get();
        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(idPaiement);
        if (transactionsCcapp == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Transaction non trouvee", 404, null);
        }
        ArchiveParent archiveParent = archiveParentRepository.findByAbonnementId(abonnement.getId());
        if (archiveParent == null) {
            logger.info("ℹ️ Pas d'archive parent pour abonnementId: {} - Continuation sans parrainage",
                    abonnement.getId());
        }
        CommonResponseWs statutPaiementresponseWs = this.checkPaiement(idPaiement);
        if (statutPaiementresponseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Paiement echoue", 400,
                    statutPaiementresponseWs.getData());
        }
        String statut = (String) statutPaiementresponseWs.getData();
        if (!statut.equals(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Un probleme avec le paiement", 400,
                    statutPaiementresponseWs.getData());
        }

        if (statut.equals(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))) {
            abonnement.setActif(true);
            abonnement.setStatutPaiement(StatutPaiement.PAYE);
            if (abonnement.getDateDebut() == null || abonnement.getDateFin() == null) {
                logger.warn("Dates NULL détectées, recalcul...");
                LocalDateTime maintenant = LocalDateTime.now();
                abonnement.setDateDebut(maintenant);
                abonnement.setDateFin(maintenant.plusMonths(
                        abonnement.getFormuleAbonnement().getDuree().getMois()));
            }
            logger.info("Abonnement ID {} - StatutPaiement: PAYE, Actif: true", abonnement.getId());
            boolean gestionReussie = this.abonnementService.gestionNiveauxService.appliquerGestionNiveau(abonnement);
            logger.debug("========================Si ABONNEMENT EST BON ACTIF ================");
            this.abonnementService.gererActivationAbonnement(abonnement);
            this.abonnementService.updateTransaction(idPaiement);
            transactionsCcapp.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success));
            transactionsCcapp.setDateResponse(new Date());
            transactionCcappRepository.save(transactionsCcapp);
            abonnementRepository.save(abonnement);
            CommonResponseWs reseauResponseWs = this.openMyCommunity(idPaiement, tokenJawupay);
            logger.debug("La reponse du reseau:{}", reseauResponseWs);
            // V2: Après paiement, on met le step à PAIEMENT (step 1)
            // Le passage à COMPLEMENT_INFO puis LINKEDIN se fera via les APIs complete-info
            // et user-step
            try {
                CommonResponseWs stepUpdate = this.setUserStepAfterPayment();
                logger.info("Step mis à jour après paiement: {}", stepUpdate);
            } catch (Exception e) {
                logger.warn("Erreur mise à jour du step après paiement (non bloquant): {}", e.getMessage());
            }
            return new CommonResponseWs(WsOperationResponseStatus.success, "Callback traite avec succes", 200, null);
        } else {
            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
            logger.error("Échec gestion niveau pour abonnement ID: {}", abonnement.getId());
            abonnementRepository.save(abonnement);
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Echec", 500, null);
        }
    }

    /**
     * Cette fonction permet de mettre les position du user
     * 
     * @param isOk
     * @return
     */
    public CommonResponseWs setUserPosition(boolean isOk) {
        logger.debug("========== Nous somme dans la fonction de verification de la position =====================");
        String tokenCcapp = CustomJwtUtils.getAccessToken(httpServletRequest);
        UserProfileWs userProfileWs = CustomJwtUtils.getCurrentUser();
        UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
        if (userProfileWs == null) {
            return new CommonResponseWs(WsOperationResponseStatus.error, "User non trouvee", 404, false);
        }
        if (isOk) {
            updateRequestWs.setUserStep(StepInSystem.LINKEDIN);
            updateRequestWs.setInSystem(true);
            try {
                userInfosClient.updateUserAndProfile(updateRequestWs, tokenCcapp);
                logger.info(" Profil utilisateur mis à jour avec succès");
                return new CommonResponseWs(WsOperationResponseStatus.success,
                        " Profil utilisateur mis à jour avec succès", 200, true);
            } catch (Exception e) {
                logger.warn("Impossible de mettre à jour le profil utilisateur (non bloquant): {}",
                        e.getMessage());
            }
        }
        return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur inatendue", 400, false);
    }

    /**
     * Cette fonction permet de verifier les condition pour l'ouverture de la
     * communaute
     * 
     * @param idPaiment
     * @param tokenJawupay
     * @return
     */
    public CommonResponseWs openMyCommunity(Long idPaiment, String tokenJawupay) {
        logger.debug("======== Nous sommes dans la fonction j'ai fini ouvre ma communaute ============");
        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(idPaiment);
        if (transactionsCcapp == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Transaction non trouvee", 404, null);
        }
        String transactionstatut = transactionsCcapp.getStatut();
        if (transactionstatut.equals(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))) {
            Abonnement abonnement = abonnementRepository.findByPaymentId(idPaiment).orElseThrow();
            if (abonnement == null) {
                return new CommonResponseWs(WsOperationResponseStatus.failed, "Abonnement non trouvee", 404, null);
            }
            StatutPaiement abonnementStatut = abonnement.getStatutPaiement();
            if (abonnementStatut != StatutPaiement.PAYE) {
                return new CommonResponseWs(WsOperationResponseStatus.error, "Un probleme est survenue", 500,
                        abonnementStatut);
            }
            if ((abonnementStatut == StatutPaiement.PAYE) && (transactionstatut
                    .equals(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)))) {
                boolean etatInSystem = Objects.requireNonNull(CustomJwtUtils.getCurrentUser()).getInSystem();
                StepInSystem etatUserStep = CustomJwtUtils.getCurrentUser().getUserStep();
                if (etatUserStep == StepInSystem.LINKEDIN && etatInSystem == true) {
                    try {
                        ACCESS_TOKEN = this.doGetPartnerJwt();
                        // TODO: Appeler les subscriptions state ccapp et jawupay et faire des
                        // verification
                        CommonResponseWs subscriptionStateCCAppResponseWs = this.userInfosClient
                                .getSubscriptionStateOnCCAPP(ACCESS_TOKEN);
                        logger.debug("La reponse de subscriptuion state de ccapp: {}",
                                subscriptionStateCCAppResponseWs);
                        if (subscriptionStateCCAppResponseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
                            return new CommonResponseWs(WsOperationResponseStatus.failed, "Une erreur s'est produite",
                                    400, subscriptionStateCCAppResponseWs);
                        }
                        PartenaireResponseDto partenaireResponseDtoCCAPP = subscriptionStateMapper
                                .jsonToDto(subscriptionStateCCAppResponseWs.getData());
                        CommonResponseWs subscriptionStateJawupayWs = this.userInfosClient
                                .getSubscriptionStateOnJawupay(tokenJawupay);
                        logger.debug("La reponse de subscriptuion state de jawupay: {}",
                                subscriptionStateCCAppResponseWs);
                        if (subscriptionStateJawupayWs.getStatus().equals(WsOperationResponseStatus.failed)) {
                            return new CommonResponseWs(WsOperationResponseStatus.failed, "Une erreur s'est produite",
                                    400, subscriptionStateJawupayWs);
                        }
                        PartenaireResponseDto partenaireResponseDtoJawupay = subscriptionStateMapper
                                .jsonToDto(subscriptionStateJawupayWs);
                        String phoneNumber = partenaireResponseDtoCCAPP.getData().getPartenaire().getPhoneNumber();
                        boolean pin = partenaireResponseDtoJawupay.getData().getPinState();
                        if (phoneNumber != null && pin == true) {
                            CommonResponseWs parrainageResponseWs = this.parrainageService
                                    .gererReseauAndCommission(idPaiment);
                            logger.debug("Le resultat du reseautage:{}", parrainageResponseWs);
                            if (parrainageResponseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
                                return new CommonResponseWs(parrainageResponseWs.getStatus(),
                                        parrainageResponseWs.getMessage(), parrainageResponseWs.getCode(), false);
                            }
                            return new CommonResponseWs(WsOperationResponseStatus.success, "Tout est bon", 200, true);
                        }
                    } catch (RuntimeException e) {
                        throw new RuntimeException(e);
                    } catch (OwomiException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return new CommonResponseWs(WsOperationResponseStatus.error, "Un probleme est survenue", 400, false);
    }

    /**
     * Cette fonction fait le check chez jawupay
     * 
     * @param idPaiement
     * @return
     */
    public CommonResponseWs checkPaiement(Long idPaiement) {
        logger.debug("===== Nous sommes dans le check =========");
        ACCESS_TOKEN = this.doGetPartnerJwt();
        CommonResponseWs responseWs = this.paiementV2ClientService.checkPaiement(idPaiement, PATNER_API_KEY,
                ACCESS_TOKEN);
        if (responseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Echec de verification de statut", 400, null);
        }
        logger.debug("La reponse de verification de statuts:{}", responseWs);
        return new CommonResponseWs(WsOperationResponseStatus.success, "Verification de statut avec succes", 200,
                responseWs.getData());
    }

    /**
     * V2: Met le step de l'utilisateur à PAIEMENT après confirmation du paiement.
     * C'est le step 1 du flow V2 (paiement effectué, attente complément d'info).
     * 
     * @return CommonResponseWs avec le résultat de la mise à jour
     */
    private CommonResponseWs setUserStepAfterPayment() {
        try {
            String token = CustomJwtUtils.getAccessToken(httpServletRequest);
            UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
            updateRequestWs.setUserStep(StepInSystem.PAIEMENT);
            CommonResponseWs response = this.userInfosClient.updateUserAndProfile(updateRequestWs, token);
            logger.info("Step utilisateur mis à jour à PAIEMENT (step 1 V2)");
            return response;
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du step après paiement: {}", e.getMessage());
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur mise à jour step: " + e.getMessage(),
                    500, null);
        }
    }

}
