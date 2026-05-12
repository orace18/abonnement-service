package com.clubchretien.abonnement.abonnement.services;

import com.clubchretien.abonnement.abonnement.dto.StatutAbonnementDto;
import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserUpdateRequestWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.TransactionsCcapp;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.StepInSystem;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.AbonnementClient;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@Service
public class LogisticService {
    private static Logger logger = LogManager.getLogger(LogisticService.class);

    @Autowired
    private UserInfosClient userInfosClient;

    @Autowired
    private AbonnementService abonnementService;

    @Autowired
    private TransactionCcappRepository transactionCcappRepository;

    @Autowired
    private AbonnementClient abonnementClient;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * Récupère les informations complètes de l'utilisateur avec vérification automatique
     * des paiements en ECHEC puis EN_ATTENTE
     *
     * ALGORITHME :
     * 1. Vérifier dernier ECHEC → Si SUCCESS chez JawuPay → Activer → STOP
     * 2. Sinon, vérifier dernier PENDING → Si SUCCESS chez JawuPay → Activer → STOP
     * 3. Retourner les infos utilisateur avec statut abonnement actuel
     *
     * ✅ SANS @Transactional pour permettre la gestion fine des transactions
     */
    public CommonResponseWs getUserInfosWithAbonnement() {
        try {
            logger.info("=== 🚀 Début getUserInfosWithAbonnement ===");

            Long userId = CustomJwtUtils.getCurrentUserId();
            logger.info("📋 UserId connecté: {}", userId);

            boolean paiementConfirme = false;

            // ============================================
            // ÉTAPE 1 : VÉRIFIER LE DERNIER ÉCHEC
            // ============================================
            logger.info("🔍 ÉTAPE 1 : Recherche du dernier abonnement en ECHEC...");
            Optional<Abonnement> dernierEchec = getDernierAbonnementEchec(userId);

            if (dernierEchec.isPresent()) {
                logger.info("⚠️ Abonnement ECHEC trouvé - ID: {}", dernierEchec.get().getId());
                paiementConfirme = verifierEtMettreAJourAbonnement(dernierEchec.get());

                if (paiementConfirme) {
                    logger.info("✅ ECHEC converti en SUCCESS ! Abonnement activé.");
                    logger.info("🛑 STOP : Pas besoin de vérifier PENDING");

                    // Construire et retourner immédiatement
                    return construireReponseComplete(userId, true);
                } else {
                    logger.info("❌ ECHEC confirmé ou toujours en attente chez JawuPay");
                }
            } else {
                logger.info("ℹ️ Aucun abonnement en ECHEC trouvé");
            }

            // ============================================
            // ÉTAPE 2 : VÉRIFIER LE DERNIER PENDING
            // (Seulement si ECHEC n'a pas été converti en SUCCESS)
            // ============================================
            logger.info("🔍 ÉTAPE 2 : Recherche du dernier abonnement EN_ATTENTE...");
            Optional<Abonnement> dernierPending = getDernierAbonnementPending(userId);

            if (dernierPending.isPresent()) {
                logger.info("⏳ Abonnement PENDING trouvé - ID: {}", dernierPending.get().getId());
                paiementConfirme = verifierEtMettreAJourAbonnement(dernierPending.get());

                if (paiementConfirme) {
                    logger.info("✅ PENDING converti en SUCCESS ! Abonnement activé.");

                    // Construire et retourner
                    return construireReponseComplete(userId, true);
                } else {
                    logger.info("⏳ PENDING toujours en attente ou échoué");
                }
            } else {
                logger.info("ℹ️ Aucun abonnement EN_ATTENTE trouvé");
            }

            // ============================================
            // ÉTAPE 3 : RETOURNER LES INFOS UTILISATEUR
            // ============================================
            logger.info("📊 ÉTAPE 3 : Construction de la réponse finale");
            return construireReponseComplete(userId, paiementConfirme);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la récupération des infos complètes: ", e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors de la récupération des informations: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    /**
     * Récupère le dernier abonnement en ECHEC pour un utilisateur
     * Trié par date de création décroissante (le plus récent)
     *
     * ✅ @Transactional(readOnly = true) pour optimisation
     */
    @Transactional(readOnly = true)
    private Optional<Abonnement> getDernierAbonnementEchec(Long userId) {
        logger.debug("🔍 Recherche dernier ECHEC pour userId: {}", userId);
        return abonnementRepository
                .findFirstByUserIdAndStatutPaiementOrderByDateCreationDesc(
                        userId,
                        StatutPaiement.ECHEC
                );
    }

    /**
     * Récupère le dernier abonnement en PENDING pour un utilisateur
     * Trié par date de création décroissante (le plus récent)
     *
     * ✅ @Transactional(readOnly = true) pour optimisation
     */
    @Transactional(readOnly = true)
    private Optional<Abonnement> getDernierAbonnementPending(Long userId) {
        logger.debug("🔍 Recherche dernier PENDING pour userId: {}", userId);
        return abonnementRepository
                .findFirstByUserIdAndStatutPaiementOrderByDateCreationDesc(
                        userId,
                        StatutPaiement.EN_ATTENTE
                );
    }

    /**
     * Vérifie et met à jour un abonnement en interrogeant JawuPay
     *
     * @param abonnement L'abonnement à vérifier
     * @return true si le paiement a été confirmé (SUCCESS), false sinon
     *
     * ✅ REQUIRES_NEW : Transaction indépendante pour isolation
     * Si cette transaction échoue, elle n'affectera pas les autres opérations
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean verifierEtMettreAJourAbonnement(Abonnement abonnement) {
        try {
            logger.info("🔍 Vérification abonnement ID: {} (Statut: {})",
                    abonnement.getId(),
                    abonnement.getStatutPaiement());

            // 1. Récupérer la transaction associée
            TransactionsCcapp transaction = transactionCcappRepository
                    .findOneByAbonnementId(abonnement.getId());

            if (transaction == null) {
                logger.warn("⚠️ Aucune transaction trouvée pour abonnement ID: {}",
                        abonnement.getId());
                return false;
            }

            Long jawupayId = transaction.getJawupayId();
            if (jawupayId == null) {
                logger.warn("⚠️ Pas de jawupayId pour transaction ID: {}",
                        transaction.getId());
                return false;
            }

            logger.info("📋 Transaction trouvée - ID: {}, JawupayId: {}, Statut: {}",
                    transaction.getId(),
                    jawupayId,
                    transaction.getStatut());

            // 2. Obtenir le token JawuPay
            String secretJawuPay = obtenirTokenJawuPay();
            if (secretJawuPay == null) {
                logger.error("❌ Impossible d'obtenir le token JawuPay");
                return false;
            }

            logger.debug("✅ Token JawuPay obtenu");

            // 3. Vérifier le statut chez JawuPay
            logger.info("📡 Interrogation JawuPay pour jawupayId: {}", jawupayId);
            CommonResponseWs responseWs = abonnementClient
                    .verifierStatutPaiement(jawupayId, secretJawuPay, CustomJwtUtils.getAccessToken(httpServletRequest));

            if (responseWs == null) {
                logger.warn("⚠️ Réponse nulle de JawuPay pour jawupayId: {}", jawupayId);
                return false;
            }

            if (responseWs.getData() == null) {
                logger.warn("⚠️ Pas de données dans la réponse JawuPay (statut: {})",
                        responseWs.getStatus());
                return false;
            }

            String statutJawuPay = (String) responseWs.getData();
            logger.info("📊 Statut JawuPay pour jawupayId {} : '{}'", jawupayId, statutJawuPay);

            // 4. Traiter selon le statut retourné
            if ("SUCCESS".equals(statutJawuPay)) {
                logger.info("✅ SUCCESS confirmé par JawuPay !");
                logger.info("🔄 Traitement du callback de paiement...");

                try {
                    abonnementService.traiterCallbackPaiement(jawupayId, true);
                    logger.info("════════════════════════════════════════════");
                    logger.info("TRAITEMENT DE CALLABACK");
                    logger.info("════════════════════════════════════════════");
                    logger.info("✅ Callback traité avec succès - Abonnement activé");
                    return true; // ✅ Paiement confirmé et traité

                } catch (Exception e) {
                    logger.error("❌ Erreur lors du traitement du callback", e);
                    // ✅ On propage l'exception pour rollback de cette transaction
                    throw e;
                }

            } else if ("FAILED".equals(statutJawuPay)) {
                logger.info("❌ FAILED confirmé par JawuPay");

                // Si l'abonnement était EN_ATTENTE, le passer en ECHEC
                if (abonnement.getStatutPaiement() == StatutPaiement.EN_ATTENTE) {
                    logger.info("🔄 Conversion PENDING → ECHEC");

                    abonnement.setStatutPaiement(StatutPaiement.ECHEC);
                    abonnement.setActif(false);
                    abonnementRepository.save(abonnement);

                    transaction.setStatut("FAILED");
                    transaction.setDateResponse(new Date());
                    transactionCcappRepository.save(transaction);

                    logger.info("✅ Abonnement et transaction mis à jour en ECHEC");
                }

                return false;

            } else if ("PENDING".equals(statutJawuPay)) {
                logger.info("⏳ Toujours PENDING chez JawuPay - Aucune action");
                return false;

            } else {
                logger.warn("⚠️ Statut JawuPay inconnu: '{}'", statutJawuPay);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la vérification de l'abonnement ID: {}",
                    abonnement.getId(), e);
            // ✅ Propager l'exception pour rollback de cette transaction uniquement
            throw new RuntimeException("Erreur vérification abonnement", e);
        }
    }

    /**
     * Obtient le token JawuPay pour effectuer les vérifications.
     *
     * Nouvelle logique :
     * - On récupère le token directement depuis le header HTTP `jawupay-access`
     * - On n'appelle plus `makeCompletLoginRegisterJawuPay()` ici
     *
     * @return Le token JawuPay ou null si absent/incorrect
     *
     * ✅ Pas de @Transactional - Lecture simple du header HTTP
     */
    private String obtenirTokenJawuPay() {
        String headerToken = httpServletRequest.getHeader("jawupay-access");
        logger.debug("✅ Token JawuPay récupéré depuis le header:{}", headerToken);
        if (headerToken == null || headerToken.trim().isEmpty()) {
            logger.warn("❌ Header 'jawupay-access' absent ou vide, impossible de vérifier JawuPay");
            return null;
        }

        logger.debug("✅ Token JawuPay récupéré depuis le header 'jawupay-access'");
        return headerToken.trim();
    }

    /**
     * Construit la réponse complète avec les infos utilisateur et l'abonnement
     * Met à jour le userStep si un paiement a été confirmé
     *
     * ✅ Pas de @Transactional - Appels HTTP et composition de données
     */
    private CommonResponseWs construireReponseComplete(Long userId, boolean paiementConfirme) {
        try {
            logger.info("🏗️ Construction de la réponse complète...");

            // 1. Récupérer les infos utilisateur
            String token = SecurityUtils.getAccessToken(httpServletRequest);
            CommonResponseWs userInfosResponse = userInfosClient.getUserInfos(userId, token);

            if (userInfosResponse == null || userInfosResponse.getData() == null) {
                logger.error("❌ Impossible de récupérer les infos utilisateur");
                return new CommonResponseWs(
                        WsOperationResponseStatus.error,
                        "Erreur lors de la récupération des informations utilisateur",
                        500,
                        null
                );
            }

            logger.info("✅ Infos utilisateur récupérées");

            // 2. Mettre à jour le userStep si paiement confirmé
            if (paiementConfirme) {
                logger.info("🔄 Mise à jour du userStep suite à la confirmation du paiement");
                updateUserStepToLinkedin(userId, token, userInfosResponse);
            }

            // 3. Récupérer l'abonnement actuel (validé)
            Optional<Abonnement> abonnementActuel = getAbonnementActuel(userId);

            // 4. Construire la map de réponse
            Map<String, Object> completeUserInfo = buildCompleteUserInfo(
                    userInfosResponse.getData(),
                    abonnementActuel
            );

            logger.info("✅ Réponse complète construite avec succès");
            logger.info("=== ✅ Fin getUserInfosWithAbonnement ===");

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Informations utilisateur et abonnement récupérées avec succès",
                    200,
                    completeUserInfo
            );

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la construction de la réponse", e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors de la construction de la réponse: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    /**
     * Récupère l'abonnement actuel (valide) d'un utilisateur
     *
     * ✅ @Transactional(readOnly = true) pour optimisation
     */
    @Transactional(readOnly = true)
    private Optional<Abonnement> getAbonnementActuel(Long userId) {
        try {
            return abonnementService.getAbonnementActuel(userId);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la récupération de l'abonnement actuel", e);
            return Optional.empty();
        }
    }

    /**
     * Met à jour le userStep à LINKEDIN après confirmation du paiement
     *
     * ✅ REQUIRES_NEW avec noRollbackFor pour isolation complète
     * Si cette opération échoue, elle ne doit pas bloquer le reste
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    private void updateUserStepToLinkedin(Long userId, String token, CommonResponseWs userInfosResponse) {
        try {
            Map<String, Object> data = (Map<String, Object>) userInfosResponse.getData();
            Map<String, Object> profile = (Map<String, Object>) data.get("profile");

            if (profile == null) {
                logger.warn("⚠️ Profil absent dans les données utilisateur");
                return;
            }

            String currentUserStep = (String) profile.get("userStep");

            if ("LINKEDIN".equals(currentUserStep)) {
                logger.info("ℹ️ UserStep déjà à LINKEDIN, pas de mise à jour nécessaire");
                return;
            }

            logger.info("🔄 Mise à jour du userStep: {} → LINKEDIN pour userId: {}",
                    currentUserStep, userId);

            UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
            updateRequestWs.setUserStep(StepInSystem.LINKEDIN);
            updateRequestWs.setInSystem(true);

            CommonResponseWs updateResponse = userInfosClient.updateUserAndProfile(
                    updateRequestWs,
                    token
            );

            if (updateResponse != null &&
                    WsOperationResponseStatus.success.equals(updateResponse.getStatus())) {

                // Mettre à jour localement pour la réponse
                profile.put("userStep", "LINKEDIN");
                logger.info("✅ UserStep mis à jour avec succès à LINKEDIN");
            } else {
                logger.warn("⚠️ Échec de la mise à jour du userStep");
            }

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la mise à jour du userStep à LINKEDIN: ", e);
            // ✅ Ne pas propager l'exception grâce à noRollbackFor
        }
    }

    /**
     * Construit la map des informations complètes utilisateur
     *
     * ✅ Pas de @Transactional - Traitement de données en mémoire
     */
//    private Map<String, Object> buildCompleteUserInfo(
//            Object userData,
//            Optional<Abonnement> abonnementOpt) {
//
//        Map<String, Object> completeUserInfo = new HashMap<>();
//        completeUserInfo.put("userInfos", userData);
//
//        if (abonnementOpt.isPresent()) {
//            Abonnement abonnement = abonnementOpt.get();
//            boolean isValide = abonnement.isValide();
//
//            completeUserInfo.put("hasAbonnement", isValide);
//
//            logger.info("📊 Abonnement - Type: {}, Valide: {}, DateFin: {}",
//                    abonnement.getFormuleAbonnement().getTypeAbonnement(),
//                    isValide,
//                    abonnement.getDateFin());
//        } else {
//            completeUserInfo.put("hasAbonnement", false);
//            logger.info("ℹ️ Aucun abonnement actif");
//        }
//
//        return completeUserInfo;
//    }

    private Map<String, Object> buildCompleteUserInfo(
            Object userData,
            Optional<Abonnement> abonnementOpt) {

        Map<String, Object> completeUserInfo = new HashMap<>();
        completeUserInfo.put("userInfos", userData);

        // ============================================
        // CONTRÔLE LINKEDIN : si userStep = LINKEDIN,
        // hasAbonnement est toujours true
        // ============================================
        boolean isLinkedinStep = false;
        try {
            Map<String, Object> data = (Map<String, Object>) userData;
            Map<String, Object> profile = (Map<String, Object>) data.get("profile");
            if (profile != null) {
                String userStep = (String) profile.get("userStep");
                isLinkedinStep = "LINKEDIN".equals(userStep);
                if (isLinkedinStep) {
                    logger.info("✅ userStep = LINKEDIN → hasAbonnement forcé à true");
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ Impossible de lire le userStep depuis userData : {}", e.getMessage());
        }


        if (isLinkedinStep) {
            completeUserInfo.put("hasAbonnement", true);

        } else if (abonnementOpt.isPresent()) {
            Abonnement abonnement = abonnementOpt.get();
            boolean isValide = abonnement.isValide();

            completeUserInfo.put("hasAbonnement", isValide);

            logger.info("📊 Abonnement - Type: {}, Valide: {}, DateFin: {}",
                    abonnement.getFormuleAbonnement().getTypeAbonnement(),
                    isValide,
                    abonnement.getDateFin());
        } else {
            completeUserInfo.put("hasAbonnement", false);
            logger.info("ℹ️ Aucun abonnement actif");
        }

        return completeUserInfo;
    }

    /**
     * Récupère les informations complètes d'un utilisateur spécifique (Admin)
     *
     * ✅ Pas de @Transactional - Composition de données depuis services
     */
    public CommonResponseWs getUserInfosWithAbonnementByUserId(Long userId) {
        try {
            logger.info("=== Récupération infos user {} + abonnement (Admin) ===", userId);

            String token = SecurityUtils.getAccessToken(httpServletRequest);
            CommonResponseWs userInfosResponse = userInfosClient.getUserInfos(userId, token);

            if (userInfosResponse == null || userInfosResponse.getData() == null) {
                logger.error("❌ Impossible de récupérer les infos pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.error,
                        "Erreur lors de la récupération des informations utilisateur",
                        500,
                        null
                );
            }

            Optional<Abonnement> abonnementOpt = getAbonnementActuel(userId);

            Map<String, Object> completeUserInfo = new HashMap<>();
            completeUserInfo.put("userInfos", userInfosResponse.getData());
            completeUserInfo.put("hasAbonnement", abonnementOpt.isPresent());

            if (abonnementOpt.isPresent()) {
                Abonnement abonnement = abonnementOpt.get();
                StatutAbonnementDto abonnementDto = new StatutAbonnementDto(abonnement);
                completeUserInfo.put("abonnement", abonnementDto);
                completeUserInfo.put("abonnementExpireAt", abonnement.getDateFin());

                logger.info("✅ Abonnement trouvé pour userId {} - Type: {}",
                        userId, abonnementDto.getTypeAbonnement());
            } else {
                completeUserInfo.put("abonnement", null);
                completeUserInfo.put("abonnementExpireAt", null);
                logger.info("ℹ️ Aucun abonnement actif pour userId: {}", userId);
            }

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Informations utilisateur et abonnement récupérées avec succès",
                    200,
                    completeUserInfo
            );

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la récupération des infos pour userId {}: ", userId, e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors de la récupération des informations: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    // ============================================
    // AUTRES MÉTHODES (Inchangées)
    // ============================================

    /**
     * Récupère le terminal d'un utilisateur
     * ✅ Pas de @Transactional - Appel HTTP externe
     */
    public CommonResponseWs getUserTerminal(Long id) {
        logger.info("Getting user terminal with id {}", id);
        String token = httpServletRequest.getHeader("access-token");
        CommonResponseWs response = userInfosClient.getUserTerminal(id, token);
        if (response == null) {
            return new CommonResponseWs(WsOperationResponseStatus.error,
                    "Erreur lors de la récupération du terminal",
                    500, null);
        }
        return new CommonResponseWs(response.getStatus(), response.getMessage(),
                response.getCode(), response.getData());
    }

    /**
     * Récupère les informations basiques de l'utilisateur connecté
     * ✅ Pas de @Transactional - Appel HTTP externe
     */
    public CommonResponseWs getUserInfos() {
        Long id = CustomJwtUtils.getCurrentUserId();
        logger.info("Getting user infos with id {}", id);
        String token = httpServletRequest.getHeader("access-token");
        CommonResponseWs response = userInfosClient.getUserInfos(id, token);
        if (response == null) {
            return new CommonResponseWs(WsOperationResponseStatus.error,
                    "Erreur lors de la récupération des infos utilisateur",
                    500, null);
        }
        return new CommonResponseWs(response.getStatus(), response.getMessage(),
                response.getCode(), response.getData());
    }

    /**
     * Récupère la session d'un utilisateur
     * ✅ Pas de @Transactional - Appel HTTP externe
     */
    public CommonResponseWs getUserSession(Long id) {
        logger.info("Getting user session with id {}", id);
        String token = httpServletRequest.getHeader("access-token");
        CommonResponseWs response = userInfosClient.getUserSession(id, token);
        if (response == null) {
            return new CommonResponseWs(WsOperationResponseStatus.error,
                    "Erreur lors de la récupération de la session",
                    500, null);
        }
        return new CommonResponseWs(response.getStatus(), response.getMessage(),
                response.getCode(), response.getData());
    }

    /**
     * Extrait les informations du terminal de l'utilisateur connecté
     * ✅ Pas de @Transactional - Manipulation de données HTTP
     */
    public CommonResponseWs extracteTerminal() {
        Long userId = CustomJwtUtils.getCurrentUser().getId();
        String token = httpServletRequest.getHeader("access-token");

        CommonResponseWs terminalData = userInfosClient.getUserTerminal(userId, token);

        if (terminalData == null || terminalData.getData() == null) {
            return new CommonResponseWs(
                    terminalData != null ? terminalData.getStatus() : WsOperationResponseStatus.failed,
                    terminalData != null ? terminalData.getMessage() : "Aucune donnée",
                    terminalData != null ? terminalData.getCode() : 500,
                    null
            );
        }

        Map<String, Object> dataMap = (Map<String, Object>) terminalData.getData();
        Map<String, Object> terminalMap = (Map<String, Object>) dataMap.get("terminal");

        if (terminalMap == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed,
                    "Terminal introuvable", 404, null);
        }

        Map<String, Object> completeTerminal = new HashMap<>();
        completeTerminal.put("operator", terminalMap.get("operator"));
        completeTerminal.put("model", terminalMap.get("model"));
        completeTerminal.put("application", terminalMap.get("application"));
        completeTerminal.put("version", terminalMap.get("version"));
        completeTerminal.put("identifier", terminalMap.get("identifier"));

        return new CommonResponseWs(WsOperationResponseStatus.success,
                "Terminal extrait avec succès", 200, completeTerminal);
    }

    /**
     * Extrait le token du terminal de l'utilisateur connecté
     * ✅ Pas de @Transactional - Manipulation de données HTTP
     */
    public CommonResponseWs extracteToken() {
        Long userId = CustomJwtUtils.getCurrentUser().getId();
        String token = httpServletRequest.getHeader("access-token");
        CommonResponseWs terminalData = userInfosClient.getUserTerminal(userId, token);

        if (terminalData == null || terminalData.getData() == null) {
            return new CommonResponseWs(
                    terminalData != null ? terminalData.getStatus() : WsOperationResponseStatus.failed,
                    terminalData != null ? terminalData.getMessage() : "Aucune donnée",
                    terminalData != null ? terminalData.getCode() : 500,
                    null
            );
        }

        Map<String, Object> dataMap = (Map<String, Object>) terminalData.getData();
        if (dataMap == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed,
                    "Token introuvable", 404, null);
        }

        String tokenDevice = dataMap.get("token").toString();
        return new CommonResponseWs(WsOperationResponseStatus.success,
                "Token extrait avec succès", 200, tokenDevice);
    }
}