package com.clubchretien.abonnement.abonnement.services;


import com.clubchretien.abonnement.abonnement.dto.FilleulAbonnementExpirantDto;
import com.clubchretien.abonnement.abonnement.services.v2.PaiementV2Service;
import com.clubchretien.abonnement.abonnement.services.v2.ParrainageService;
import com.owomi.commons.enums.RequestStatus;
import com.owomi.commons.ws.v2.PartnerPaymentInitializerWs;
import com.owomi.database.entities.ArchiveParent;
import com.owomi.database.entities.enums.*;
import com.owomi.database.entities.enums.TypeOperation;
import com.owomi.database.repositories.ArchiveParentRepository;
import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.configs.JwtConfig;
import com.google.gson.Gson;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.enums.authentication.AuthenticationProviderEnums;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.authentication.*;
import com.owomi.commons.ws.jawupay.CreatePaymentWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.UserRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.UserTerminalRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.AbonnementClient;
import com.owomi.rest.client.abonnementClient.AuthenticationRestClient;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.auth.UserDataClient;
import com.owomi.rest.client.commissionService.CommissionServiceClient;

import com.owomi.rest.client.mobileService.MobileAuthenticationServiceClient;
import com.owomi.rest.client.mobileService.MobileServiceClient;
import com.owomi.rest.client.notif.NotificationUtils;
import com.owomi.rest.client.partnerAdminClient.PaiementV2ClientService;
import com.owomi.rest.client.partnerAdminClient.PartnerAdminClientService;
import com.sun.xml.bind.v2.TODO;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.owomi.commons.constants.MessagesConstants.*;

@Slf4j
@Service
@Transactional
public class AbonnementService {

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    ResponseStatusUtils responseStatusUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormuleAbonnementService formuleAbonnementService;

    @Autowired
    public GestionNiveauxService gestionNiveauxService;

    @Autowired
    private AbonnementClient abonnementClient;

    @Autowired
    private CommissionServiceClient commissionServiceClient;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private LogisticService logisticService;

    @Autowired
    private UserTerminalRepository userTerminalRepository;

    @Autowired
    private UserInfosClient userInfosClient;

    private final UserDataClient userDataClient;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CustomJwtUtils customJwtUtils;

    @Autowired
    AuthenticationRestClient authenticationRestClient;
    @Autowired
    ArchiveParentRepository archiveParentRepository;

    @Autowired
    TransactionCcappRepository transactionCcappRepository;

    @Autowired
    private FirebaseProviderService firebaseProviderService;

    @Autowired
    NotificationUtils notificationUtils;

    @Autowired
    MobileAuthenticationServiceClient mobileAuthenticationServiceClient;

    @Autowired
    PartenaireRepository partenaireRepository;

    @Autowired
    PaiementV2ClientService paiementV2ClientService;

    @Autowired
    PartnerAdminClientService partnerAdminClientService;

    @Autowired
    PaiementV2Service paiementV2Service;

    @Autowired
    private ParrainageService parrainageService;


    @Value("${clubchretien.prefix}")
    String prefice;
    @Value("${clubchretien.appname}")
    String appName;
    @Value("${clubchretien.appidentifier}")
    String appIdentifier;
    @Value("${partner.api.key}")
    private String PATNER_API_KEY;

    @Autowired
    private ReglementDeCompteService reglementDeCompteService;

    @Autowired
    Mapper mapper;

    @Autowired
    MobileServiceClient mobileServiceClient;

    private final AuthenticationRestClient authRestClient;

    private static Logger logger = LogManager.getLogger(AbonnementService.class);

    public AbonnementService(UserDataClient userDataClient, AuthenticationRestClient authRestClient) {
        this.userDataClient = userDataClient;
        this.authRestClient = authRestClient;
    }

    /**
     *
     * @param
     * @return lastTerminal
     */
    public Map<String, Object> getTerminalInfo() {

        logger.info("Nous somme dans la fonction getTerminalInfo");
        // 1. Récupérer le user du JWT
        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
        logger.info("Voici le User courrant: {}", userProfile);
        logger.info("Voici l'id User courrant: {}", userProfile.getId());
        User userProfile1 = userRepository.findById(userProfile.getId()).orElse(null);

        logger.info("Voici le User: {}", userProfile1);

        Long userId = userProfile.getId();
        System.out.println("UserId du JWT: " + userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            System.out.println(" User non trouvé en BDD pour ID: " + userId);
            return Map.of("error", "User non trouvé");
        }
        System.out.println(" User trouvé: " + user.getEmail());

        // 3. Chercher le dernier UserTerminal
        UserTerminal lastTerminal = userTerminalRepository
                .findTopByUserOrderByConnexionDateDesc(user);

        if (lastTerminal == null) {
            System.out.println(" Aucun UserTerminal trouvé pour userId: " + userId);

            // Debug: Combien de UserTerminal existent pour cet user ?
            List<UserTerminal> allUserTerminals = Collections.singletonList(userTerminalRepository.findOneById(userId));
            System.out.println("📊 Total UserTerminals pour ce user: " + allUserTerminals.size());

            if (!allUserTerminals.isEmpty()) {
                System.out.println(" Des UserTerminals existent mais la requête échoue");
                allUserTerminals.forEach(
                        ut -> System.out.println("  - ID: " + ut.getId() + ", Date: " + ut.getConnexionDate()));
            }

            return Map.of("error", "Aucun terminal enregistré");
        }

        System.out.println(" UserTerminal trouvé: " + lastTerminal.getId()
                + " (terminal: " + lastTerminal.getTerminal().getIdentifiant() + ")");

        return CustomJwtUtils.getCurrentTerminalInfo(lastTerminal);
    }

    public String getFirebaseProviderIdFromFullMap(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject jsonObject = new JSONObject(payload);

            JSONObject firebaseFullMap = jsonObject.getJSONObject("firebaseFullMap");
            JSONObject claims = firebaseFullMap.getJSONObject("claims");
            JSONObject session = claims.getJSONObject("session");
            JSONObject user = session.getJSONObject("user");

            String firebaseProviderId = user.getString("firebaseProviderId");

            logger.info("Le firebaseProviderId extraite ====>: " + firebaseProviderId);

            return firebaseProviderId;

        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction via firebaseFullMap: ", e);
            return null;
        }
    }

    public CommonResponseWs makeCompletLoginRegisterJawuPay() {
        logger.info("=== Début makeCompletLoginRegisterJawuPay ===");

        CommonResponseWs responseWs = logisticService.extracteTerminal();
        if (!responseWs.getStatus().equals(WsOperationResponseStatus.success))
            return responseWs;
        logger.info("Terminal récupéré: {}", responseWs.getData());
        Map<String, Object> data = (Map<String, Object>) responseWs.getData();

        Long userId = CustomJwtUtils.getCurrentUserId();

        String identifier = (String) data.get("identifier");
        String model = (String) data.get("model");
        String version = (String) data.get("version");
        String operator = (String) data.get("operator");
        String token = httpServletRequest.getHeader("access-token");
        String fbProviderId = firebaseProviderService.getOrCreateFirebaseProviderId(userId);
        logger.info("FirebaseProviderId pour userId {}: {}", userId, fbProviderId);
        CommonResponseWs deviceAuthRespnse = userDataClient.deviceAuthJawuPay(identifier, operator, model, version);
        if (!deviceAuthRespnse.getStatus().equals(WsOperationResponseStatus.success))
            return deviceAuthRespnse;
        logger.debug("La réponse du device  ====>: {}", deviceAuthRespnse.getData());
        CommonResponseWs rs = userInfosClient.getPhoneInfos(token);
        if (!rs.getStatus().equals(WsOperationResponseStatus.success))
            return rs;
        logger.debug("La réponse de userinfos phone  ====>: {}", rs.getData());
        CommonResponseWs userData = userInfosClient.getUserInfos(userId, token);
        if (!userData.getStatus().equals(WsOperationResponseStatus.success))
            return userData;
        logger.debug("La réponse de userInfos ====>: {}", userData.getData());
        Map<String, Object> u = (Map<String, Object>) userData.getData();
        String emailTwo = (String) u.get("email");
        Map<String, Object> userInfo = (Map<String, Object>) rs.getData();
        String phoneNumber = (String) userInfo.get("phoneNumber");
        String phoneCodeNumber = (String) userInfo.get("phoneCodeNumber");
        String phoneFullNumber = (String) userInfo.get("phoneFullNumber");

        if (phoneCodeNumber == null && phoneFullNumber == null && phoneNumber == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Le user n'a pas de num", 500, null);
        } else {

            CommonResponseWs commonResponseWs = deviceAuthRespnse;

            logger.info("La réponse de device:====>{}", commonResponseWs);
            Gson gson = new Gson();

            SessionInfoWs emailSessionInfoWs = gson.fromJson(
                    gson.toJson(commonResponseWs.getData()),
                    SessionInfoWs.class);
            logger.info("Après conversion :====>{}", emailSessionInfoWs);

            String tokenDevice = emailSessionInfoWs.getToken();
            String firebaseProviderId = customJwtUtils.getUid(token);
            String tokenTerminal = emailSessionInfoWs.getTerminal().getToken();
            firebaseProviderService.updateContextToken(userId, firebaseProviderId);

            FirebasePhoneInputWs inputWs = FirebasePhoneInputWs.input()
                    .phoneFullNumber(prefice + phoneFullNumber)
                    .phoneCodeNumber(phoneCodeNumber)
                    .phoneNumber(prefice + phoneNumber)
                    .typeProvider(AuthenticationProviderEnums.FIREBASE)
                    .build();

            logger.debug("authenticationByEmail emailSessionInfoWs {} ", emailSessionInfoWs);

            SessionClaimsWs sessionAfterEmail = new SessionClaimsWs();
            sessionAfterEmail.setBlocked(false);
            sessionAfterEmail.setType(emailSessionInfoWs.getType());
            sessionAfterEmail.setToken(emailSessionInfoWs.getToken());
            sessionAfterEmail.setTerminal(emailSessionInfoWs.getTerminal());
            sessionAfterEmail.setUser(emailSessionInfoWs.getUser());
            sessionAfterEmail
                    .setUserTerminal(new SessionUserTerminalWs(sessionAfterEmail.getUser().getFirebaseProviderId()));

            logger.info("Le SessionClaimsWs: {}", sessionAfterEmail);

            ClientCcPhoneInputWs clientInput = ClientCcPhoneInputWs.input()
                    .inputWs(inputWs)
                    .sessionClaimsWs(sessionAfterEmail)
                    .token(tokenDevice)
                    .build();

            logger.info("Le ClientCCPhoneInputWs: {}", clientInput);

            CommonResponseWs response = userDataClient.providerPhoneTokenAuthJawuPay(
                    clientInput, tokenTerminal, emailTwo, fbProviderId);

            logger.info("Réponse providerPhoneTokenAuthJawuPay: {}", response);

            if (!response.getStatus().equals(WsOperationResponseStatus.success))
                return response;

            FireBaseUpdateInfoWs fireBaseUpdateInfoWs = new FireBaseUpdateInfoWs();
            fireBaseUpdateInfoWs.setAppName(appName);
            fireBaseUpdateInfoWs.setAppIdentifier(appIdentifier);
            fireBaseUpdateInfoWs.setFirebaseProviderId(firebaseProviderId);
            fireBaseUpdateInfoWs.setGeneretedFirebaseProviderId(fbProviderId);

            CommonResponseWs firebaseResponse = userDataClient.fireBaseUpdateInfos(fireBaseUpdateInfoWs);

            if (!firebaseResponse.getStatus().equals(WsOperationResponseStatus.success))
                return firebaseResponse;

            Map<String, Object> phoneResponse = (Map<String, Object>) response.getData();
            String jawupayToken = (String) phoneResponse.get("idToken");

            firebaseProviderService.updateRefreshToken(userId, jawupayToken,
                    phoneResponse.get("refreshToken").toString());

            logger.info("idToken récupéré avec succès, longueur: {}", jawupayToken.length());

            CommonResponseWs loginResponse = userDataClient.loginJawuPay(jawupayToken);
            if (!loginResponse.getStatus().equals(WsOperationResponseStatus.success))
                return loginResponse;
            // String finalToken = (String) phoneResponse.get("accessToken");
            // logger.info("Token jawupay récupéré: {}", finalToken);
            //
            // CommonResponseWs stat = authenticationRestClient.checkUserState(finalToken);
            // if (!stat.getStatus().equals(WsOperationResponseStatus.success)) return stat;
            logger.info("=== Fin makeCompletLoginRegisterJawuPay avec succès ===");
            return loginResponse;

        }
    }

    public CommonResponseWs getJawuPayToken(String email, String displayname) {
        UserProfileWs userProfileWs = CustomJwtUtils.getCurrentUser();
        logger.info("Verification du profil ====>", userProfileWs);

        firebaseProviderService.getRefreshToken(userProfileWs.getId());

        CommonResponseWs responseWs = authRestClient.verifyUser(email);

        if (responseWs == null) {
            logger.info("Verification du mail est bon ====>", responseWs.getData());
            return new CommonResponseWs(responseWs.getStatus(), responseWs.getMessage(), responseWs.getCode(),
                    responseWs.getData());
        }

        CommonResponseWs response = authRestClient.createUser(email, displayname);
        if (response == null) {
            logger.info("Erreru lors de la creation userfake  ====>", response.getData());
            return new CommonResponseWs(response.getStatus(), response.getMessage(), response.getCode(),
                    response.getData());
        }
        return new CommonResponseWs(response.getStatus(), response.getMessage(), response.getCode(),
                response.getData());

    }

    /**
     * Rafraîchit le token Jawupay pour un utilisateur
     * Délègue au FirebaseProviderService qui gère la logique de refresh avec
     * fallback Firebase
     *
     * @param userId ID de l'utilisateur
     * @return CommonResponseWs contenant le nouveau token Jawupay
     */
    public CommonResponseWs refreshJawupayToken(Long userId) {
        logger.info("=== AbonnementService: Demande de refresh token pour userId: {} ===", userId);
        return firebaseProviderService.refreshJawupayToken(userId);
    }

    /**
     * Calcule de duree;
     */
    public int calculDuree(Abonnement abonnement) {
        if (abonnement.getFormuleAbonnement().getDuree() == Duree.UN_MOIS) {
            return 1;
        } else if (abonnement.getFormuleAbonnement().getDuree() == Duree.TROIS_MOIS) {
            return 3;
        } else if (abonnement.getFormuleAbonnement().getDuree() == Duree.DOUZE_MOIS) {
            return 12;
        } else {
            return 1;
        }
    }

    // ============================================================================
    // REFACTORING DE LA MÉTHODE initializer()
    // - Séparation en fonctions distinctes pour chaque cas
    // - Gestion d'exceptions robuste avec try-catch
    // - Logs détaillés à chaque retour
    // - Messages clairs pour l'utilisateur
    //
    // ============================================================================

    /**
     * Point d'entrée principal pour l'initialisation d'un abonnement
     *
     * @param numeroCCParent Numéro du parrain (optionnel)
     * @param formuleId      ID de la formule d'abonnement
     * @return CommonResponseWs avec les détails de l'initialisation
     */
    public CommonResponseWs initializer(Optional<String> numeroCCParent, Long formuleId) {
        Long userId = CustomJwtUtils.getCurrentUserId();
        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();

        logger.info("═══════════════════════════════════════════════════════════════════");
        logger.info("DÉBUT INITIALISATION - UserId: {}, FormuleId: {}, Parrain: {}",
                userId, formuleId, numeroCCParent.orElse("AUCUN"));
        logger.info("═══════════════════════════════════════════════════════════════════");

        try {
            // 1. Validation du terminal
            CommonResponseWs terminalResponse = validerTerminal();
            if (!terminalResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.error("❌ RETOUR: Échec validation terminal");
                logger.error("   Status: {}, Message: {}", terminalResponse.getStatus(), terminalResponse.getMessage());
                return terminalResponse;
            }

            // 2. Validation du token JawuPay
            String jawupayAccess = httpServletRequest.getHeader("jawupay-access");
            CommonResponseWs tokenValidation = validerTokenJawuPay(jawupayAccess);
            if (!tokenValidation.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.error("❌ RETOUR: Token JawuPay invalide");
                logger.error("   Status: {}, Message: {}", tokenValidation.getStatus(), tokenValidation.getMessage());
                return tokenValidation;
            }

            // 3. Vérification des abonnements en attente
            String telParrain = numeroCCParent.orElse("");
            CommonResponseWs verificationEnAttente = verifierAbonnementEnAttente(
                    userId, formuleId, telParrain, jawupayAccess);

            // Si un abonnement en attente a été détecté et traité
            if (verificationEnAttente != null) {
                logger.info("✅ RETOUR: Abonnement en attente détecté et traité");
                logger.info("   Status: {}, Message: {}",
                        verificationEnAttente.getStatus(), verificationEnAttente.getMessage());
                logger.info("   Data: {}", verificationEnAttente.getData());
                return verificationEnAttente;
            }

            // 4. Aucun blocage détecté - Procéder à la nouvelle initialisation
            logger.info("✅ Aucun blocage - Nouvelle initialisation autorisée");
            CommonResponseWs nouvelleInitialisation = procederNouvelleInitialisation(
                    numeroCCParent, userId, formuleId, userProfile, jawupayAccess);

            logger.info("✅ RETOUR: Nouvelle initialisation réussie");
            logger.info("   Status: {}, Message: {}",
                    nouvelleInitialisation.getStatus(), nouvelleInitialisation.getMessage());
            logger.info("   Data: {}", nouvelleInitialisation.getData());

            return nouvelleInitialisation;

        } catch (IllegalArgumentException e) {
            logger.error("❌ RETOUR: Erreur de validation métier");
            logger.error("   Message: {}", e.getMessage(), e);

            CommonResponseWs errorResponse = new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    e.getMessage(),
                    400,
                    null);
            logger.error("   Response: {}", errorResponse);
            return errorResponse;

        } catch (Exception e) {
            logger.error("❌ RETOUR: Erreur technique inattendue");
            logger.error("   Exception: {}", e.getMessage(), e);

            CommonResponseWs errorResponse = new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors du traitement de votre demande. Veuillez réessayer.",
                    500,
                    null);
            logger.error("   Response: {}", errorResponse);
            return errorResponse;

        } finally {
            logger.info("═══════════════════════════════════════════════════════════════════");
            logger.info("FIN INITIALISATION - UserId: {}", userId);
            logger.info("═══════════════════════════════════════════════════════════════════");
        }
    }

    public CommonResponseWs initializerV2(Optional<String> numeroCCParent, Long formuleId) {
        Long userId = customJwtUtils.getCurrentUserId();
        UserProfileWs userProfile = customJwtUtils.getCurrentUser();

        logger.info("═══════════════════════════════════════════════════════════════════");
        logger.info("DÉBUT INITIALISATION - UserId: {}, FormuleId: {}, Parrain: {}",
                userId, formuleId, numeroCCParent.orElse("AUCUN"));
        logger.info("═══════════════════════════════════════════════════════════════════");

        logger.info("userId : {}, userProfil : {}, userStep : {}", userId, userProfile,
                userProfile.getUserStep().getValue());

        try {
            // 1. Validation du terminal
            /*
             * CommonResponseWs terminalResponse = validerTerminal();
             * if (!terminalResponse.getStatus().equals(WsOperationResponseStatus.success))
             * {
             * logger.error("❌ RETOUR: Échec validation terminal");
             * logger.error("   Status: {}, Message: {}", terminalResponse.getStatus(),
             * terminalResponse.getMessage());
             * return terminalResponse;
             * }
             */

            // 2. Validation du token JawuPay
            /*
             * String jawupayAccess = httpServletRequest.getHeader("jawupay-access");
             * CommonResponseWs tokenValidation = validerTokenJawuPay(jawupayAccess);
             * if (!tokenValidation.getStatus().equals(WsOperationResponseStatus.success)) {
             * logger.error("❌ RETOUR: Token JawuPay invalide");
             * logger.error("   Status: {}, Message: {}", tokenValidation.getStatus(),
             * tokenValidation.getMessage());
             * return tokenValidation;
             * }
             */

            // Vérifier si le statut du gars est vide
            // - si non retourner impossible
            // - si oui continuer
            StepInSystem userStep = userProfile.getUserStep();
            if (userStep != StepInSystem.INITIAL && userStep != StepInSystem.PAIEMENT) return new CommonResponseWs(WsOperationResponseStatus.failed, "étape dépassée", 400, null);

            // 3. Vérification des abonnements en attente
            String telParrain = numeroCCParent.orElse("");
            String accessToken = httpServletRequest.getHeader("access-token");
            CommonResponseWs verificationEnAttente = verifierAbonnementEnAttenteV2(
                    userId, formuleId, telParrain, accessToken);

            // Si un abonnement en attente a été détecté et traité
            if (verificationEnAttente != null) {
                logger.info("✅ RETOUR: Abonnement en attente détecté et traité");
                logger.info("   Status: {}, Message: {}",
                        verificationEnAttente.getStatus(), verificationEnAttente.getMessage());
                logger.info("   Data: {}", verificationEnAttente.getData());
                return verificationEnAttente;
            }

            // 4. Aucun blocage détecté - Procéder à la nouvelle initialisation
            logger.info("✅ Aucun blocage - Nouvelle initialisation autorisée");
            CommonResponseWs nouvelleInitialisation = procederNouvelleInitialisationV2(
                    numeroCCParent, userId, formuleId, userProfile);

            logger.info("✅ RETOUR: Nouvelle initialisation réussie");
            logger.info("   Status: {}, Message: {}",
                    nouvelleInitialisation.getStatus(), nouvelleInitialisation.getMessage());
            logger.info("   Data: {}", nouvelleInitialisation.getData());

            return nouvelleInitialisation;

        } catch (IllegalArgumentException e) {
            logger.error("❌ RETOUR: Erreur de validation métier");
            logger.error("   Message: {}", e.getMessage(), e);

            CommonResponseWs errorResponse = new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    e.getMessage(),
                    400,
                    null);
            logger.error("   Response: {}", errorResponse);
            return errorResponse;

        } catch (Exception e) {
            logger.error("❌ RETOUR: Erreur technique inattendue");
            logger.error("   Exception: {}", e.getMessage(), e);

            CommonResponseWs errorResponse = new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors du traitement de votre demande. Veuillez réessayer.",
                    500,
                    null);
            logger.error("   Response: {}", errorResponse);
            return errorResponse;

        } finally {
            logger.info("═══════════════════════════════════════════════════════════════════");
            logger.info("FIN INITIALISATION - UserId: {}", userId);
            logger.info("═══════════════════════════════════════════════════════════════════");
        }
    }

    // ============================================================================
    // FONCTION 1 : Validation du terminal
    // ============================================================================

    /**
     * Valide que l'utilisateur possède un terminal enregistré
     *
     * @return CommonResponseWs avec succès ou erreur
     */
    private CommonResponseWs validerTerminal() {
        logger.info("→ Validation du terminal utilisateur");

        try {
            CommonResponseWs responseWs = logisticService.extracteTerminal();

            if (responseWs == null) {
                logger.error("  ✗ extracteTerminal() a retourné null");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Erreur lors de la récupération des informations du terminal",
                        500,
                        null);
            }

            if (!responseWs.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.error("  ✗ Terminal non trouvé ou invalide");
                logger.error("    Status: {}, Message: {}", responseWs.getStatus(), responseWs.getMessage());
                return responseWs;
            }

            logger.info("  ✓ Terminal validé: {}", responseWs.getData());
            return responseWs;

        } catch (Exception e) {
            logger.error("  ✗ Exception lors de la validation du terminal: {}", e.getMessage(), e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur technique lors de la validation du terminal",
                    500,
                    null);
        }
    }

    // ============================================================================
    // FONCTION 2 : Validation du token JawuPay
    // ============================================================================

    /**
     * Valide la présence et le format du token JawuPay
     *
     * @param jawupayAccess Token JawuPay depuis les headers
     * @return CommonResponseWs avec succès ou erreur
     */
    private CommonResponseWs validerTokenJawuPay(String jawupayAccess) {
        logger.info("→ Validation du token JawuPay");

        try {
            if (jawupayAccess == null || jawupayAccess.trim().isEmpty()) {
                logger.error("  ✗ Token JawuPay absent dans les headers");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Token d'authentification JawuPay manquant. Veuillez vous reconnecter.",
                        401,
                        null);
            }

            // Validation basique du format JWT
            if (!jawupayAccess.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")) {
                logger.error("  ✗ Format du token JawuPay invalide");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Token d'authentification JawuPay invalide. Veuillez vous reconnecter.",
                        401,
                        null);
            }

            logger.info("  ✓ Token JawuPay validé (longueur: {} caractères)", jawupayAccess.length());
            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Token valide",
                    200,
                    null);

        } catch (Exception e) {
            logger.error("  ✗ Exception lors de la validation du token: {}", e.getMessage(), e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors de la validation du token",
                    500,
                    null);
        }
    }

    // ============================================================================
    // FONCTION 3 : Vérification des abonnements en attente
    // ============================================================================

    /**
     * Vérifie s'il existe un abonnement EN_ATTENTE et décide de la marche à suivre
     *
     * @param userId        ID de l'utilisateur
     * @param formuleId     ID de la formule souhaitée
     * @param telParrain    Numéro du parrain saisi
     * @param jawupayAccess Token JawuPay
     * @return CommonResponseWs si blocage/renvoi données, null si continuer
     */
    private CommonResponseWs verifierAbonnementEnAttente(
            Long userId, Long formuleId, String telParrain, String jawupayAccess) {

        logger.info("→ Vérification des abonnements EN_ATTENTE pour userId: {}", userId);

        try {
            Optional<Abonnement> abonnementEnAttente = abonnementRepository
                    .findFirstByUserIdAndStatutPaiementOrderByDateCreationDesc(
                            userId,
                            StatutPaiement.EN_ATTENTE);

            if (abonnementEnAttente.isEmpty()) {
                logger.info("  ✓ Aucun abonnement EN_ATTENTE trouvé");
                return null; // Continuer la nouvelle initialisation
            }

            Abonnement abonnement = abonnementEnAttente.get();
            logger.info("  ⚠ Abonnement EN_ATTENTE détecté - ID: {}, FormuleId: {}, DateCreation: {}",
                    abonnement.getId(),
                    abonnement.getFormuleAbonnement().getId(),
                    abonnement.getDateCreation());

            // Cas 1 : Même parrain
            CommonResponseWs memeParrainResponse = verifierMemeParrain(
                    abonnement, telParrain, jawupayAccess);
            if (memeParrainResponse != null) {
                return memeParrainResponse;
            }

            // Cas 2 : Même formule
            CommonResponseWs memeFormuleResponse = verifierMemeFormule(
                    abonnement, formuleId, userId, jawupayAccess);
            if (memeFormuleResponse != null) {
                return memeFormuleResponse;
            }

            // Cas 3 : Formule différente - Nettoyage nécessaire
            logger.info("  → Formule différente détectée - Nettoyage des transactions en attente");
            nettoyerTransactionsEnAttente(userId, jawupayAccess);

            logger.info("  ✓ Nettoyage terminé - Nouvelle initialisation autorisée");
            return null; // Continuer

        } catch (Exception e) {
            logger.error("  ✗ Exception lors de la vérification des abonnements EN_ATTENTE: {}",
                    e.getMessage(), e);

            // En cas d'erreur, on laisse continuer mais on log l'erreur
            logger.warn("  ⚠ Continuation malgré l'erreur (sécurité)");
            return null;
        }
    }

    private CommonResponseWs verifierAbonnementEnAttenteV2(
            Long userId, Long formuleId, String telParrain, String accessToken) {

        logger.info("→ Vérification des abonnements EN_ATTENTE pour userId: {}", userId);

        try {
            Optional<Abonnement> abonnementEnAttente = abonnementRepository
                    .findFirstByUserIdAndStatutPaiementOrderByDateCreationDesc(
                            userId,
                            StatutPaiement.EN_ATTENTE);

            if (abonnementEnAttente.isEmpty()) {
                logger.info("  ✓ Aucun abonnement EN_ATTENTE trouvé");
                return null; // Continuer la nouvelle initialisation
            }

            Abonnement abonnement = abonnementEnAttente.get();
            logger.info("  ⚠ Abonnement EN_ATTENTE détecté - ID: {}, FormuleId: {}, DateCreation: {}",
                    abonnement.getId(),
                    abonnement.getFormuleAbonnement().getId(),
                    abonnement.getDateCreation());

            // Cas 1 : Même parrain
            /*
             * CommonResponseWs memeParrainResponse = verifierMemeParrain(
             * abonnement, telParrain, jawupayAccess);
             * if (memeParrainResponse != null) {
             * return memeParrainResponse;
             * }
             */

            // Cas 2 : Même formule
            /*
             * CommonResponseWs memeFormuleResponse = verifierMemeFormule(
             * abonnement, formuleId, userId, jawupayAccess);
             * if (memeFormuleResponse != null) {
             * return memeFormuleResponse;
             * }
             */

            // Cas 3 : Formule différente - Nettoyage nécessaire
            logger.info("  → Formule différente détectée - Nettoyage des transactions en attente");
            nettoyerTransactionsEnAttenteV2(userId, accessToken);

            logger.info("  ✓ Nettoyage terminé - Nouvelle initialisation autorisée");
            return null; // Continuer

        } catch (Exception e) {
            logger.error("  ✗ Exception lors de la vérification des abonnements EN_ATTENTE: {}",
                    e.getMessage(), e);

            // En cas d'erreur, on laisse continuer mais on log l'erreur
            logger.warn("  ⚠ Continuation malgré l'erreur (sécurité)");
            return null;
        }
    }

    // ============================================================================
    // FONCTION 4 : Vérification du même parrain
    // ============================================================================

    /**
     * Vérifie si c'est le même parrain et gère la réponse appropriée
     *
     * @param abonnement      Abonnement EN_ATTENTE trouvé
     * @param telParrainSaisi Numéro du parrain saisi par l'utilisateur
     * @param jawupayAccess   Token JawuPay
     * @return CommonResponseWs si même parrain, null sinon
     */
    private CommonResponseWs verifierMemeParrain(
            Abonnement abonnement, String telParrainSaisi, String jawupayAccess) {

        logger.info("  → Vérification si même parrain");

        try {
            // Récupération du parrain existant
            ArchiveParent archiveParentExistant = archiveParentRepository
                    .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());

            String parrainExistant = "";
            if (archiveParentExistant != null && archiveParentExistant.getNumeroCCParent() != null) {
                parrainExistant = archiveParentExistant.getNumeroCCParent();
            }

            String nouveauParrain = (telParrainSaisi != null) ? telParrainSaisi : "";

            logger.info("    Parrain existant: '{}', Nouveau parrain: '{}'",
                    parrainExistant, nouveauParrain);

            boolean memeParrain = parrainExistant.equals(nouveauParrain);

            if (!memeParrain) {
                logger.info("    ✓ Parrain différent - Poursuite de la vérification");
                return null; // Pas le même parrain, continuer
            }

            logger.info("    ⚠ MÊME PARRAIN détecté - Vérification du statut du paiement");

            // Récupération de la transaction
            TransactionsCcapp transactionExistante = transactionCcappRepository
                    .findOneByAbonnementId(abonnement.getId());

            if (transactionExistante == null) {
                logger.warn("    ✗ Aucune transaction trouvée pour cet abonnement");
                return renvoyerDonneesExistantesSansTransaction(abonnement, parrainExistant);
            }

            String statutTransaction = transactionExistante.getStatut();
            logger.info("    Transaction trouvée - ID: {}, Statut: {}, JawupayId: {}",
                    transactionExistante.getId(),
                    statutTransaction,
                    transactionExistante.getJawupayId());

            // Vérifier si transaction vraiment en attente
            if (statutTransaction == null ||
                    (!statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) &&
                            !statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {

                logger.info("    ✓ Transaction déjà terminée (statut: {}) - Autoriser nouvelle initialisation",
                        statutTransaction);
                return null; // Transaction terminée, continuer
            }

            // Transaction en attente - Vérifier auprès de JawuPay
            Long jawupayId = transactionExistante.getJawupayId();
            if (jawupayId == null) {
                logger.warn("    ✗ Pas de jawupayId - Renvoi des données existantes");
                return renvoyerDonneesExistantesAvecTransaction(
                        abonnement, parrainExistant, transactionExistante);
            }

            // Check JawuPay
            return verifierStatutJawuPayEtRepondre(
                    abonnement, transactionExistante, jawupayId, parrainExistant, jawupayAccess);

        } catch (Exception e) {
            logger.error("    ✗ Exception lors de la vérification du même parrain: {}",
                    e.getMessage(), e);

            // En cas d'erreur, renvoyer les données existantes par sécurité
            return renvoyerDonneesExistantesSansTransaction(abonnement, telParrainSaisi);
        }
    }

    // ============================================================================
    // FONCTION 5 : Vérification du statut JawuPay et réponse
    // ============================================================================

    /**
     * Vérifie le statut réel du paiement auprès de JawuPay et décide de la réponse
     *
     * @param abonnement    Abonnement concerné
     * @param transaction   Transaction associée
     * @param jawupayId     ID du paiement JawuPay
     * @param parrainNumero Numéro du parrain
     * @param jawupayAccess Token JawuPay
     * @return CommonResponseWs avec la décision appropriée
     */
    private CommonResponseWs verifierStatutJawuPayEtRepondre(
            Abonnement abonnement,
            TransactionsCcapp transaction,
            Long jawupayId,
            String parrainNumero,
            String jawupayAccess) {

        logger.info("    → Vérification du statut réel auprès de JawuPay (paymentId: {})", jawupayId);

        try {
            CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
                    null, jawupayId, jawupayAccess);

            CommonResponseWs checkResponse = this.check(checkRequest);

            if (checkResponse == null) {
                logger.error("      ✗ Réponse null de JawuPay - Renvoi des données existantes par sécurité");
                return renvoyerDonneesExistantesAvecTransaction(abonnement, parrainNumero, transaction);
            }

            logger.info("      Réponse JawuPay - Status: {}, Data: {}",
                    checkResponse.getStatus(), checkResponse.getData());

            // CAS 1 : Paiement réussi
            if (checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.info("      ✅ JawuPay confirme SUCCESS - Traitement du callback");

                try {
                    boolean isPaid = responseStatusUtils
                            .adapterStatus(WsOperationResponseStatus.success)
                            .equals(checkResponse.getData());

                    Abonnement abonnementMisAJour = this.traiterCallbackPaiement(jawupayId, isPaid);

                    if (abonnementMisAJour != null &&
                            abonnementMisAJour.getStatutPaiement() == StatutPaiement.PAYE) {

                        logger.info("      ✅ Callback traité avec succès - Nouvelle initialisation autorisée");
                        return null; // Autoriser nouvelle initialisation
                    } else {
                        logger.warn("      ⚠ Callback traité mais paiement non confirmé - Renvoi données");
                        return renvoyerDonneesExistantesAvecTransaction(abonnement, parrainNumero, transaction);
                    }

                } catch (OwomiException e) {
                    logger.error("      ✗ Erreur traitement callback: {}", e.getMessage(), e);
                    return renvoyerDonneesExistantesAvecTransaction(abonnement, parrainNumero, transaction);
                }
            }

            // CAS 2 : Paiement encore en attente
            if (checkResponse.getStatus().equals(WsOperationResponseStatus.pending) ||
                    checkResponse.getStatus().equals(WsOperationResponseStatus.inProcess)) {

                logger.info("      ⏳ JawuPay confirme PENDING - Renvoi des données existantes");

                return new CommonResponseWs(
                        WsOperationResponseStatus.success,
                        "Vous avez déjà un paiement en cours. " +
                                "Veuillez finaliser votre paiement ou attendre qu'il soit traité.",
                        200,
                        construireDonneesExistantes(abonnement, parrainNumero, transaction));
            }

            // CAS 3 : Paiement échoué
            logger.info("      ❌ JawuPay confirme FAILED - Mise à jour et autorisation nouvelle initialisation");

            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
            abonnementRepository.save(abonnement);

            transaction.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
            transaction.setDateResponse(new Date());
            transactionCcappRepository.save(transaction);

            logger.info("      ✓ Abonnement et transaction mis à jour en ECHEC");
            return null; // Autoriser nouvelle initialisation

        } catch (Exception e) {
            logger.error("      ✗ Exception lors du check JawuPay: {}", e.getMessage(), e);

            // Par sécurité, renvoyer les données existantes
            return renvoyerDonneesExistantesAvecTransaction(abonnement, parrainNumero, transaction);
        }
    }

    // ============================================================================
    // FONCTION 6 : Vérification de la même formule
    // ============================================================================

    /**
     * Vérifie si c'est la même formule et décide du blocage ou non
     *
     * @param abonnement         Abonnement EN_ATTENTE trouvé
     * @param formuleIdSouhaitee Formule souhaitée par l'utilisateur
     * @param userId             ID de l'utilisateur
     * @param jawupayAccess      Token JawuPay
     * @return CommonResponseWs si blocage, null sinon
     */
    private CommonResponseWs verifierMemeFormule(
            Abonnement abonnement, Long formuleIdSouhaitee, Long userId, String jawupayAccess) {

        logger.info("  → Vérification si même formule");

        try {
            Long formuleExistante = abonnement.getFormuleAbonnement().getId();
            boolean memeFormule = formuleExistante.equals(formuleIdSouhaitee);

            logger.info("    Formule existante: {}, Formule souhaitée: {}, Même formule: {}",
                    formuleExistante, formuleIdSouhaitee, memeFormule);

            if (!memeFormule) {
                logger.info("    ✓ Formule différente - Nettoyage nécessaire");
                return null; // Formule différente, continuer vers le nettoyage
            }

            logger.info("    ⚠ MÊME FORMULE détectée - Vérification de la transaction");

            // Récupération de la transaction
            TransactionsCcapp transactionEnAttente = transactionCcappRepository
                    .findOneByAbonnementId(abonnement.getId());

            if (transactionEnAttente == null) {
                logger.warn("    ✗ Aucune transaction trouvée - Autoriser nouvelle initialisation");
                return null;
            }

            String statutTransaction = transactionEnAttente.getStatut();
            logger.info("    Transaction trouvée - Statut: {}, JawupayId: {}",
                    statutTransaction, transactionEnAttente.getJawupayId());

            // Vérifier si transaction vraiment en attente
            if (statutTransaction == null ||
                    (!statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) &&
                            !statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {

                logger.info("    ✓ Transaction terminée - Autoriser nouvelle initialisation");
                return null;
            }

            // Transaction en attente - Vérifier auprès de JawuPay
            Long jawupayId = transactionEnAttente.getJawupayId();
            if (jawupayId == null) {
                logger.warn("    ✗ Pas de jawupayId - Bloquer par sécurité");
                return bloquerMemeFormule(abonnement, transactionEnAttente);
            }

            logger.info("    → Vérification auprès de JawuPay (jawupayId: {})", jawupayId);

            try {
                CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
                        null, jawupayId, jawupayAccess);

                CommonResponseWs checkResponse = this.check(checkRequest);

                if (checkResponse == null) {
                    logger.error("      ✗ Réponse null de JawuPay - Bloquer par sécurité");
                    return bloquerMemeFormule(abonnement, transactionEnAttente);
                }

                logger.info("      Réponse JawuPay - Status: {}", checkResponse.getStatus());

                // Si succès, traiter le callback
                if (checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                    logger.info("      ✅ Paiement réussi - Traitement du callback");

                    try {
                        boolean isPaid = responseStatusUtils
                                .adapterStatus(WsOperationResponseStatus.success)
                                .equals(checkResponse.getData());

                        this.traiterCallbackPaiement(jawupayId, isPaid);
                        logger.info("      ✓ Callback traité - Autoriser nouvelle initialisation");
                        return null;

                    } catch (OwomiException e) {
                        logger.error("      ✗ Erreur traitement callback: {}", e.getMessage(), e);
                        return bloquerMemeFormule(abonnement, transactionEnAttente);
                    }
                }

                // Sinon bloquer
                logger.warn("      ⏳ Paiement encore en cours - Bloquer nouvelle initialisation");
                return bloquerMemeFormule(abonnement, transactionEnAttente);

            } catch (Exception e) {
                logger.error("      ✗ Exception check JawuPay: {}", e.getMessage(), e);
                return bloquerMemeFormule(abonnement, transactionEnAttente);
            }

        } catch (Exception e) {
            logger.error("    ✗ Exception vérification même formule: {}", e.getMessage(), e);
            // En cas d'erreur, autoriser nouvelle initialisation
            return null;
        }
    }

    // ============================================================================
    // FONCTION 7 : Bloquer la même formule
    // ============================================================================

    /**
     * Bloque l'initialisation car même formule avec paiement en cours
     *
     * @param abonnement  Abonnement en attente
     * @param transaction Transaction associée
     * @return CommonResponseWs avec message de blocage
     */
    private CommonResponseWs bloquerMemeFormule(Abonnement abonnement, TransactionsCcapp transaction) {
        logger.info("    🔒 BLOCAGE - Même formule avec paiement en cours");

        Map<String, Object> data = Map.of(
                "abonnementEnAttenteId", abonnement.getId(),
                "formuleId", abonnement.getFormuleAbonnement().getId(),
                "commandeId", abonnement.getCommandeId(),
                "montant", (abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement)),
                "dateCreation", abonnement.getDateCreation(),
                "idPaiement", abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L,
                "transactionId", transaction.getId(),
                "statutTransaction", transaction.getStatut(),
                "referenceTransaction", transaction.getReference());

        String message = "Vous avez déjà un paiement en cours pour cette formule d'abonnement. " +
                "Veuillez finaliser ou annuler votre transaction en cours avant d'en initier une nouvelle.";

        CommonResponseWs response = new CommonResponseWs(
                WsOperationResponseStatus.failed,
                message,
                400,
                data);

        logger.info("    Response: {}", response);
        return response;
    }

    // ============================================================================
    // FONCTION 8 : Nettoyage des transactions en attente
    // ============================================================================

    /**
     * Nettoie toutes les transactions EN_ATTENTE en vérifiant leur statut
     *
     * @param userId        ID de l'utilisateur
     * @param jawupayAccess Token JawuPay
     */
    private void nettoyerTransactionsEnAttente(Long userId, String jawupayAccess) {
        logger.info("  → Nettoyage de toutes les transactions EN_ATTENTE");

        try {
            List<Abonnement> tousAbonnementsEnAttente = abonnementRepository
                    .findByUserIdAndStatutPaiementOrderByDateCreationDesc(userId, StatutPaiement.EN_ATTENTE);

            logger.info("    {} abonnement(s) EN_ATTENTE à vérifier", tousAbonnementsEnAttente.size());

            int nbTraites = 0;
            int nbErreurs = 0;

            for (Abonnement abonnementEnCours : tousAbonnementsEnAttente) {
                try {
                    logger.info("    → Traitement abonnement ID: {}", abonnementEnCours.getId());

                    TransactionsCcapp transactionEnCours = transactionCcappRepository
                            .findOneByAbonnementId(abonnementEnCours.getId());

                    if (transactionEnCours == null) {
                        logger.warn("      ⚠ Pas de transaction - Annulation de l'abonnement");
                        abonnementEnCours.setStatutPaiement(StatutPaiement.ANNULE);
                        abonnementEnCours.setActif(false);
                        abonnementRepository.save(abonnementEnCours);
                        nbTraites++;
                        continue;
                    }

                    String statutTransaction = transactionEnCours.getStatut();

                    // Vérifier uniquement les transactions en attente
                    if (statutTransaction == null ||
                            (!statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) &&
                                    !statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {

                        logger.info("      ✓ Transaction déjà terminée (statut: {})", statutTransaction);
                        nbTraites++;
                        continue;
                    }

                    Long jawupayId = transactionEnCours.getJawupayId();
                    if (jawupayId == null) {
                        logger.warn("      ⚠ Pas de jawupayId - Mise en ECHEC");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                        continue;
                    }

                    // Check JawuPay
                    logger.info("      → Check JawuPay (jawupayId: {})", jawupayId);

                    CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
                            null, jawupayId, jawupayAccess);

                    CommonResponseWs checkResponse = this.check(checkRequest);

                    if (checkResponse == null) {
                        logger.warn("      ⚠ Réponse null - Mise en ECHEC par sécurité");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                        continue;
                    }

                    if (checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                        logger.info("      ✅ Paiement réussi - Traitement du callback");

                        try {
                            boolean isPaid = responseStatusUtils
                                    .adapterStatus(WsOperationResponseStatus.success)
                                    .equals(checkResponse.getData());

                            this.traiterCallbackPaiement(jawupayId, isPaid);
                            nbTraites++;

                        } catch (OwomiException e) {
                            logger.error("      ✗ Erreur callback: {}", e.getMessage(), e);
                            nbErreurs++;
                        }
                    } else {
                        logger.info("      ❌ Paiement non réussi - Mise en ECHEC");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                    }

                } catch (Exception e) {
                    logger.error("      ✗ Erreur traitement abonnement {}: {}",
                            abonnementEnCours.getId(), e.getMessage(), e);
                    nbErreurs++;
                }
            }

            logger.info("  ✓ Nettoyage terminé - {} traités, {} erreurs", nbTraites, nbErreurs);

        } catch (Exception e) {
            logger.error("  ✗ Exception lors du nettoyage: {}", e.getMessage(), e);
        }
    }

    private void nettoyerTransactionsEnAttenteV2(Long userId, String accessToken) {
        logger.info("  → Nettoyage de toutes les transactions EN_ATTENTE");

        try {
            List<Abonnement> tousAbonnementsEnAttente = abonnementRepository
                    .findByUserIdAndStatutPaiementOrderByDateCreationDesc(userId, StatutPaiement.EN_ATTENTE);

            logger.info("    {} abonnement(s) EN_ATTENTE à vérifier", tousAbonnementsEnAttente.size());

            int nbTraites = 0;
            int nbErreurs = 0;

            for (Abonnement abonnementEnCours : tousAbonnementsEnAttente) {
                try {
                    logger.info("    → Traitement abonnement ID: {}", abonnementEnCours.getId());

                    TransactionsCcapp transactionEnCours = transactionCcappRepository
                            .findOneByAbonnementId(abonnementEnCours.getId());

                    if (transactionEnCours == null) {
                        logger.warn("      ⚠ Pas de transaction - Annulation de l'abonnement");
                        abonnementEnCours.setStatutPaiement(StatutPaiement.ANNULE);
                        abonnementEnCours.setActif(false);
                        abonnementRepository.save(abonnementEnCours);
                        nbTraites++;
                        continue;
                    }

                    String statutTransaction = transactionEnCours.getStatut();

                    // Vérifier uniquement les transactions en attente
                    if (statutTransaction == null ||
                            (!statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) &&
                                    !statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {

                        logger.info("      ✓ Transaction déjà terminée (statut: {})", statutTransaction);
                        nbTraites++;
                        continue;
                    }

                    Long jawupayId = transactionEnCours.getJawupayId();
                    if (jawupayId == null) {
                        logger.warn("      ⚠ Pas de jawupayId - Mise en ECHEC");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                        continue;
                    }

                    // Check JawuPay
                    logger.info("      → Check JawuPay (jawupayId: {})", jawupayId);

                    // CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs( null,
                    // jawupayId, jawupayAccess);

                    // CommonResponseWs checkResponse = this.check(checkRequest);

                    String token = partnerAdminClientService.getLassToken(accessToken).getData().toString();

                    CommonResponseWs checkResponse = paiementV2ClientService.checkPaiement(jawupayId, PATNER_API_KEY,
                            token);

                    if (checkResponse == null) {
                        logger.warn("      ⚠ Réponse null - Mise en ECHEC par sécurité");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                        continue;
                    }




                    if (checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                        logger.info("      ✅ Paiement réussi - Traitement du callback");

                        try {
                            boolean isPaid = responseStatusUtils
                                    .adapterStatus(WsOperationResponseStatus.success)
                                    .equals(checkResponse.getData());

                            this.traiterCallbackPaiement(jawupayId, isPaid);
                            nbTraites++;

                        } catch (OwomiException e) {
                            logger.error("      ✗ Erreur callback: {}", e.getMessage(), e);
                            nbErreurs++;
                        }
                    } else {
                        logger.info("      ❌ Paiement non réussi - Mise en ECHEC");
                        mettreEnEchec(abonnementEnCours, transactionEnCours);
                        nbTraites++;
                    }

                } catch (Exception e) {
                    logger.error("      ✗ Erreur traitement abonnement {}: {}",
                            abonnementEnCours.getId(), e.getMessage(), e);
                    nbErreurs++;
                }
            }

            logger.info("  ✓ Nettoyage terminé - {} traités, {} erreurs", nbTraites, nbErreurs);

        } catch (Exception e) {
            logger.error("  ✗ Exception lors du nettoyage: {}", e.getMessage(), e);
        }
    }

    /**
     * Met un abonnement et sa transaction en ECHEC
     */
    private void mettreEnEchec(Abonnement abonnement, TransactionsCcapp transaction) {
        try {
            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
            abonnementRepository.save(abonnement);

            transaction.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
            transaction.setDateResponse(new Date());
            transactionCcappRepository.save(transaction);

            logger.info("      ✓ Abonnement {} et transaction {} mis en ECHEC",
                    abonnement.getId(), transaction.getId());

        } catch (Exception e) {
            logger.error("      ✗ Erreur mise en ECHEC: {}", e.getMessage(), e);
        }
    }

    // ============================================================================
    // FONCTION 9 : Procéder à la nouvelle initialisation
    // ============================================================================

    /**
     * Procède à la nouvelle initialisation d'un abonnement
     *
     * @param numeroCCParent Numéro du parrain
     * @param userId         ID de l'utilisateur
     * @param formuleId      ID de la formule
     * @param userProfile    Profil de l'utilisateur
     * @param jawupayAccess  Token JawuPay
     * @return CommonResponseWs avec les données de l'abonnement créé
     */
    private CommonResponseWs procederNouvelleInitialisation(
            Optional<String> numeroCCParent,
            Long userId,
            Long formuleId,
            UserProfileWs userProfile,
            String jawupayAccess) {

        logger.info("→ Procédure de nouvelle initialisation");

        try {
            // 1. Mise à jour du step utilisateur si nécessaire
            if (!userProfile.getUserStep().equals(StepInSystem.LINKEDIN)) {
                logger.info("  → Mise à jour du step utilisateur vers PAIEMENT");

                try {
                    UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
                    updateRequestWs.setUserStep(StepInSystem.PAIEMENT);
                    userInfosClient.updateUserAndProfile(
                            updateRequestWs,
                            SecurityUtils.getAccessToken(httpServletRequest));
                    logger.info("  ✓ Step mis à jour");

                } catch (Exception e) {
                    logger.warn("  ⚠ Échec mise à jour step (non bloquant): {}", e.getMessage());
                }
            }

            // 2. Initialisation de l'achat d'abonnement
            logger.info("  → Initialisation de l'achat d'abonnement");
            Abonnement abonnement = initialiserAchatAbonnement(numeroCCParent, userId, formuleId);

            logger.info("  ✓ Abonnement créé - ID: {}, CommandeId: {}",
                    abonnement.getId(), abonnement.getCommandeId());

            // 3. Vérification critique du montant
            double montantCalcule = abonnement.getFormuleAbonnement().getPrix().doubleValue()
                    * calculDuree(abonnement);
            double prixFormule = abonnement.getFormuleAbonnement().getPrix().doubleValue();
            int dureeFormule = calculDuree(abonnement);
            double montantAttendu = prixFormule * dureeFormule;

            if (Math.abs(montantCalcule - montantAttendu) > 0.01) {
                logger.error("  ✗ ALERTE SÉCURITÉ - Montant incohérent !");
                logger.error("    Montant calculé: {}, Montant attendu: {}", montantCalcule, montantAttendu);
                logger.error("    FormuleId: {}, Prix: {}, Durée: {}", formuleId, prixFormule, dureeFormule);

                throw new IllegalStateException("Erreur de calcul du montant. Veuillez réessayer.");
            }

            logger.info("  ✓ Montant vérifié - Formule: {}, Prix: {}, Durée: {} mois, Total: {} XOF",
                    abonnement.getFormuleAbonnement().getTypeAbonnement(),
                    prixFormule,
                    dureeFormule,
                    montantCalcule);

            // 4. Création de la demande de paiement
            logger.info("  → Création de la demande de paiement JawuPay");

            CreatePaymentWs paymentWs = new CreatePaymentWs();
            paymentWs.setDevise(TypeDevise.XOF.toString());

            if (userProfile.getUserStep().equals(StepInSystem.LINKEDIN) &&
                    userProfile.getInSystem()) {
                paymentWs.setTypePayment(TypeOperation.RENOUVELLEMENT.toString());
            } else {
                paymentWs.setTypePayment(TypeOperation.PAIEMENT.toString());
            }

            paymentWs.setUrlCallback(
                    "https://api-gateway.clubchretien.org/abonnement-service/api/abonnements/callback");
            paymentWs.setMessage("Initialisation de paiement de votre abonnement");
            paymentWs.setMontant(montantCalcule);

            // 5. Initialisation du paiement
            CommonResponseWs initResp = null;

            try {
                initResp = initPaiement(paymentWs, jawupayAccess);

            } catch (Exception e) {
                logger.error("  ✗ Exception lors de l'initialisation du paiement: {}", e.getMessage(), e);

                // Rollback : Supprimer l'abonnement créé
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException(
                        "Erreur lors de l'initialisation du paiement. Veuillez réessayer.", e);
            }

            if (initResp == null || initResp.getStatus() == null ||
                    !initResp.getStatus().equals(WsOperationResponseStatus.success)) {

                logger.error("  ✗ Échec de l'initialisation du paiement");
                logger.error("    Réponse: {}", initResp);

                // Rollback : Supprimer l'abonnement créé
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException(
                        "Échec de l'initialisation du paiement chez le fournisseur. Veuillez réessayer.");
            }
            logger.debug("La reponse de init {}", initResp.getData());
            Long idPaiement = null;
            String tokenTransaction = null;

            if (initResp.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) initResp.getData();
                idPaiement = convertToLong(data.get("paymentId"));
                tokenTransaction = (String) data.get("transactionToken");
            } else {
                idPaiement = convertToLong(initResp.getData());
            }

            if (idPaiement == null) {
                logger.error("  ✗ ID de paiement null reçu de JawuPay");

                // Rollback
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException("ID de paiement invalide reçu. Veuillez réessayer.");
            }

            logger.info("  ✓ Paiement initialisé - PaymentId: {}", idPaiement);

            // Sauvegarde des données de parrainage
            if (numeroCCParent.isPresent() && !numeroCCParent.get().isEmpty()) {
                try {
                    CommonResponseWs parrainageResponse = parrainageService.saveParrainageData(
                            numeroCCParent,
                            idPaiement,
                            abonnement.getId()
                    );
                    logger.info("saveParrainageData - Status: {}, Message: {}, Data: {}",
                            parrainageResponse.getStatus(),
                            parrainageResponse.getMessage(),
                            parrainageResponse.getData());
                } catch (Exception e) {
                    logger.warn("Échec saveParrainageData (non bloquant): {}", e.getMessage());
                }
            }

            // 6. Création de la transaction
            logger.info("  → Création de la transaction en BDD");

            String ref = "PYM-" + UUID.randomUUID().toString().toUpperCase();

            TransactionsCcapp transaction = new TransactionsCcapp();
            transaction.setDevise(paymentWs.getDevise());
            transaction.setMontant(BigDecimal.valueOf(paymentWs.getMontant()));
            transaction.setType(paymentWs.getTypePayment());
            transaction.setCallbackUrl(paymentWs.getUrlCallback());
            transaction.setPartenaireId(paymentWs.getIdPartenaire());
            transaction.setJawupayId(idPaiement);
            transaction.setAbonnementId(abonnement.getId());
            transaction.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.pending));
            transaction.setReference(ref);
            transaction = transactionCcappRepository.save(transaction);

            logger.info("  ✓ Transaction créée - ID: {}, Référence: {}", transaction.getId(), ref);

            // 7. Mise à jour de l'abonnement avec le paymentId
            abonnement.setPaymentId(idPaiement);
            abonnement = abonnementRepository.save(abonnement);

            logger.info("  ✓ Abonnement mis à jour avec paymentId");

            // 8. Mise à jour de l'archive parent avec le paymentId
            try {
                ArchiveParent archiveParent = archiveParentRepository
                        .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());

                if (archiveParent != null) {
                    archiveParent.setPaymentId(idPaiement);
                    archiveParentRepository.save(archiveParent);
                    logger.info("  ✓ PaymentId mis à jour dans l'archive parrain");
                }

            } catch (Exception e) {
                logger.warn("  ⚠ Échec mise à jour archive parent (non bloquant): {}", e.getMessage());
            }

            // 9. Construction de la réponse
            Map<String, Object> resultat = new HashMap<>();
            resultat.put("idTransaction", transaction.getId());
            resultat.put("reference", ref);
            resultat.put("idAbonnement", abonnement.getId());
            resultat.put("montant", montantCalcule);
            resultat.put("idPaiement", idPaiement);
            resultat.put("formuleId", formuleId);
            resultat.put("tokenTransaction", tokenTransaction);

            logger.info("  ✅ Nouvelle initialisation réussie");
            logger.info("    AbonnementId: {}, PaymentId: {}, Montant: {} XOF",
                    abonnement.getId(), idPaiement, montantCalcule);

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Abonnement initialisé avec succès",
                    200,
                    resultat);

        } catch (IllegalArgumentException e) {
            logger.error("  ✗ Erreur de validation: {}", e.getMessage(), e);
            throw e;

        } catch (RuntimeException e) {
            logger.error("  ✗ Erreur runtime: {}", e.getMessage(), e);
            throw e;

        } catch (Exception e) {
            logger.error("  ✗ Erreur inattendue: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'initialisation de l'abonnement", e);
        }
    }

    private CommonResponseWs procederNouvelleInitialisationV2(
            Optional<String> numeroCCParent,
            Long userId,
            Long formuleId,
            UserProfileWs userProfile) {

        logger.info("→ Procédure de nouvelle initialisation");

        try {

            // 2. Initialisation de l'achat d'abonnement
            logger.info("  → Initialisation de l'achat d'abonnement");
            Abonnement abonnement = initialiserAchatAbonnement(numeroCCParent, userId, formuleId);

            logger.info("  ✓ Abonnement créé - ID: {}, CommandeId: {}",
                    abonnement.getId(), abonnement.getCommandeId());

            // 3. Vérification critique du montant
            double montantCalcule = abonnement.getFormuleAbonnement().getPrix().doubleValue()
                    * calculDuree(abonnement);
            double prixFormule = abonnement.getFormuleAbonnement().getPrix().doubleValue();
            int dureeFormule = calculDuree(abonnement);
            double montantAttendu = prixFormule * dureeFormule;

            if (Math.abs(montantCalcule - montantAttendu) > 0.01) {
                logger.error("  ✗ ALERTE SÉCURITÉ - Montant incohérent !");
                logger.error("    Montant calculé: {}, Montant attendu: {}", montantCalcule, montantAttendu);
                logger.error("    FormuleId: {}, Prix: {}, Durée: {}", formuleId, prixFormule, dureeFormule);

                throw new IllegalStateException("Erreur de calcul du montant. Veuillez réessayer.");
            }

            logger.info("  ✓ Montant vérifié - Formule: {}, Prix: {}, Durée: {} mois, Total: {} XOF",
                    abonnement.getFormuleAbonnement().getTypeAbonnement(),
                    prixFormule,
                    dureeFormule,
                    montantCalcule);

            // 4. Création de la demande de paiement
            logger.info("  → Création de la demande de paiement JawuPay");

            CreatePaymentWs paymentWs = new CreatePaymentWs();
            paymentWs.setDevise(TypeDevise.XOF.toString());

            if (userProfile.getUserStep().equals(StepInSystem.LINKEDIN) &&
                    userProfile.getInSystem()) {
                paymentWs.setTypePayment(TypeOperation.RENOUVELLEMENT.toString());
            } else {
                paymentWs.setTypePayment(TypeOperation.PAIEMENT.toString());
            }

            paymentWs.setUrlCallback(
                    "https://api-gateway.clubchretien.org/abonnement-service/api/abonnements/callback");
            paymentWs.setMessage("Initialisation de paiement de votre abonnement");
            paymentWs.setMontant(montantCalcule);

            // 5. Initialisation du paiement
            CommonResponseWs initResp = null;

            PartnerPaymentInitializerWs paymentInitializerWs = new PartnerPaymentInitializerWs();
            String ref = "PYM-" + UUID.randomUUID().toString().toUpperCase();

            paymentInitializerWs.setAmount(montantCalcule);
            paymentInitializerWs.setMessage("Initialisation de paiement de votre abonnement");
            paymentInitializerWs.setCallbackUrl(
                    "https://api-gateway.clubchretien.org/abonnement-service/api/abonnements/callback");
            paymentInitializerWs.setReference(ref);
            paymentInitializerWs.setEmail(userProfile.getEmail());

            try {
                initResp = paiementV2Service.initPaiement(paymentInitializerWs);

            } catch (Exception e) {
                logger.error("  ✗ Exception lors de l'initialisation du paiement: {}", e.getMessage(), e);

                // Rollback : Supprimer l'abonnement créé
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException(
                        "Erreur lors de l'initialisation du paiement. Veuillez réessayer.", e);
            }

            if (initResp == null || initResp.getStatus() == null ||
                    !initResp.getStatus().equals(WsOperationResponseStatus.success)) {

                logger.error("  ✗ Échec de l'initialisation du paiement");
                logger.error("    Réponse: {}", initResp);

                // Rollback : Supprimer l'abonnement créé
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException(
                        "Échec de l'initialisation du paiement chez le fournisseur. Veuillez réessayer.");
            }

            logger.debug("La reponse de init {}", initResp.getData());

            Long idPaiement = null;
            String tokenTransaction = null;

            if (initResp.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) initResp.getData();
                idPaiement = convertToLong(data.get("paymentId"));
                tokenTransaction = (String) data.get("transactionToken");
            } else {
                idPaiement = convertToLong(initResp.getData());
            }

            if (idPaiement == null) {
                logger.error("  ✗ ID de paiement null reçu de JawuPay");

                // Rollback
                logger.warn("  → Rollback : Suppression de l'abonnement {}", abonnement.getId());
                abonnementRepository.delete(abonnement);

                throw new RuntimeException("ID de paiement invalide reçu. Veuillez réessayer.");
            }

            logger.info("  ✓ Paiement initialisé - PaymentId: {}", idPaiement);

            // Sauvegarde des données de parrainage
            if (numeroCCParent.isPresent() && !numeroCCParent.get().isEmpty()) {
                try {
                    CommonResponseWs parrainageResponse = parrainageService.saveParrainageData(
                            numeroCCParent,
                            idPaiement,
                            abonnement.getId());
                    logger.info("SaveParrainageData - Status: {}, Message: {}, Data: {}",
                            parrainageResponse.getStatus(),
                            parrainageResponse.getMessage(),
                            parrainageResponse.getData());
                } catch (Exception e) {
                    logger.warn("Échec saveParrainageData (non bloquant): {}", e.getMessage());
                }
            }

            // 6. Création de la transaction
            logger.info("  → Création de la transaction en BDD");

            TransactionsCcapp transaction = new TransactionsCcapp();
            transaction.setDevise(paymentWs.getDevise());
            transaction.setMontant(BigDecimal.valueOf(paymentWs.getMontant()));
            transaction.setType(paymentWs.getTypePayment());
            transaction.setCallbackUrl(paymentWs.getUrlCallback());
            transaction.setPartenaireId(paymentWs.getIdPartenaire());
            transaction.setJawupayId(idPaiement);
            transaction.setAbonnementId(abonnement.getId());
            transaction.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.pending));
            transaction.setReference(ref);
            transaction = transactionCcappRepository.save(transaction);

            logger.info("  ✓ Transaction créée - ID: {}, Référence: {}", transaction.getId(), ref);

            // 7. Mise à jour de l'abonnement avec le paymentId
            abonnement.setPaymentId(idPaiement);
            abonnement = abonnementRepository.save(abonnement);

            logger.info("  ✓ Abonnement mis à jour avec paymentId");

            // TODO: Notification en cas de reabonnement à integre en cas de condition de
            // réabonnement verifier

            try {
                String token = httpServletRequest.getHeader("access-token");

                CommonResponseWs firebaseUID = mobileServiceClient.getFirebaseUID(userProfile.getPhoneFullNumber(),
                        token);
                String userFirebaseUID = (String) firebaseUID.getData();
                logger.debug("firebaseUID is >>>>>>>>>>>>>>>>>>>>> {}", userFirebaseUID);

                if (firebaseUID != null) {
                    CommonResponseWs commonResponseWs = mobileAuthenticationServiceClient
                            .getPartenaire(userFirebaseUID, token);

                    // FIX: Même problème potentiel avec Partenaire
                    String firebaseToken = null;
                    if (commonResponseWs != null && commonResponseWs.getData() != null) {
                        if (commonResponseWs.getData() instanceof Map) {
                            Map<String, Object> partenaireData = (Map<String, Object>) commonResponseWs
                                    .getData();
                            firebaseToken = (String) partenaireData.get("firebasetoken");
                        } else if (commonResponseWs.getData() instanceof Partenaire) {
                            firebaseToken = ((Partenaire) commonResponseWs.getData()).getFirebasetoken();
                        }
                    }

                    if (firebaseToken != null) {
                        String messageReabonnement = String.format(RENEWAL_SUCCESS_MESSAGE_TEMPLATE,
                                abonnement.getDateFin(), userProfile.getDisplayName());
                        notificationUtils.notificationInCCapp(userFirebaseUID, RENEWAL_SUCCESS_TITLE,
                                messageReabonnement,
                                firebaseToken);
                    } else {
                        logger.warn("⚠️ Pas de firebaseToken pour le user, notification ignorée");
                    }
                } else {
                    logger.warn("⚠️ Pas de firebaseUID pour le user, notification ignorée");
                }
            } catch (Exception notifEx) {
                logger.warn("⚠️ Erreur d'envoi de notification à l'utilisateur (non bloquant): {}",
                        notifEx.getMessage());
            }

            // 8. Mise à jour de l'archive parent avec le paymentId
            try {
                ArchiveParent archiveParent = archiveParentRepository
                        .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());

                if (archiveParent != null) {
                    archiveParent.setPaymentId(idPaiement);
                    archiveParentRepository.save(archiveParent);
                    logger.info("  ✓ PaymentId mis à jour dans l'archive parrain");
                }

            } catch (Exception e) {
                logger.warn("  ⚠ Échec mise à jour archive parent (non bloquant): {}", e.getMessage());
            }

            // 9. Construction de la réponse
            Map<String, Object> resultat = new HashMap<>();
            resultat.put("idTransaction", transaction.getId());
            resultat.put("reference", ref);
            resultat.put("idAbonnement", abonnement.getId());
            resultat.put("montant", montantCalcule);
            resultat.put("idPaiement", idPaiement);
            resultat.put("formuleId", formuleId);
            resultat.put("tokenTransaction", tokenTransaction);

            logger.info("  ✅ Nouvelle initialisation réussie");
            logger.info("    AbonnementId: {}, PaymentId: {}, Montant: {} XOF",
                    abonnement.getId(), idPaiement, montantCalcule);
            // 1. Mise à jour du step utilisateur si nécessaire
            if (!userProfile.getUserStep().equals(StepInSystem.LINKEDIN)) {
                logger.info("  → Mise à jour du step utilisateur vers PAIEMENT");

                try {
                    UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
                    updateRequestWs.setUserStep(StepInSystem.PAIEMENT);
                    userInfosClient.updateUserAndProfile(
                            updateRequestWs,
                            SecurityUtils.getAccessToken(httpServletRequest)
                    );
                    logger.info("  ✓ Step mis à jour");

                } catch (Exception e) {
                    logger.warn("  ⚠ Échec mise à jour step (non bloquant): {}", e.getMessage());
                }
            }

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Abonnement initialisé avec succès",
                    200,
                    resultat);

        } catch (IllegalArgumentException e) {
            logger.error("  ✗ Erreur de validation: {}", e.getMessage(), e);
            throw e;

        } catch (RuntimeException e) {
            logger.error("  ✗ Erreur runtime: {}", e.getMessage(), e);
            throw e;

        } catch (Exception e) {
            logger.error("  ✗ Erreur inattendue: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'initialisation de l'abonnement", e);
        }
    }

    // ============================================================================
    // FONCTIONS UTILITAIRES
    // ============================================================================

    /**
     * Renvoie les données existantes d'un abonnement sans transaction
     */
    private CommonResponseWs renvoyerDonneesExistantesSansTransaction(
            Abonnement abonnement, String parrainNumero) {

        Map<String, Object> donneesExistantes = new HashMap<>();
        donneesExistantes.put("idAbonnement", abonnement.getId());
        donneesExistantes.put("formuleId", abonnement.getFormuleAbonnement().getId());
        donneesExistantes.put("commandeId", abonnement.getCommandeId());
        donneesExistantes.put("montant",
                abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement));
        donneesExistantes.put("dateCreation", abonnement.getDateCreation());
        donneesExistantes.put("idPaiement",
                abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L);
        donneesExistantes.put("parrainNumero", parrainNumero);

        String message = "Vous avez déjà un abonnement en cours d'initialisation. " +
                "Veuillez finaliser le processus de paiement.";

        return new CommonResponseWs(
                WsOperationResponseStatus.success,
                message,
                200,
                donneesExistantes);
    }

    /**
     * Renvoie les données existantes d'un abonnement avec transaction
     */
    private CommonResponseWs renvoyerDonneesExistantesAvecTransaction(
            Abonnement abonnement, String parrainNumero, TransactionsCcapp transaction) {

        Map<String, Object> donneesExistantes = construireDonneesExistantes(
                abonnement, parrainNumero, transaction);

        String message = "Vous avez déjà un paiement en cours pour cet abonnement. " +
                "Veuillez finaliser votre paiement ou attendre qu'il soit traité.";

        return new CommonResponseWs(
                WsOperationResponseStatus.success,
                message,
                200,
                donneesExistantes);
    }

    /**
     * Construit le map des données existantes
     */
    private Map<String, Object> construireDonneesExistantes(
            Abonnement abonnement, String parrainNumero, TransactionsCcapp transaction) {

        Map<String, Object> donneesExistantes = new HashMap<>();
        donneesExistantes.put("idAbonnement", abonnement.getId());
        donneesExistantes.put("formuleId", abonnement.getFormuleAbonnement().getId());
        donneesExistantes.put("commandeId", abonnement.getCommandeId());
        donneesExistantes.put("montant",
                abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement));
        donneesExistantes.put("dateCreation", abonnement.getDateCreation());
        donneesExistantes.put("idPaiement",
                abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L);
        donneesExistantes.put("parrainNumero", parrainNumero);
        donneesExistantes.put("idTransaction", transaction.getId());
        donneesExistantes.put("reference", transaction.getReference());
        donneesExistantes.put("statutTransaction", transaction.getStatut());

        return donneesExistantes;
    }


    /**
     * Initialise un nouvel abonnement avec vérification intelligente des paiements
     * en attente
     * - Bloque uniquement si c'est la MÊME formule en attente
     * - Permet le changement de formule même avec un paiement en cours
     * - Vérifie le statut réel auprès de JawuPay avant de bloquer
     *
     * @return CommonResponseWs avec les détails de l'initialisation
     */
    /*
     * public CommonResponseWs initializer(Optional<String> numeroCCParent, Long
     * formuleId) {
     * Long userId = CustomJwtUtils.getCurrentUserId();
     * UserProfileWs userProfileWs = CustomJwtUtils.getCurrentUser();
     * StepInSystem userStep = userProfileWs.getUserStep();
     *
     * String jawupayAccess = httpServletRequest.getHeader("jawupay-access");
     * String telParrain = numeroCCParent.orElse("");
     *
     * // ============================================
     * // VÉRIFICATION INTELLIGENTE : Paiement en attente
     * // ============================================
     * logger.info("Vérification des paiements en attente pour userId: {}", userId);
     *
     * Optional<Abonnement> abonnementEnAttente = abonnementRepository
     * .findFirstByUserIdAndStatutPaiementOrderByDateCreationDesc(
     * userId,
     * StatutPaiement.EN_ATTENTE);
     *
     * if (abonnementEnAttente.isPresent()) {
     * Abonnement abonnement = abonnementEnAttente.get();
     * logger.
     * warn("Abonnement en attente détecté pour userId: {} - AbonnementId: {}",
     * userId, abonnement.getId());
     *
     * // ============================================
     * // 🔍 VÉRIFICATION DU PARRAIN : Si même parrain → renvoyer données existantes
     * // ============================================
     * ArchiveParent archiveParentExistant = archiveParentRepository
     * .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());
     * String parrainExistant = (archiveParentExistant != null) ?
     * archiveParentExistant.getNumeroCCParent() : "";
     * if (parrainExistant == null)
     * parrainExistant = "";
     * String nouveauParrain = telParrain != null ? telParrain : "";
     *
     * logger.info("Comparaison parrains - Existant: '{}', Nouveau: '{}'",
     * parrainExistant, nouveauParrain);
     *
     * boolean memeParrain = parrainExistant.equals(nouveauParrain);
     *
     * if (memeParrain) {
     * // Même parrain : vérifier le statut JawuPay avant de renvoyer les données
     * logger.info("Même parrain détecté - Vérification du statut JawuPay");
     *
     * TransactionsCcapp transactionExistante = transactionCcappRepository
     * .findOneByAbonnementId(abonnement.getId());
     *
     * if (transactionExistante != null && transactionExistante.getJawupayId() !=
     * null) {
     * Long jawupayId = transactionExistante.getJawupayId();
     * String statutTransaction = transactionExistante.getStatut();
     *
     * // Vérifier si transaction encore en attente
     * if (statutTransaction != null &&
     * (statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) ||
     * statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {
     *
     * try {
     * logger.info("🔍 Vérification du statut réel du paiement {} auprès de JawuPay"
     * , jawupayId);
     *
     * CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
     * null, jawupayId, jawupayAccess);
     *
     * CommonResponseWs checkResponse = this.check(checkRequest);
     * logger.debug("La réponse de JawuPay: {}", checkResponse);
     *
     * if (checkResponse != null &&
     * checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
     * // Paiement réussi → Traiter le callback
     * logger.
     * info("✅ JawuPay confirme succès pour jawupayId: {} - Traitement du callback",
     * jawupayId);
     *
     * boolean isPaid = (responseStatusUtils
     * .adapterStatus(WsOperationResponseStatus.success))
     * .equals(checkResponse.getData());
     *
     * try {
     * Abonnement abonnementMisAJour = this.traiterCallbackPaiement(jawupayId,
     * isPaid);
     *
     * if (abonnementMisAJour != null &&
     * abonnementMisAJour.getStatutPaiement() == StatutPaiement.PAYE) {
     * logger.info("✅ Paiement finalisé - Nouvelle initialisation autorisée");
     * // Paiement traité, on permet une nouvelle initialisation
     * // (on ne retourne pas ici, on continue le flux)
     * }
     * } catch (OwomiException e) {
     * logger.error("❌ Erreur traitement callback pour jawupayId: {}", jawupayId,
     * e);
     * }
     * } else {
     * // Paiement pas encore réussi → Renvoyer les données existantes
     * logger.info("⏳ Paiement encore en cours - Renvoi des données existantes");
     *
     * Map<String, Object> donneesExistantes = new HashMap<>();
     * donneesExistantes.put("idAbonnement", abonnement.getId());
     * donneesExistantes.put("formuleId",
     * abonnement.getFormuleAbonnement().getId());
     * donneesExistantes.put("commandeId", abonnement.getCommandeId());
     * donneesExistantes.put("montant",
     * abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement));
     * donneesExistantes.put("dateCreation", abonnement.getDateCreation());
     * donneesExistantes.put("idPaiement",
     * abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L);
     * donneesExistantes.put("parrainNumero", parrainExistant);
     * donneesExistantes.put("idTransaction", transactionExistante.getId());
     * donneesExistantes.put("reference", transactionExistante.getReference());
     * donneesExistantes.put("statutTransaction", transactionExistante.getStatut());
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.success,
     * "Vous avez déjà un abonnement en attente de paiement",
     * 200,
     * donneesExistantes);
     * }
     * } catch (Exception e) {
     * logger.error("❌ Erreur check JawuPay pour jawupayId: {}", jawupayId, e);
     * // En cas d'erreur, renvoyer les données existantes
     * Map<String, Object> donneesExistantes = new HashMap<>();
     * donneesExistantes.put("idAbonnement", abonnement.getId());
     * donneesExistantes.put("formuleId",
     * abonnement.getFormuleAbonnement().getId());
     * donneesExistantes.put("commandeId", abonnement.getCommandeId());
     * donneesExistantes.put("montant",
     * abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement));
     * donneesExistantes.put("dateCreation", abonnement.getDateCreation());
     * donneesExistantes.put("idPaiement",
     * abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L);
     * donneesExistantes.put("parrainNumero", parrainExistant);
     * donneesExistantes.put("idTransaction", transactionExistante.getId());
     * donneesExistantes.put("reference", transactionExistante.getReference());
     * donneesExistantes.put("statutTransaction", transactionExistante.getStatut());
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.success,
     * "Vous avez déjà un abonnement en attente de paiement",
     * 200,
     * donneesExistantes);
     * }
     * } else {
     * // Transaction déjà terminée → Permettre nouvelle initialisation
     * logger.
     * info("✅ Transaction terminée (statut: {}) - Nouvelle initialisation autorisée"
     * ,
     * statutTransaction);
     * }
     * } else {
     * // Pas de transaction ou pas de jawupayId → Renvoyer les données existantes
     * logger.info("⚠️ Pas de transaction JawuPay - Renvoi des données existantes");
     *
     * Map<String, Object> donneesExistantes = new HashMap<>();
     * donneesExistantes.put("idAbonnement", abonnement.getId());
     * donneesExistantes.put("formuleId",
     * abonnement.getFormuleAbonnement().getId());
     * donneesExistantes.put("commandeId", abonnement.getCommandeId());
     * donneesExistantes.put("montant",
     * abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement));
     * donneesExistantes.put("dateCreation", abonnement.getDateCreation());
     * donneesExistantes.put("idPaiement",
     * abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L);
     * donneesExistantes.put("parrainNumero", parrainExistant);
     *
     * if (transactionExistante != null) {
     * donneesExistantes.put("idTransaction", transactionExistante.getId());
     * donneesExistantes.put("reference", transactionExistante.getReference());
     * donneesExistantes.put("statutTransaction", transactionExistante.getStatut());
     * }
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.success,
     * "Vous avez déjà un abonnement en attente de paiement",
     * 200,
     * donneesExistantes);
     * }
     * }
     *
     * // Parrain différent : continuer avec la logique existante
     * logger.
     * info("Parrain différent détecté - Poursuite de la vérification normale");
     *
     * // ============================================
     * // ✅ VÉRIFICATION CRITIQUE : Est-ce la MÊME formule ?
     * // ============================================
     * boolean memeFormule =
     * abonnement.getFormuleAbonnement().getId().equals(formuleId);
     *
     * if (!memeFormule) {
     * logger.info(
     * "🔄 Formule différente détectée - Ancienne: {}, Nouvelle: {}. Vérification de toutes les transactions en attente"
     * ,
     * abonnement.getFormuleAbonnement().getId(), formuleId);
     *
     * // ============================================
     * // 🔍 VÉRIFIER TOUTES LES TRANSACTIONS EN ATTENTE
     * // ============================================
     * List<Abonnement> tousAbonnementsEnAttente = abonnementRepository
     * .findByUserIdAndStatutPaiementOrderByDateCreationDesc(userId,
     * StatutPaiement.EN_ATTENTE);
     *
     * logger.info("📊 Nombre total d'abonnements EN_ATTENTE pour userId {}: {}",
     * userId, tousAbonnementsEnAttente.size());
     *
     * for (Abonnement abonnementEnCours : tousAbonnementsEnAttente) {
     * TransactionsCcapp transactionEnCours = transactionCcappRepository
     * .findOneByAbonnementId(abonnementEnCours.getId());
     *
     * if (transactionEnCours != null) {
     * String statutTransaction = transactionEnCours.getStatut();
     *
     * // Vérifier uniquement les transactions vraiment en attente
     * if (statutTransaction != null &&
     * (statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) ||
     * statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {
     *
     * Long jawupayId = transactionEnCours.getJawupayId();
     *
     * if (jawupayId != null) {
     * try {
     * logger.info("🔍 Check JawuPay pour transaction {} (jawupayId: {})",
     * transactionEnCours.getId(), jawupayId);
     *
     * CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
     * null,
     * jawupayId,
     * jawupayAccess);
     *
     * CommonResponseWs checkResponse = this.check(checkRequest);
     * logger.debug("La reponse de Jawupay:{}", checkResponse);
     *
     * if (checkResponse != null) {
     * if (checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
     * // Paiement réussi chez JawuPay
     * logger.info("✅ JawuPay confirme succès pour jawupayId: {}", jawupayId);
     *
     * boolean isPaid = (responseStatusUtils
     * .adapterStatus(WsOperationResponseStatus.success))
     * .equals(checkResponse.getData());
     *
     * try {
     * this.traiterCallbackPaiement(jawupayId, isPaid);
     * logger.info("✅ Callback traité pour jawupayId: {}", jawupayId);
     * } catch (OwomiException e) {
     * logger.error("❌ Erreur traitement callback pour jawupayId: {}",
     * jawupayId, e);
     * }
     *
     * } else if
     * (checkResponse.getStatus().equals(WsOperationResponseStatus.pending)
     * ||
     * checkResponse.getStatus().equals(WsOperationResponseStatus.inProcess)) {
     * // Encore pending chez JawuPay → Mettre ECHEC chez nous
     * logger.warn("⏳ JawuPay renvoie PENDING pour jawupayId: {} → Mise en ECHEC",
     * jawupayId);
     *
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ECHEC);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     *
     * transactionEnCours.setStatut(responseStatusUtils
     * .adapterStatus(WsOperationResponseStatus.failed));
     * transactionEnCours.setDateResponse(new Date());
     * transactionCcappRepository.save(transactionEnCours);
     *
     * logger.info("✅ Abonnement {} et transaction {} mis en ECHEC",
     * abonnementEnCours.getId(), transactionEnCours.getId());
     *
     * } else {
     * // Échec chez JawuPay → Mettre ECHEC chez nous
     * logger.info("❌ JawuPay confirme échec pour jawupayId: {}", jawupayId);
     *
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ECHEC);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     *
     * transactionEnCours.setStatut(responseStatusUtils
     * .adapterStatus(WsOperationResponseStatus.failed));
     * transactionEnCours.setDateResponse(new Date());
     * transactionCcappRepository.save(transactionEnCours);
     *
     * logger.info("✅ Abonnement {} et transaction {} mis en ECHEC",
     * abonnementEnCours.getId(), transactionEnCours.getId());
     * }
     * } else {
     * logger.warn(
     * "⚠️ Pas de réponse de JawuPay pour jawupayId: {} → Mise en ECHEC par sécurité"
     * ,
     * jawupayId);
     *
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ECHEC);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     *
     * transactionEnCours.setStatut(
     * responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
     * transactionEnCours.setDateResponse(new Date());
     * transactionCcappRepository.save(transactionEnCours);
     * }
     *
     * } catch (Exception e) {
     * logger.error("❌ Erreur check JawuPay pour jawupayId: {} → Mise en ECHEC",
     * jawupayId,
     * e);
     *
     * // En cas d'erreur, mettre en échec par sécurité
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ECHEC);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     *
     * transactionEnCours.setStatut(
     * responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
     * transactionEnCours.setDateResponse(new Date());
     * transactionCcappRepository.save(transactionEnCours);
     * }
     * } else {
     * // Pas de jawupayId → Mettre en ECHEC
     * logger.warn("⚠️ Pas de jawupayId pour transaction {} → Mise en ECHEC",
     * transactionEnCours.getId());
     *
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ECHEC);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     *
     * transactionEnCours
     * .setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed
     * ));
     * transactionEnCours.setDateResponse(new Date());
     * transactionCcappRepository.save(transactionEnCours);
     * }
     * } else {
     * logger.
     * info("ℹ️ Transaction {} déjà terminée (statut: {}), pas de vérification nécessaire"
     * ,
     * transactionEnCours.getId(), statutTransaction);
     * }
     * } else {
     * // Pas de transaction trouvée → Annuler l'abonnement
     * logger.warn("⚠️ Pas de transaction pour abonnement {} → Annulation",
     * abonnementEnCours.getId());
     *
     * abonnementEnCours.setStatutPaiement(StatutPaiement.ANNULE);
     * abonnementEnCours.setActif(false);
     * abonnementRepository.save(abonnementEnCours);
     * }
     * }
     *
     * logger.info("✅ Nettoyage terminé - Poursuite avec nouvelle formule {}",
     * formuleId);
     * // Continuer l'initialisation avec la nouvelle formule
     *
     * } else {
     * // ============================================
     * // ⚠️ MÊME FORMULE : Vérifier la transaction
     * // ============================================
     * logger.
     * info("⚠️ Même formule détectée - Vérification de la transaction en cours");
     *
     * TransactionsCcapp transactionEnAttente = transactionCcappRepository
     * .findOneByAbonnementId(abonnement.getId());
     *
     * if (transactionEnAttente != null) {
     * String statutTransaction = transactionEnAttente.getStatut();
     * logger.info("Transaction trouvée pour abonnement {} - Statut: {}",
     * abonnement.getId(), statutTransaction);
     *
     * // Vérifier si la transaction est réellement en attente
     * if (statutTransaction != null &&
     * (statutTransaction.equalsIgnoreCase(RequestStatus.INPROGRESS.getValue()) ||
     * statutTransaction.equalsIgnoreCase(RequestStatus.PENDING.getValue()))) {
     *
     * logger.warn("Transaction en attente détectée - TransactionId: {}, Statut: {}"
     * ,
     * transactionEnAttente.getId(), statutTransaction);
     *
     * // ============================================
     * // 🔍 CHECK JAWUPAY AVANT DE BLOQUER
     * // ============================================
     * Long jawupayId = transactionEnAttente.getJawupayId();
     * if (jawupayId != null) {
     * try {
     * logger.info("🔍 Vérification du statut réel du paiement {} auprès de JawuPay"
     * ,
     * jawupayId);
     *
     * CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
     * null,
     * jawupayId,
     * jawupayAccess);
     *
     * CommonResponseWs checkResponse = this.check(checkRequest);
     * logger.debug("La reponse de Jawupay:{}", checkResponse);
     *
     * if (checkResponse != null &&
     * checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {
     *
     * logger.info("✅ Check JawuPay réussi - Traitement du callback");
     *
     * boolean isPaid = (responseStatusUtils
     * .adapterStatus(WsOperationResponseStatus.success))
     * .equals(checkResponse.getData());
     *
     * try {
     * Abonnement abonnementMisAJour = this.traiterCallbackPaiement(
     * jawupayId,
     * isPaid);
     *
     * if (abonnementMisAJour != null &&
     * abonnementMisAJour.getStatutPaiement() == StatutPaiement.PAYE) {
     * logger.info(
     * "✅ Paiement finalisé avec succès - Nouvelle initialisation autorisée");
     * // Le paiement a été traité, continuer normalement
     * } else {
     * logger.info("❌ Le paiement a échoué - Permettre nouvelle tentative");
     * // Le paiement a échoué, permettre une nouvelle tentative
     * }
     * } catch (OwomiException e) {
     * logger.error("❌ Erreur lors du traitement du callback après check", e);
     * }
     * } else {
     * logger.
     * warn("⛔ Échec du check JawuPay - Blocage de la nouvelle initialisation");
     *
     * // Le check a échoué, bloquer la nouvelle tentative
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "Vous avez déjà un paiement en cours pour cette formule. " +
     * "Veuillez finaliser ou annuler votre transaction en cours avant d'en initier une nouvelle."
     * ,
     * 400,
     * Map.of(
     * "abonnementEnAttenteId", abonnement.getId(),
     * "formuleId", abonnement.getFormuleAbonnement().getId(),
     * "commandeId", abonnement.getCommandeId(),
     * "montant",
     * (abonnement.getPrixAbonnement().intValue()
     * calculDuree(abonnement)),
     * "dateCreation", abonnement.getDateCreation(),
     * "idPaiement",
     * abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L,
     * "transactionId", transactionEnAttente.getId(),
     * "statutTransaction", statutTransaction,
     * "referenceTransaction", transactionEnAttente.getReference()));
     * }
     * } catch (Exception e) {
     * logger.error("❌ Erreur lors du check JawuPay pour jawupayId: {}", jawupayId,
     * e);
     *
     * // En cas d'erreur de check, on bloque pour éviter les doublons
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "Vous avez déjà un paiement en cours pour cette formule. " +
     * "Impossible de vérifier le statut. Veuillez réessayer plus tard.",
     * 400,
     * Map.of(
     * "abonnementEnAttenteId", abonnement.getId(),
     * "formuleId", abonnement.getFormuleAbonnement().getId(),
     * "commandeId", abonnement.getCommandeId(),
     * "montant",
     * (abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement)),
     * "dateCreation", abonnement.getDateCreation(),
     * "idPaiement",
     * abonnement.getPaymentId() != null ? abonnement.getPaymentId() : 0L,
     * "transactionId", transactionEnAttente.getId(),
     * "statutTransaction", statutTransaction,
     * "referenceTransaction", transactionEnAttente.getReference(),
     * "checkError", e.getMessage()));
     * }
     * } else {
     * logger.warn(
     * "⚠️ Aucun jawupayId trouvé pour la transaction - Blocage de la nouvelle initialisation"
     * );
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "Vous avez déjà un paiement en cours pour cette formule. " +
     * "Veuillez finaliser ou annuler votre transaction en cours avant d'en initier une nouvelle."
     * ,
     * 400,
     * Map.of(
     * "abonnementEnAttenteId", abonnement.getId(),
     * "formuleId", abonnement.getFormuleAbonnement().getId(),
     * "commandeId", abonnement.getCommandeId(),
     * "montant",
     * (abonnement.getPrixAbonnement().intValue() * calculDuree(abonnement)),
     * "dateCreation", abonnement.getDateCreation(),
     * "transactionId", transactionEnAttente.getId(),
     * "statutTransaction", statutTransaction,
     * "referenceTransaction", transactionEnAttente.getReference()));
     * }
     * } else {
     * // Transaction terminée (SUCCESS/FAIL) → Permettre nouvelle tentative
     * logger.
     * info("✅ Transaction terminée (statut: {}) - Nouvelle tentative autorisée",
     * statutTransaction);
     * }
     * } else {
     * // Pas de transaction trouvée → Permettre nouvelle tentative
     * logger.info(
     * "⚠️ Aucune transaction trouvée pour abonnement en attente - Nouvelle tentative autorisée"
     * );
     * }
     * }
     * }
     *
     * logger.info("✅ Aucun blocage détecté - Poursuite de l'initialisation");
     *
     * // ============================================
     * // INITIALISATION NORMALE DE L'ABONNEMENT
     * // ============================================
     *
     * if (!userStep.equals(StepInSystem.LINKEDIN)) {
     * UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
     * updateRequestWs.setUserStep(StepInSystem.PAIEMENT);
     * userInfosClient.updateUserAndProfile(updateRequestWs,
     * SecurityUtils.getAccessToken(httpServletRequest));
     * }
     *
     * log.info("Traitement de l'achat d'abonnement pour l'utilisateur: {}",
     * userId);
     *
     * try {
     * Abonnement abonnement = initialiserAchatAbonnement(numeroCCParent, userId,
     * formuleId);
     *
     * // ============================================
     * // 🔒 VÉRIFICATION CRITIQUE DU MONTANT
     * // ============================================
     * double montantCalcule =
     * abonnement.getFormuleAbonnement().getPrix().doubleValue() *
     * calculDuree(abonnement);
     *
     * // Vérifier que le montant correspond bien à la formule (protection backend)
     * double prixFormule =
     * abonnement.getFormuleAbonnement().getPrix().doubleValue();
     * int dureeFormule = calculDuree(abonnement);
     * double montantAttendu = prixFormule * dureeFormule;
     *
     * // Tolérance de 0.01 pour les erreurs d'arrondi
     * if (Math.abs(montantCalcule - montantAttendu) > 0.01) {
     * logger.error("❌ ALERTE SÉCURITÉ : Montant incohérent détecté !");
     * logger.error("   Montant calculé: {}, Montant attendu: {}", montantCalcule,
     * montantAttendu);
     * logger.error("   FormuleId: {}, Prix: {}, Durée: {}", formuleId, prixFormule,
     * dureeFormule);
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "Erreur de calcul du montant. Veuillez réessayer.",
     * 400,
     * null);
     * }
     *
     * logger.
     * info("✅ Vérification montant OK - Formule: {}, Prix: {}, Durée: {} mois, Total: {}"
     * ,
     * abonnement.getFormuleAbonnement().getTypeAbonnement(),
     * prixFormule,
     * dureeFormule,
     * montantCalcule);
     *
     * CreatePaymentWs paymentWs = new CreatePaymentWs();
     * paymentWs.setDevise(TypeDevise.XOF.toString());
     *
     * if (userStep.equals(StepInSystem.LINKEDIN) && userProfileWs.getInSystem()) {
     * paymentWs.setTypePayment(TypeOperation.RENOUVELLEMENT.toString());
     * } else {
     * paymentWs.setTypePayment(TypeOperation.PAIEMENT.toString());
     * }
     *
     * paymentWs
     * .setUrlCallback(
     * "https://api-gateway.clubchretien.org/abonnement-service/api/abonnements/callback"
     * );
     * paymentWs.setMessage("Initialisation de paiement de votre abonnement");
     * paymentWs.setMontant(montantCalcule);
     *
     * CommonResponseWs initResp = initPaiement(paymentWs, jawupayAccess);
     * logger.info("Le token jawupay: {}", jawupayAccess);
     *
     * if (initResp == null || initResp.getStatus() == null ||
     * !initResp.getStatus().equals(WsOperationResponseStatus.success)) {
     * logger.
     * error("❌ Échec de l'initialisation du paiement - Réponse invalide de JawuPay"
     * );
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "Erreur lors de l'initialisation du paiement",
     * 500,
     * null);
     * }
     *
     * Long idPaiement = convertToLong(initResp.getData());
     *
     * if (idPaiement == null) {
     * log.error("❌ ID de paiement null reçu de JawuPay - Transaction non créée");
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * "ID de paiement invalide reçu. Veuillez réessayer.",
     * 500,
     * null);
     * }
     *
     * Map<String, Object> resultat = new HashMap<>();
     * String ref = "PYM-" + UUID.randomUUID().toString().toUpperCase();
     *
     * TransactionsCcapp transaction = new TransactionsCcapp();
     * transaction.setDevise(paymentWs.getDevise());
     * transaction.setMontant(BigDecimal.valueOf(paymentWs.getMontant()));
     * transaction.setType(paymentWs.getTypePayment());
     * transaction.setCallbackUrl(paymentWs.getUrlCallback());
     * transaction.setPartenaireId(paymentWs.getIdPartenaire());
     * transaction.setJawupayId(idPaiement);
     * transaction.setAbonnementId(abonnement.getId());
     * transaction.setStatut(responseStatusUtils.adapterStatus(
     * WsOperationResponseStatus.pending));
     * transaction.setReference(ref);
     * transactionCcappRepository.save(transaction);
     *
     * log.info("✅ Abonnement créé avec succès - ID: {}", abonnement.getId());
     * log.info("✅ Transaction créée avec succès - ID: {}", transaction.getId());
     *
     * resultat.put("idTransaction", transaction.getId());
     * resultat.put("reference", ref);
     * resultat.put("idAbonnement", abonnement.getId());
     * resultat.put("montant",
     * abonnement.getFormuleAbonnement().getPrix().doubleValue() *
     * calculDuree(abonnement));
     * resultat.put("idPaiement", idPaiement);
     * resultat.put("formuleId", formuleId);
     *
     * abonnement.setPaymentId(idPaiement);
     * abonnementRepository.save(abonnement);
     *
     * // ============================================
     * // 📝 MISE À JOUR ARCHIVE PARENT (Fix PaymentId vide)
     * // ============================================
     * ArchiveParent archiveParent = archiveParentRepository
     * .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());
     * if (archiveParent != null) {
     * archiveParent.setPaymentId(idPaiement);
     * archiveParentRepository.save(archiveParent);
     * logger.
     * info("✅ PaymentId mis à jour dans l'archive parrain pour abonnementId: {}",
     * abonnement.getId());
     * }
     *
     * return new CommonResponseWs(
     * WsOperationResponseStatus.success,
     * "Abonnement initialisé avec succès",
     * 200,
     * resultat);
     *
     * } catch (IllegalArgumentException e) {
     * log.error("❌ Erreur de validation lors de l'achat d'abonnement: ", e);
     * return new CommonResponseWs(
     * WsOperationResponseStatus.failed,
     * e.getMessage(),
     * 500,
     * null);
     * } catch (Exception e) {
     * log.error("❌ Erreur lors du traitement de l'achat d'abonnement: ", e);
     * return new CommonResponseWs(
     * WsOperationResponseStatus.error,
     * "Erreur lors du traitement: " + e.getMessage(),
     * 500,
     * null);
     * }
     * }
     */
    public CommonResponseWs initPaiement(CreatePaymentWs ws, String jawupayToken) {
        CommonResponseWs resp = abonnementClient.initPaiement(ws, jawupayToken);
        if (!resp.getStatus().equals(WsOperationResponseStatus.success))
            return null;
        return resp;
    }

    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Double) {
            return ((Double) value).longValue();
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.error("Impossible de convertir la chaîne en Long: {}", value);
                return null;
            }
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        logger.error("Type non supporté pour la conversion en Long: {}", value.getClass().getName());
        return null;
    }

    /**
     * Initialise l'achat d'un abonnement
     */
    /**
     * Initialise l'achat d'un abonnement avec gestion du parrainage
     *
     * @param telephoneParrain Numéro de téléphone du parrain (optionnel)
     * @param userId           ID de l'utilisateur qui souscrit
     * @param formuleId        ID de la formule d'abonnement choisie
     * @return L'abonnement créé
     * @throws IllegalArgumentException Si la formule est introuvable ou invalide
     * @throws RuntimeException         Si une erreur survient lors de
     *                                  l'initialisation
     */
    public Abonnement initialiserAchatAbonnement(
            Optional<String> telephoneParrain,
            Long userId,
            Long formuleId) {

        log.info("=== Début initialisation abonnement - UserId: {}, FormuleId: {} ===", userId, formuleId);

        // 1. Validation de la formule
        Optional<FormuleAbonnement> formuleOpt = formuleAbonnementService.getFormuleById(formuleId);
        if (formuleOpt.isEmpty()) {
            log.error("Formule d'abonnement introuvable - FormuleId: {}", formuleId);
            throw new IllegalArgumentException("Formule d'abonnement introuvable");
        }

        FormuleAbonnement formule = formuleOpt.get();
        log.info("Formule trouvée - Type: {}, Prix: {}, Durée: {} mois",
                formule.getTypeAbonnement(),
                formule.getPrix(),
                formule.getDuree().getMois());

        try {
            // 2. Validation des règles d'achat (upgrade/downgrade)
            validerAchatAbonnement(userId, formule.getTypeAbonnement());

            // 3. Préparation des données
            String token = CustomJwtUtils.getAccessToken(httpServletRequest);
            String telSaisi = telephoneParrain.orElse("");
            String numeroCCSysteme = "";
            Long parentId = null;

            UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
            ArchiveParent archiveParent = new ArchiveParent();

            // 4. Traitement du parrainage si un numéro est fourni
            if (!telSaisi.isEmpty()) {
                log.info("Traitement du parrainage - Téléphone parrain: {}", telSaisi);

                // 4b. Recherche du parrain par téléphone
                try {
                    CommonResponseWs parentResponse = userInfosClient.getParentByPhone(telSaisi, token);

                    // Si réponse nulle ou échec → continuer sans parrainage (mais archivage prévu)
                    if (parentResponse == null) {
                        log.warn(
                                "⚠️ Réponse nulle lors de la recherche du parrain avec numéro: {} - Continuation sans parrainage",
                                telSaisi);
                    } else if (!WsOperationResponseStatus.success.equals(parentResponse.getStatus())) {
                        log.warn("⚠️ Parrain non trouvé avec le numéro: {} (Statut: {}) - Continuation sans parrainage",
                                telSaisi, parentResponse.getStatus());
                    } else if (parentResponse.getData() == null) {
                        log.warn("⚠️ Données parrain nulles pour le numéro: {} - Continuation sans parrainage",
                                telSaisi);
                    } else {
                        // 4c. Extraction sécurisée des données du parrain
                        LinkedHashMap<String, Object> parentData = (LinkedHashMap<String, Object>) parentResponse
                                .getData();

                        // Extraction du parentId avec cast sécurisé
                        Object idObj = parentData.get("id");
                        if (idObj == null) {
                            log.warn(
                                    "⚠️ ID du parrain absent dans la réponse pour numéro: {} - Continuation sans parrainage",
                                    telSaisi);
                        } else {
                            if (idObj instanceof Number) {
                                parentId = ((Number) idObj).longValue();
                            } else {
                                try {
                                    parentId = Long.parseLong(idObj.toString());
                                } catch (NumberFormatException e) {
                                    log.warn(
                                            "⚠️ Impossible de convertir l'ID parrain en Long: {} - Continuation sans parrainage",
                                            idObj);
                                }
                            }

                            // Continuer seulement si parentId a été extrait avec succès
                            if (parentId != null) {
                                // Extraction du numéro CC système
                                numeroCCSysteme = (String) parentData.get("numeroCC");
                                String displayName = (String) parentData.get("displayName");

                                log.info("Parrain trouvé - ID: {}, NumeroCC: {}, Nom: {}",
                                        parentId, numeroCCSysteme, displayName);

                                // 4d. Validation des données du parrain
                                if (numeroCCSysteme == null || numeroCCSysteme.isEmpty()) {
                                    log.warn(
                                            "⚠️ Numéro CC système absent pour le parentId: {} - Continuation sans parrainage",
                                            parentId);
                                    parentId = null;
                                } else if (parentId.equals(userId)) {
                                    // Vérification : l'utilisateur ne peut pas être son propre parrain
                                    // C'est une erreur intentionnelle, on garde l'exception
                                    log.error("Tentative d'auto-parrainage - UserId: {}", userId);
                                    throw new IllegalArgumentException("Vous ne pouvez pas être votre propre parrain");
                                } else {
                                    // 4e. Mise à jour du profil utilisateur avec les infos du parrain
                                    updateRequestWs.setParentId(parentId);
                                    updateRequestWs.setParentCCNumber(numeroCCSysteme);

                                    CommonResponseWs updateResponse = abonnementClient.updateUser(updateRequestWs,
                                            token);

                                    if (updateResponse == null ||
                                            !WsOperationResponseStatus.success.equals(updateResponse.getStatus())) {
                                        log.warn(
                                                "⚠️ Échec mise à jour profil avec parrain (Statut: {}) - Continuation sans parrainage",
                                                updateResponse != null ? updateResponse.getStatus() : "null");
                                        parentId = null;
                                        numeroCCSysteme = "";
                                    } else {
                                        log.info("✅ Profil utilisateur mis à jour avec le parrain");
                                    }
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Re-lancer les erreurs de validation (ex: auto-parrainage)
                    throw e;
                } catch (Exception e) {
                    log.warn("⚠️ Erreur lors du traitement du parrainage: {} - Continuation sans parrainage",
                            e.getMessage());
                    parentId = null;
                    numeroCCSysteme = "";
                }
            }

            // Log final du statut du parrainage
            if (parentId != null && !numeroCCSysteme.isEmpty()) {
                log.info("✅ Parrainage configuré avec succès - ParentId: {}", parentId);
            } else if (!telSaisi.isEmpty()) {
                log.info("ℹ️ Parrainage orphelin détecté - Numéro {} sera archivé pour correction", telSaisi);
            } else {
                log.info("ℹ️ Abonnement sans parrainage");
            }

            // 5. Génération de l'ID de commande unique
            Long commandeId = genererCommandeId();
            log.info("Commande générée - ID: {}", commandeId);

            // 6. Création de l'abonnement
            Abonnement abonnement = new Abonnement(formule, formule.getPrix(), userId, commandeId);

            // 7. Calcul et assignation des dates
            LocalDateTime maintenant = LocalDateTime.now();
            abonnement.setDateAchat(maintenant);
            abonnement.setDateDebut(maintenant);
            abonnement.setDateFin(maintenant.plusMonths(formule.getDuree().getMois()));

            log.info("Dates calculées - Achat: {}, Début: {}, Fin: {}",
                    abonnement.getDateAchat(),
                    abonnement.getDateDebut(),
                    abonnement.getDateFin());

            // 8. Sauvegarde de l'abonnement
            abonnement = abonnementRepository.save(abonnement);
            log.info("✅ Abonnement sauvegardé - ID: {}", abonnement.getId());

            // 9. Archivage de la relation de parrainage (Systématique si numéro fourni)
            if (!telSaisi.isEmpty()) {
                archiveParent.setNumeroCCParent(telSaisi);
                archiveParent.setAbonnementId(abonnement.getId());
                // paymentId sera mis à jour plus tard dans initializer()
                archiveParentRepository.save(archiveParent);

                log.info("✅ Relation parrainage archivée - AbonnementId: {}, Numéro: {}",
                        abonnement.getId(), telSaisi);


                // Mise à jour finale du profil si le parrain a été identifié
                if (parentId != null && !numeroCCSysteme.isEmpty()) {
                    updateRequestWs.setParentCCNumber(numeroCCSysteme);
                    updateRequestWs.setParentId(parentId);
                    userInfosClient.updateUserAndProfile(updateRequestWs, token);
                }
            }

            log.info("=== Fin initialisation abonnement - AbonnementId: {} ===", abonnement.getId());
            return abonnement;

        } catch (IllegalArgumentException e) {
            // Erreurs de validation métier - ne pas wrapper
            log.error("❌ Erreur de validation lors de l'initialisation de l'abonnement", e);
            throw e;

        } catch (Exception e) {
            // Erreurs techniques - wrapper dans RuntimeException
            log.error("❌ Erreur technique lors de l'initialisation de l'abonnement", e);
            throw new RuntimeException(
                    "Erreur lors de l'initialisation de l'abonnement: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Valide si un utilisateur peut acheter un type d'abonnement
     * 
     * Règles:
     * - Si l'abonnement actuel est SUPÉRIEUR au nouveau type:
     * → Permettre uniquement si l'abonnement actuel expire dans les 5 prochains
     * jours
     * - Si les types sont IDENTIQUES: Permettre (renouvellement/extension)
     * - Si le nouveau type est SUPÉRIEUR: Permettre (upgrade)
     */
    private void validerAchatAbonnement(Long userId, TypeAbonnement nouveauType) {
        Optional<Abonnement> abonnementActuelOpt = getAbonnementActuel(userId);
        if (abonnementActuelOpt.isPresent()) {
            Abonnement abonnementActuel = abonnementActuelOpt.get();
            TypeAbonnement typeActuel = abonnementActuel.getFormuleAbonnement().getTypeAbonnement();
            if (typeActuel.isEgalA(nouveauType)) {
                logger.info("Renouvellement d'abonnement autorisé pour userId: {} - Type: {}", userId,
                        typeActuel.getLibelle());
                return;
            }
            if (typeActuel.isSuperieurA(nouveauType)) {
                LocalDateTime maintenant = LocalDateTime.now();
                LocalDateTime limiteDowngrade = maintenant.plusDays(5);
                if (abonnementActuel.getDateFin() != null && abonnementActuel.getDateFin().isBefore(limiteDowngrade)) {
                    logger.info("Downgrade autorisé pour userId: {} - Type actuel: {} expire le: {}, nouveau type: {}",
                            userId, typeActuel.getLibelle(), abonnementActuel.getDateFin(), nouveauType.getLibelle());
                    return;
                }
                throw new IllegalArgumentException(
                        "Impossible de passer d'un abonnement " + typeActuel.getLibelle() +
                                " à un abonnement " + nouveauType.getLibelle() +
                                ". Le downgrade n'est autorisé que si votre abonnement actuel expire dans les 5 prochains jours.");
            }
            logger.info("Upgrade autorisé pour userId: {} - De {} vers {}", userId, typeActuel.getLibelle(),
                    nouveauType.getLibelle());
        }
    }

    /**
     * Récupère le type d'abonnement actuel d'un utilisateur
     */
    public Optional<TypeAbonnement> getTypeAbonnementActuel(Long userId) {
        return getAbonnementActuel(userId)
                .map(abonnement -> abonnement.getFormuleAbonnement().getTypeAbonnement());
    }

    /**
     * L'api qui gere l'activation des abonnements
     * 
     * @param paymentId
     * @param success
     * @return
     * @throws OwomiException
     */
    /**
     * Nouvelle méthode : Traite le callback en utilisant les IDs explicitement
     * fournis.
     * Utilise UserInfosClient pour récupérer les données et vérifie le statut chez
     * Jawupay.
     */
    public Abonnement traiterCallbackPaiementParId(Long paymentId, boolean success, Long userId, Long parentId,
            String token, String tokenCCapp) throws OwomiException {
        logger.info("=== Traitement callback par ID - PayId: {}, UserId: {}, ParentId: {} ===", paymentId, userId,
                parentId);

        // 1. Vérification du paiement auprès de Jawupay
        CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(null, paymentId, token);
        try {
            CommonResponseWs checkResponse = this.check(checkRequest);
            if (checkResponse == null || !WsOperationResponseStatus.success.equals(checkResponse.getStatus())) {
                throw new OwomiException("Échec de la vérification Jawupay pour le paiement " + paymentId);
            }
            // Confirmation du statut PAYE
            success = (responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))
                    .equals(checkResponse.getData());
        } catch (Exception e) {
            throw new OwomiException("Erreur lors de la vérification Jawupay: " + e.getMessage());
        }

        // 2. Récupérer l'utilisateur via l'API UserInfosClient
        String tokenCcapp = tokenCCapp;
        CommonResponseWs userRes = userInfosClient.getUserInfos(userId, tokenCcapp);
        logger.debug("Le user infos:{}", userRes);
        if (userRes == null || !WsOperationResponseStatus.success.equals(userRes.getStatus())
                || userRes.getData() == null) {
            throw new IllegalArgumentException("Utilisateur non trouvé via l'API pour l'ID: " + userId);
        }

        Map<String, Object> userData = (Map<String, Object>) userRes.getData();
        UserProfileWs profile = new UserProfileWs();
        profile.setId(userId);
        profile.setDisplayName((String) userData.get("displayName"));
        profile.setEmail((String) userData.get("email"));
        profile.setPhoneFullNumber((String) userData.get("phoneFullNumber"));
        profile.setInSystem((Boolean) userData.get("inSystem"));

        // String firebaseUId = (String) userData.get("firebaseProviderId");
        // if (firebaseUId == null && userData.get("profile") instanceof Map) {
        // firebaseUId = (String) ((Map)
        // userData.get("profile")).get("firebaseProviderId");
        // }

        return finaliserPaiementAbonnement(paymentId, success, profile, null, tokenCcapp, parentId);
    }

    public Abonnement rollbackCallbackTest(Long paymentId, Long parentIdOverride, String tokenCcApp)
            throws OwomiException {
        logger.info("=== Rollback Callback Test - PayId: {} ===", paymentId);

        // 1. Récupérer l'abonnement
        Optional<Abonnement> ab = abonnementRepository.findByPaymentId(paymentId);
        if (ab.isEmpty()) {
            throw new OwomiException("Abonnement non trouvé pour paymentId: " + paymentId);
        }

        Abonnement abonnement = ab.get();

        // 2. Trouve le parent si possible (pour rollback parrainage)
        Long parentId = null;

        if (parentIdOverride != null) {
            parentId = parentIdOverride;
            logger.info("Utilisation du parentId pour rollback fourni explicitement: {}", parentId);
        } else {
            ArchiveParent archiveParent = archiveParentRepository
                    .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());
            if (archiveParent != null && archiveParent.getNumeroCCParent() != null) {
                try {
                    CommonResponseWs response = userInfosClient.getParentByPhone(archiveParent.getNumeroCCParent(),
                            tokenCcApp);
                    if (response != null && response.getData() != null) {
                        LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) response.getData();
                        parentId = ((Number) data.get("id")).longValue();
                    }
                } catch (Exception e) {
                    logger.warn("Impossible de récupérer le parent pour rollback: {}", e.getMessage());
                }
            }
        }

        // 3. Rollback commissions et parrainage
        reglementDeCompteService.rollbackCommissionsAbonnement(abonnement, parentId, tokenCcApp);

        // 4. Mettre à jour l'abonnement
        abonnement.setStatutPaiement(StatutPaiement.ANNULE);
        abonnement.setActif(false);

        // 5. Désactiver membre dans user profile? (Optional, maybe keep as PAIEMENT
        // step)
        // On ne remet pas forcément inSystem à false car ça pourrait casser d'autres
        // choses.

        return abonnementRepository.save(abonnement);
    }

    public Abonnement traiterCallbackPaiement(Long paymentId, boolean success) throws OwomiException {
        return finaliserPaiementAbonnement(paymentId, success);
    }

    public Abonnement traiterCallbackPaiementAvecToken(Long paymentId, boolean success, String tokenParam) throws OwomiException {
        logger.info("════════════════════════════════════════════");
        logger.info("Fonction traiterCallbackPaiement");
        logger.info("════════════════════════════════════════════");

        UserProfileWs userProfileWs = CustomJwtUtils.getCurrentUser();

        // Si appel depuis reinitialiser() avec token (pas de user dans le SecurityContext)
        if (userProfileWs == null && tokenParam != null && !tokenParam.isEmpty()) {
            logger.info("Pas de user dans le contexte - Extraction depuis le token");
            userProfileWs = customJwtUtils.extractUserProfile(tokenParam);
        }

        // Utiliser le token passé en paramètre, sinon récupérer depuis la requête HTTP
        String tokenCcapp = (tokenParam != null && !tokenParam.isEmpty())
                ? tokenParam
                : SecurityUtils.getAccessToken(httpServletRequest);

        // Extraction sécurisée du firebaseUId
        String firebaseUId = null;
        if (tokenCcapp != null && !tokenCcapp.isEmpty()) {
            try {
                firebaseUId = customJwtUtils.getUid(tokenCcapp);
            } catch (Exception e) {
                logger.warn("⚠️ Impossible d'extraire le firebaseUId depuis le token (non bloquant): {}", e.getMessage());
            }
        }

        return finaliserPaiementAbonnement(paymentId, success, userProfileWs, firebaseUId, tokenCcapp, null);
    }

    public Abonnement finaliserPaiementAbonnement(Long paymentId, boolean success) throws OwomiException {
        return finaliserPaiementAbonnement(paymentId, success, null, null, null, null);
    }

    public Abonnement finaliserPaiementAbonnement(Long paymentId, boolean success, UserProfileWs userProfileWs,
            String firebaseUId, String tokenCcapp, Long parentIdOverride) throws OwomiException {
        if (paymentId == null) {
            throw new OwomiException("paymentId obligatoire pour finaliser un abonnement");
        }

        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(paymentId);
        if (transactionsCcapp == null) {
            throw new OwomiException("Transaction introuvable pour paymentId: " + paymentId);
        }

        Long abonnementId = transactionsCcapp.getAbonnementId();
        if (abonnementId == null) {
            throw new OwomiException("Transaction sans abonnement lie pour paymentId: " + paymentId);
        }

        Optional<Abonnement> abonnementOpt = abonnementRepository.findByIdForUpdate(abonnementId);
        if (abonnementOpt.isEmpty()) {
            throw new OwomiException("Abonnement introuvable pour la transaction paymentId: " + paymentId);
        }

        Abonnement abonnement = abonnementOpt.get();
        abonnement.setPaymentId(paymentId);

        if (!success) {
            return marquerPaiementAbonnementEchoue(abonnement, transactionsCcapp, paymentId);
        }

        String statutSucces = responseStatusUtils.adapterStatus(WsOperationResponseStatus.success);
        if (estAbonnementDejaFinalise(abonnement)) {
            if (!statutSucces.equals(transactionsCcapp.getStatut())) {
                marquerTransactionSucces(transactionsCcapp);
            }
            logger.info("Abonnement {} deja finalise pour paymentId {}, aucun rejeu", abonnement.getId(), paymentId);
            return abonnement;
        }

        if (abonnement.getDateDebut() == null || abonnement.getDateFin() == null) {
            LocalDateTime maintenant = LocalDateTime.now();
            abonnement.setDateDebut(maintenant);
            if (abonnement.getFormuleAbonnement() != null && abonnement.getFormuleAbonnement().getDuree() != null) {
                abonnement.setDateFin(maintenant.plusMonths(abonnement.getFormuleAbonnement().getDuree().getMois()));
            }
        }

        abonnement.setStatutPaiement(StatutPaiement.PAYE);
        abonnement.setActif(true);

        boolean gestionReussie = gestionNiveauxService.appliquerGestionNiveau(abonnement);
        if (!gestionReussie) {
            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
            marquerTransactionEchec(transactionsCcapp);
            logger.error("Echec gestion niveau pour abonnement ID: {}", abonnement.getId());
            return abonnementRepository.save(abonnement);
        }

        gererActivationAbonnement(abonnement);
        abonnement.setActif(true);
        marquerTransactionSucces(transactionsCcapp);

        Abonnement abonnementSauvegarde = abonnementRepository.save(abonnement);
        executerEffetsPostFinalisationNonBloquants(abonnementSauvegarde, userProfileWs, firebaseUId, tokenCcapp,
                parentIdOverride);

        logger.info("Finalisation abonnement OK - abonnementId: {}, paymentId: {}, statut: {}, actif: {}",
                abonnementSauvegarde.getId(), paymentId, abonnementSauvegarde.getStatutPaiement(),
                abonnementSauvegarde.getActif());
        return abonnementSauvegarde;
    }

    private Abonnement marquerPaiementAbonnementEchoue(Abonnement abonnement, TransactionsCcapp transactionsCcapp,
            Long paymentId) {
        if (estAbonnementDejaFinalise(abonnement)) {
            logger.warn("Paiement {} signale en echec mais abonnement {} deja finalise, aucun rollback",
                    paymentId, abonnement.getId());
            return abonnement;
        }
        abonnement.setStatutPaiement(StatutPaiement.ECHEC);
        abonnement.setActif(false);
        marquerTransactionEchec(transactionsCcapp);
        return abonnementRepository.save(abonnement);
    }

    private boolean estAbonnementDejaFinalise(Abonnement abonnement) {
        return abonnement != null
                && StatutPaiement.PAYE.equals(abonnement.getStatutPaiement())
                && Boolean.TRUE.equals(abonnement.getActif());
    }

    private void marquerTransactionSucces(TransactionsCcapp transactionsCcapp) {
        transactionsCcapp.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success));
        transactionsCcapp.setDateResponse(new Date());
        transactionCcappRepository.save(transactionsCcapp);
    }

    private void marquerTransactionEchec(TransactionsCcapp transactionsCcapp) {
        transactionsCcapp.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
        transactionsCcapp.setDateResponse(new Date());
        transactionCcappRepository.save(transactionsCcapp);
    }

    private void executerEffetsPostFinalisationNonBloquants(Abonnement abonnement, UserProfileWs userProfileWs,
            String firebaseUId, String tokenCcapp, Long parentIdOverride) {
        try {
            notifierPaiementEtBienvenue(abonnement, userProfileWs, firebaseUId, tokenCcapp);
        } catch (Exception e) {
            logger.warn("Effets notification abonnement ignores pour abonnement {}: {}", abonnement.getId(),
                    e.getMessage());
        }

        try {
            mettreAJourProfilApresPaiement(userProfileWs, tokenCcapp);
        } catch (Exception e) {
            logger.warn("Effets profil utilisateur ignores pour abonnement {}: {}", abonnement.getId(),
                    e.getMessage());
        }

        try {
            traiterParrainageApresPaiement(abonnement, userProfileWs, tokenCcapp, parentIdOverride);
        } catch (Exception e) {
            logger.warn("Effets parrainage/commission ignores pour abonnement {}: {}", abonnement.getId(),
                    e.getMessage());
        }
    }

    private void notifierPaiementEtBienvenue(Abonnement abonnement, UserProfileWs userProfileWs, String firebaseUId,
            String tokenCcapp) {
        if (!hasText(firebaseUId) || !hasText(tokenCcapp)) {
            return;
        }
        String displayName = userProfileWs != null && hasText(userProfileWs.getDisplayName())
                ? userProfileWs.getDisplayName()
                : "Utilisateur";
        String message = String.format(PAYMENT_SUCCESS_MESSAGE_TEMPLATE, abonnement.getPrixAbonnement(),
                abonnement.getDateDebut(), abonnement.getDateFin(), abonnement.getPaymentId());
        notificationUtils.notificationInCCapp(firebaseUId, PAYMENT_SUCCESS_TITLE, message, tokenCcapp);

        String welcomeMessage = String.format(WELCOME_MESSAGE_TEMPLATE, displayName,
                abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle());
        notificationUtils.notificationInCCapp(firebaseUId, WELCOME_TITLE, welcomeMessage, tokenCcapp);
    }

    private void mettreAJourProfilApresPaiement(UserProfileWs userProfileWs, String tokenCcapp) {
        if (userProfileWs == null || !hasText(tokenCcapp)) {
            return;
        }
        UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
        updateRequestWs.setUserStep(StepInSystem.LINKEDIN);
        updateRequestWs.setInSystem(true);
        userInfosClient.updateUserAndProfile(updateRequestWs, tokenCcapp);
    }

    private void traiterParrainageApresPaiement(Abonnement abonnement, UserProfileWs userProfileWs, String tokenCcapp,
            Long parentIdOverride) throws OwomiException {
        Long parentId = parentIdOverride;
        String numeroCCParent = null;

        ArchiveParent archiveParent = archiveParentRepository.findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());
        if (archiveParent != null) {
            numeroCCParent = archiveParent.getNumeroCCParent();
        }

        if (parentId == null && hasText(numeroCCParent) && hasText(tokenCcapp)) {
            CommonResponseWs response = userInfosClient.getParentByPhone(numeroCCParent, tokenCcapp);
            if (response != null && response.getData() instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) response.getData();
                Object parentIdValue = data.get("id");
                if (parentIdValue instanceof Number) {
                    parentId = ((Number) parentIdValue).longValue();
                }
            }
        }

        if (parentId != null) {
            reglementDeCompteService.traiterCommissionsAbonnement(abonnement, parentId, tokenCcapp);
        } else if (userProfileWs != null || !hasText(tokenCcapp)) {
            reglementDeCompteService.creeMembre(abonnement, tokenCcapp);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
    private Abonnement exectuterTraitementCallbackCommun(Long paymentId, boolean success, UserProfileWs userProfileWs,
            String firebaseUId, String tokenCcapp, Long parentIdOverride) throws OwomiException {

        logger.debug("========================TRAITEMENT DU CALLBACK (COMMUN)================");
        if (userProfileWs == null)
            return null;

        StepInSystem userStep = userProfileWs.getUserStep();
        if (userStep == null || !userStep.equals(StepInSystem.PAIEMENT)) {
            userStep = StepInSystem.PAIEMENT;
        }

        UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(paymentId);
        if (transactionsCcapp == null)
            return null;

        Long commandeId = transactionsCcapp.getAbonnementId();
        if (commandeId == null) {
            throw new IllegalArgumentException(
                    "L'ID d'abonnement est null dans la transaction (jawupayId: " + paymentId + ")");
        }
        Optional<Abonnement> abonnementOpt = abonnementRepository.findById(commandeId);
        if (abonnementOpt.isEmpty()) {
            throw new IllegalArgumentException("Abonnement introuvable pour la commande: " + commandeId);
        }

        Abonnement abonnement = abonnementOpt.get();
        abonnement.setPaymentId(paymentId);

        if (success) {
            String message = String.format(PAYMENT_SUCCESS_MESSAGE_TEMPLATE, abonnement.getPrixAbonnement(),
                    abonnement.getDateDebut(), abonnement.getDateFin(), abonnement.getPaymentId());

            if (firebaseUId != null) {
                try {
                    notificationUtils.notificationInCCapp(firebaseUId, PAYMENT_SUCCESS_TITLE, message, tokenCcapp);
                } catch (Exception e) {
                    logger.warn("⚠️ Échec envoi notification paiement réussi (non bloquant): {}", e.getMessage());
                }
            }

            abonnement.setStatutPaiement(StatutPaiement.PAYE);
            abonnement.setActif(true);

            Boolean inSystem = userProfileWs.getInSystem();
            if (inSystem == null || !inSystem) {
                updateRequestWs.setUserStep(StepInSystem.LINKEDIN);
                updateRequestWs.setInSystem(true);
                // Appel API uniquement si token disponible (si token est vide, c'est un appel
                // système, on update peut-être manuellement ou on skip)
                if (tokenCcapp != null && !tokenCcapp.isEmpty()) {
                    try {
                        userInfosClient.updateUserAndProfile(updateRequestWs, tokenCcapp);
                        logger.info("✅ Profil utilisateur mis à jour avec succès");
                    } catch (Exception e) {
                        logger.warn("⚠️ Impossible de mettre à jour le profil utilisateur (non bloquant): {}",
                                e.getMessage());
                        // Ne pas bloquer le traitement du callback pour ça
                    }
                }
            } else if (Boolean.TRUE.equals(inSystem)) {
                updateRequestWs.setUserStep(StepInSystem.LINKEDIN);
                if (tokenCcapp != null && !tokenCcapp.isEmpty()) {
                    try {
                        userInfosClient.updateUserAndProfile(updateRequestWs, tokenCcapp);
                        logger.info("✅ Profil utilisateur mis à jour avec succès");
                    } catch (Exception e) {
                        logger.warn("⚠️ Impossible de mettre à jour le profil utilisateur (non bloquant): {}",
                                e.getMessage());
                        // Ne pas bloquer le traitement du callback pour ça
                    }
                }
            }

            if (abonnement.getDateDebut() == null || abonnement.getDateFin() == null) {
                logger.warn("Dates NULL détectées, recalcul...");
                LocalDateTime maintenant = LocalDateTime.now();
                abonnement.setDateDebut(maintenant);
                abonnement.setDateFin(maintenant.plusMonths(
                        abonnement.getFormuleAbonnement().getDuree().getMois()));
            }

            logger.info("Abonnement ID {} - StatutPaiement: PAYE, Actif: true", abonnement.getId());
            boolean gestionReussie = gestionNiveauxService.appliquerGestionNiveau(abonnement);

            if (gestionReussie) {
                logger.debug("========================Si ABONNEMENT EST BON ACTIF ================");
                gererActivationAbonnement(abonnement);
                this.updateTransaction(paymentId);

                // Détermination du parrain
                Long parentId = null;
                String parentName = "";
                String firebaseIDParent = "";
                String numeroCCParent = null;
                String parentSource = "none";

                if (parentIdOverride != null) {
                    log.info("Parent override fourni: {}", parentIdOverride);
                    parentId = parentIdOverride;
                    parentSource = "override";
                    // On pourrait fetcher le nom du parent ici si besoin pour la notif, mais c'est
                    // secondaire
                } else {
                    ArchiveParent archiveParent = archiveParentRepository
                            .findFirstByAbonnementIdOrderByIdDesc(abonnement.getId());
                    logger.debug("L'archive est:{}", archiveParent);
                    if (archiveParent != null) {
                        logger.debug("========================ARCHIVE PARENT EST TROUVEE================");
                        numeroCCParent = archiveParent.getNumeroCCParent();
                        logger.info("Archive parent numeroCC: {}", numeroCCParent);
                        if (numeroCCParent != null && !numeroCCParent.isEmpty()) {
                            CommonResponseWs response = userInfosClient.getParentByPhone(numeroCCParent, tokenCcapp);
                            if (response == null) {
                                logger.warn("Parent lookup null pour numeroCC: {}", numeroCCParent);
                            } else {
                                logger.info("Parent lookup status: {}", response.getStatus());
                                if (response.getData() != null) {
                                    LinkedHashMap<String, Object> data = (LinkedHashMap<String, Object>) response.getData();
                                    parentId = ((Number) data.get("id")).longValue();
                                    parentName = ((String) data.get("displayName"));
                                    parentSource = "archive";
                                } else {
                                    logger.warn("Parent introuvable pour numeroCC: {}", numeroCCParent);
                                }
                            }
                            updateRequestWs.setParentCCNumber(numeroCCParent);
                        } else {
                            logger.warn("Archive parent sans numeroCC pour abonnementId: {}", abonnement.getId());
                        }
                    } else {
                        logger.warn("Archive parent introuvable pour abonnementId: {}", abonnement.getId());
                    }
                }

                logger.info("Parrain retenu pour commissions: source={}, parentId={}, parentName={}, numeroCC={}",
                        parentSource, parentId, parentName, numeroCCParent);

                if (parentId != null) {
                    logger.info("ParentId final pour traitement commissions: {}", parentId);
                    // On passe le token (même si vide/null, le service update devra gérer)
                    // TODO: Si token est vide, ReglementDeCompteService peut échouer sur des appels
                    // API qui nécessitent un TOKEN UTILISATEUR.
                    // Pour un traitement système, il faudrait un token admin ou système.
                    reglementDeCompteService.traiterCommissionsAbonnement(abonnement, parentId, tokenCcapp);

                    // Notif parent
                    /**
                     * Reccuperer le numero a l'aide de userInfos(userID, tokenCCApp)
                     * Ensuite recuperer le partenaire avec le client de mobile
                     * ensuite recuperer le firebasetoken de la reponse
                     * Enfin envoyer la notification
                     */
                //Récupérer les infos du parrain depuis la base
                    // Notif parent - On utilise directement numeroCCParent déjà disponible
                    if (numeroCCParent != null && !numeroCCParent.isEmpty()) {
                        try {
                            logger.info("📤 Tentative notification parrain via numéro: {}", numeroCCParent);
                            logger.info("🔍 DEBUG tokenCcapp dans notif parrain: {}", tokenCcapp != null ? "présent longueur=" + tokenCcapp.length() : "NULL");

                            CommonResponseWs firebaseUID = mobileServiceClient.getFirebaseUID(numeroCCParent, tokenCcapp);
                            String parentFirebaseUID = (String) firebaseUID.getData();
                            logger.info("🔍 parentFirebaseUID: {}", parentFirebaseUID);

                            if (parentFirebaseUID != null) {
                                CommonResponseWs commonResponseWs = mobileAuthenticationServiceClient
                                        .getPartenaire(parentFirebaseUID, tokenCcapp);

                                // LOG TEMPORAIRE
                                logger.info("🔍 DEBUG getPartenaire status: {}", commonResponseWs != null ? commonResponseWs.getStatus() : "NULL");
                                logger.info("🔍 DEBUG getPartenaire message: {}", commonResponseWs != null ? commonResponseWs.getMessage() : "NULL");
                                logger.info("🔍 DEBUG getPartenaire data: {}", commonResponseWs != null ? commonResponseWs.getData() : "NULL");

                                String firebaseToken = null;
                                if (commonResponseWs != null && commonResponseWs.getData() != null) {
                                    if (commonResponseWs.getData() instanceof Map) {
                                        Map<String, Object> partenaireData = (Map<String, Object>) commonResponseWs.getData();
                                        firebaseToken = (String) partenaireData.get("firebasetoken");
                                    } else if (commonResponseWs.getData() instanceof Partenaire) {
                                        firebaseToken = ((Partenaire) commonResponseWs.getData()).getFirebasetoken();
                                    }
                                }

                                if (firebaseToken != null) {
                                    String messageParent = String.format(NEW_MEMBER_PARENT_MESSAGE_TEMPLATE,
                                            userProfileWs.getDisplayName(), numeroCCParent, parentName);
                                    try {
                                        notificationUtils.notificationInCCapp(parentFirebaseUID, NEW_MEMBER_PARENT_TITLE,
                                                messageParent, firebaseToken);
                                        logger.info("✅ NOTIFICATION PARRAIN ENVOYÉE - ParentId: {}, FirebaseUID: {}",
                                                parentId, parentFirebaseUID);
                                    } catch (Exception e) {
                                        logger.error("❌ NOTIFICATION PARRAIN ÉCHOUÉE - ParentId: {}, Erreur: {}",
                                                parentId, e.getMessage());
                                    }
                                } else {
                                    logger.warn("⚠️ Pas de firebaseToken → notification ignorée - ParentId: {}", parentId);
                                }
                            } else {
                                logger.warn("⚠️ Pas de firebaseUID → notification ignorée - ParentId: {}", parentId);
                            }
                        } catch (Exception notifEx) {
                            logger.warn("⚠️ Erreur envoi notification parrain (non bloquant): {}", notifEx.getMessage());
                        }
                    } else {
                        logger.warn("⚠️ numeroCCParent vide → notification parrain ignorée");
                    }
                } else {
                    logger.debug("========================PAS DE PARENT================");
                    boolean isOK = this.reglementDeCompteService.creeMembre(abonnement, tokenCcapp);
                    logger.debug("Creation de membre:{}", isOK);
                }

            abonnement.setActif(true);
            updateRequestWs.setInSystem(true);
            if (tokenCcapp != null && !tokenCcapp.isEmpty()) {
                userInfosClient.updateUserAndProfile(updateRequestWs, tokenCcapp);
            }

            abonnement = abonnementRepository.save(abonnement);
            String messageWelcome = String.format(WELCOME_MESSAGE_TEMPLATE, userProfileWs.getDisplayName(),
                    abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle());

            if (firebaseUId != null) {
                try {
                    notificationUtils.notificationInCCapp(firebaseUId, WELCOME_TITLE, messageWelcome, tokenCcapp);
                } catch (Exception e) {
                    logger.warn("⚠️ Échec envoi notification bienvenue (non bloquant): {}", e.getMessage());
                }
            }

            logger.info("SUCCÈS FINAL - Abonnement ID: {}, Actif: {}, Statut: {}",
                    abonnement.getId(), abonnement.getActif(), abonnement.getStatutPaiement());
        } else {
            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
            logger.error("Échec gestion niveau pour abonnement ID: {}", abonnement.getId());
        }
        } else {
            abonnement.setStatutPaiement(StatutPaiement.ECHEC);
            abonnement.setActif(false);
        }
        return abonnementRepository.save(abonnement);
    }

    /**
     * Gère l'activation d'un abonnement selon les règles métier
     */

    public void gererActivationAbonnement(Abonnement nouvelAbonnement) throws OwomiException {
        Long userId = nouvelAbonnement.getUserId();
        TypeAbonnement nouveauType = nouvelAbonnement.getFormuleAbonnement().getTypeAbonnement();

        Optional<Abonnement> abonnementActuel = getAbonnementActuelExcluant(userId, nouvelAbonnement.getId());

        if (abonnementActuel.isEmpty()) {
            // Pas d'abonnement en cours => date d'achat comme date de début
            nouvelAbonnement.setDateDebutPersonnalisee(nouvelAbonnement.getDateAchat());
            logger.info("Aucun abonnement actuel, démarrage immédiat");

        } else {
            Abonnement actuel = abonnementActuel.get();
            TypeAbonnement typeActuel = actuel.getFormuleAbonnement().getTypeAbonnement();

            logger.info("Gestion activation - Ancien type: {}, Nouveau type: {}",
                    typeActuel, nouveauType);

            if (nouveauType.isInferieurA(typeActuel)) {
                // Abonnement inférieur => date d'achat comme date de début
                nouvelAbonnement.setDateDebutPersonnalisee(nouvelAbonnement.getDateAchat());

                actuel.setActif(false);
                abonnementRepository.save(actuel);
                logger.info("Downgrade - Ancien abonnement ID {} désactivé", actuel.getId());

            } else if (nouveauType.isEgalA(typeActuel)) {
                // FIX : Abonnement identique => date de fin de l'actuel comme date de début
                nouvelAbonnement.setDateDebutPersonnalisee(actuel.getDateFin());

                String tokenCcapp = SecurityUtils.getAccessToken(httpServletRequest);
                String firebaseUId = null;
                String displayName = "Utilisateur";

                try {
                    if (hasText(tokenCcapp)) {
                        firebaseUId = customJwtUtils.getUid(tokenCcapp);
                    }
                } catch (Exception e) {
                    logger.warn("Impossible d'extraire le firebase UID pour notification renouvellement: {}",
                            e.getMessage());
                }
                try {
                    UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                    if (currentUser != null && hasText(currentUser.getDisplayName())) {
                        displayName = currentUser.getDisplayName();
                    }
                } catch (Exception e) {
                    logger.debug("Aucun utilisateur JWT disponible pour notification renouvellement");
                }
                String message = String.format(RENEWAL_SUCCESS_MESSAGE_TEMPLATE,
                            nouvelAbonnement.getFormuleAbonnement(), nouvelAbonnement.getDateDebut(), displayName);
                    try {
                        notificationUtils.notificationInCCapp(firebaseUId, RENEWAL_SUCCESS_TITLE, message, tokenCcapp);
                    } catch (Exception e) {
                    logger.warn("⚠️ Échec envoi notification renouvellement (non bloquant): {}", e.getMessage());
                }

                // AJOUT CRITIQUE : Désactiver l'ancien abonnement !
                actuel.setActif(false);
                abonnementRepository.save(actuel);
                logger.info("Renouvellement - Ancien abonnement ID {} désactivé, nouveau démarre le {}",
                        actuel.getId(), actuel.getDateFin());

            } else {
                // Abonnement supérieur => upgrade immédiat
                nouvelAbonnement.setDateDebutPersonnalisee(nouvelAbonnement.getDateAchat());
                actuel.setActif(false);
                abonnementRepository.save(actuel);
                logger.info("Upgrade - Ancien abonnement ID {} désactivé", actuel.getId());
            }
        }

        // IMPORTANT : Confirmation finale que le nouvel abonnement est actif
        nouvelAbonnement.setActif(true);
        logger.info("Nouvel abonnement ID {} confirmé actif", nouvelAbonnement.getId());
    }

    public Optional<Abonnement> getAbonnementActuel(Long userId) {
        logger.info(" Recherche abonnement actuel pour userId: {}", userId);

        // Recherche avec critères plus larges pour debug
        List<Abonnement> tousAbonnements = abonnementRepository
                .findByUserIdOrderByDateCreationDesc(userId);

        logger.info(" Total abonnements trouvés: {}", tousAbonnements.size());

        for (Abonnement ab : tousAbonnements) {
            logger.info("  - ID: {}, Actif: {}, Statut: {}, DateFin: {}, Valide: {}",
                    ab.getId(), ab.getActif(), ab.getStatutPaiement(),
                    ab.getDateFin(), ab.isValide());
        }

        // Chercher avec la méthode originale
        Optional<Abonnement> result = abonnementRepository
                .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                        userId,
                        StatutPaiement.PAYE,
                        LocalDateTime.now());

        if (result.isEmpty()) {
            logger.warn("Aucun abonnement actif trouvé avec la requête stricte");

            // Fallback : chercher le dernier abonnement PAYE même s'il est expiré
            result = tousAbonnements.stream()
                    .filter(ab -> ab.getStatutPaiement() == StatutPaiement.PAYE)
                    .filter(ab -> ab.getActif())
                    .findFirst();

            if (result.isPresent()) {
                logger.info("Abonnement PAYE trouvé (peut-être expiré): {}", result.get().getId());
            }
        } else {
            logger.info("Abonnement actif trouvé: {}", result.get().getId());
        }

        return result;
    }

    private Optional<Abonnement> getAbonnementActuelExcluant(Long userId, Long excludeAbonnementId) {
        logger.info(" Recherche abonnement actuel pour userId: {} (excludeId: {})", userId, excludeAbonnementId);

        Optional<Abonnement> result = abonnementRepository
                .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                        userId,
                        StatutPaiement.PAYE,
                        LocalDateTime.now());

        if (result.isPresent()) {
            if (!result.get().getId().equals(excludeAbonnementId)) {
                logger.info("Abonnement actif trouve: {}", result.get().getId());
                return result;
            }
            logger.info("Abonnement actif exclu (id: {})", result.get().getId());
        }

        List<Abonnement> tousAbonnements = abonnementRepository
                .findByUserIdOrderByDateCreationDesc(userId);

        logger.info(" Total abonnements trouves (avant exclusion): {}", tousAbonnements.size());
        for (Abonnement ab : tousAbonnements) {
            logger.info("  - ID: {}, Actif: {}, Statut: {}, DateFin: {}, Valide: {}",
                    ab.getId(), ab.getActif(), ab.getStatutPaiement(),
                    ab.getDateFin(), ab.isValide());
        }

        Optional<Abonnement> filtered = tousAbonnements.stream()
                .filter(ab -> excludeAbonnementId == null || !excludeAbonnementId.equals(ab.getId()))
                .filter(ab -> ab.getStatutPaiement() == StatutPaiement.PAYE)
                .filter(ab -> Boolean.TRUE.equals(ab.getActif()))
                .filter(ab -> ab.getDateFin() != null && ab.getDateFin().isAfter(LocalDateTime.now()))
                .findFirst();

        if (filtered.isEmpty()) {
            logger.warn("Aucun abonnement actif trouve avec la requete filtree");
            filtered = tousAbonnements.stream()
                    .filter(ab -> excludeAbonnementId == null || !excludeAbonnementId.equals(ab.getId()))
                    .filter(ab -> ab.getStatutPaiement() == StatutPaiement.PAYE)
                    .filter(ab -> Boolean.TRUE.equals(ab.getActif()))
                    .findFirst();
            if (filtered.isPresent()) {
                logger.info("Abonnement PAYE trouve (peut-etre expire): {}", filtered.get().getId());
            }
        } else {
            logger.info("Abonnement actif trouve (filtre): {}", filtered.get().getId());
        }

        return filtered;
    }

    /**
     * Récupère l'historique des abonnements d'un utilisateur
     */
    public List<Abonnement> getHistoriqueAbonnements(Long userId) {
        return abonnementRepository.findByUserIdOrderByDateCreationDesc(userId);
    }

    /**
     * Vérifie si un utilisateur a un abonnement actif
     */
    public boolean hasAbonnementActif(Long userId) {
        return abonnementRepository.existsByUserIdAndActifTrueAndStatutPaiementAndDateFinAfter(
                userId,
                StatutPaiement.PAYE,
                LocalDateTime.now());
    }

    /**
     * Récupère les abonnements qui expirent dans les X jours
     */
    public List<Abonnement> getAbonnementsExpirantDans(int jours) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.plusDays(jours);
        return abonnementRepository.findByActifTrueAndStatutPaiementAndDateFinBetweenOrderByDateFinAsc(
                StatutPaiement.PAYE,
                maintenant,
                limite);
    }

    /**
     * Récupère les abonnements expirés
     */
    public List<Abonnement> getAbonnementsExpires() {
        return abonnementRepository.findByActifTrueAndStatutPaiementAndDateFinBeforeOrderByDateFinDesc(
                StatutPaiement.PAYE,
                LocalDateTime.now());
    }

    /**
     * Désactive les abonnements expirés
     */
    public void desactiverAbonnementsExpires() {
        List<Abonnement> expires = getAbonnementsExpires();
        for (Abonnement abonnement : expires) {
            if (abonnement.getActif()) {
                abonnement.setActif(false);
                abonnementRepository.save(abonnement);
            }
        }
    }

    /**
     * Récupère un abonnement par ID de commande
     */
    public Optional<Abonnement> getAbonnementByCommandeId(String commandeId) {
        return abonnementRepository.findByCommandeId(commandeId);
    }

    /**
     * Récupère un abonnement par ID de paiement
     */
    public Optional<Abonnement> getAbonnementByPaymentId(Long paymentId) {
        return abonnementRepository.findByPaymentId(paymentId);
    }

    /**
     * Génère un ID de commande unique
     */
    private Long genererCommandeId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    /**
     * Annule un abonnement en attente
     */
    public void annulerAbonnement(String commandeId) {
        Optional<Abonnement> abonnementOpt = abonnementRepository.findByCommandeId(commandeId);
        if (abonnementOpt.isPresent()) {
            Abonnement abonnement = abonnementOpt.get();
            if (abonnement.getStatutPaiement() == StatutPaiement.EN_ATTENTE) {
                abonnement.setStatutPaiement(StatutPaiement.ANNULE);
                abonnement.setActif(false);
                abonnementRepository.save(abonnement);
            } else {
                throw new IllegalStateException("Impossible d'annuler un abonnement avec le statut: " +
                        abonnement.getStatutPaiement());
            }
        } else {
            throw new IllegalArgumentException("Abonnement introuvable");
        }
    }

    /**
     * Annule un abonnement en attente par ID
     */
    public boolean annulerAbonnementEnAttente(Long abonnementId) {
        Optional<Abonnement> abonnementOpt = abonnementRepository.findById(abonnementId);
        if (abonnementOpt.isPresent()) {
            Abonnement abonnement = abonnementOpt.get();
            if (abonnement.getStatutPaiement() == StatutPaiement.EN_ATTENTE) {
                abonnement.setStatutPaiement(StatutPaiement.ANNULE);
                abonnement.setActif(false);
                abonnementRepository.save(abonnement);
                return true;
            }
        }
        return false;
    }

    /**
     * Obtenir tous les abonnements (pour admin)
     */
    public List<Abonnement> getAllAbonnements() {
        return abonnementRepository.findAll();
    }

    /**
     * Obtenir les abonnements qui expirent dans les prochains jours
     */
    public List<Abonnement> getAbonnementsExpirentBientot(int jours) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dateLimite = maintenant.plusDays(jours);
        return abonnementRepository.findByActifTrueAndDateFinLessThanEqualAndDateFinAfterOrderByDateFinAsc(
                dateLimite,
                maintenant);
    }


    /**
     * Obtenir un abonnement par ID
     */
    public Abonnement getAbonnementById(Long id) {
        return abonnementRepository.findById(id).orElse(null);
    }

    /**
     * Vérifie si un abonnement appartient à un utilisateur spécifique
     */
    public boolean isAbonnementOwnedByUser(Long abonnementId, Long userId) {
        Optional<Abonnement> abonnementOpt = abonnementRepository.findById(abonnementId);
        return abonnementOpt.isPresent() && abonnementOpt.get().getUserId().equals(userId);
    }

    /**
     * Active un abonnement après paiement réussi
     */
    public boolean activerAbonnement(Long abonnementId) throws OwomiException {
        Optional<Abonnement> abonnementOpt = abonnementRepository.findById(abonnementId);
        if (abonnementOpt.isPresent()) {
            Abonnement abonnement = abonnementOpt.get();
            if (abonnement.getStatutPaiement() == StatutPaiement.EN_ATTENTE) {
                abonnement.setStatutPaiement(StatutPaiement.PAYE);
                gererActivationAbonnement(abonnement);
                abonnementRepository.save(abonnement);
                return true;
            }
        }
        return false;
    }

    public CommonResponseWs updateTransaction(Long idPayment) {
        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(idPayment);
        if (transactionsCcapp == null || transactionsCcapp.getId() == null)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Transaction not found", 404, null);
        transactionsCcapp.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success));
        transactionsCcapp.setDateResponse(new Date());
        transactionCcappRepository.save(transactionsCcapp);
        return new CommonResponseWs(WsOperationResponseStatus.success, "Transaction mis à jour avec succès", 200,
                transactionsCcapp.getId());
    }

    public CommonResponseWs check(CallbackCreditRequestWs ws) throws OwomiException {
        return this.abonnementClient.verifierStatutPaiement(ws.getIdPaiement(), ws.getSecretClient(),
                ws.getPartnerClient());
    }

    /**
     * Récupère le parentId depuis la base de données via le service authentication
     * Plus fiable que de le récupérer depuis le JWT qui peut être obsolète
     * 
     * @param userId ID de l'utilisateur
     * @param token  Token d'accès
     * @return Le parentId ou null si non trouvé
     */
    @SuppressWarnings("unchecked")
    private Long getParentIdFromDatabase(Long userId, String token) {
        try {
            CommonResponseWs response = userInfosClient.getUserInfos(userId, token);

            if (response == null || !WsOperationResponseStatus.success.equals(response.getStatus())) {
                logger.warn("Impossible de récupérer les infos utilisateur pour userId: {}", userId);
                return null;
            }

            Object data = response.getData();
            if (data == null) {
                logger.warn("Données utilisateur vides pour userId: {}", userId);
                return null;
            }

            // Le data peut être soit une Map, soit un objet UserInfoWs sérialisé en Map
            if (data instanceof Map) {
                Map<String, Object> userInfo = (Map<String, Object>) data;
                Object profileObj = userInfo.get("profile");

                if (profileObj instanceof Map) {
                    Map<String, Object> profile = (Map<String, Object>) profileObj;
                    Object parentIdObj = profile.get("parentId");

                    if (parentIdObj != null) {
                        if (parentIdObj instanceof Number) {
                            return ((Number) parentIdObj).longValue();
                        } else if (parentIdObj instanceof String) {
                            return Long.parseLong((String) parentIdObj);
                        }
                    }
                }
            }

            logger.warn("ParentId non trouvé dans le profil pour userId: {}", userId);
            return null;

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du parentId pour userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Récupère la liste des filleuls de l'utilisateur connecté dont l'abonnement
     * expire dans 7 jours
     * 
     * @return Liste des filleuls (displayName, grade) avec leurs abonnements
     *         expirant dans les 7 prochains jours
     */
    public List<FilleulAbonnementExpirantDto> getFilleulsAbonnementsExpirantDans7Jours() {
        Long userId = CustomJwtUtils.getCurrentUserId();
        logger.info("Recherche des filleuls avec abonnements expirant dans 7 jours pour userId: {}", userId);

        try {
            // Récupérer tous les filleuls de l'utilisateur connecté
            List<User> filleuls = userRepository.findFilleulsByParentId(userId);

            if (filleuls == null || filleuls.isEmpty()) {
                logger.info("Aucun filleul trouvé pour userId: {}", userId);
                return new ArrayList<>();
            }

            logger.info("Nombre de filleuls trouvés: {}", filleuls.size());

            // Définir la fenêtre de temps (maintenant jusqu'à 7 jours)
            LocalDateTime maintenant = LocalDateTime.now();
            LocalDateTime limite = maintenant.plusDays(7);

            List<FilleulAbonnementExpirantDto> filleulsExpirants = new ArrayList<>();

            // Pour chaque filleul, vérifier son abonnement
            for (User filleul : filleuls) {
                Optional<Abonnement> abonnementOpt = abonnementRepository
                        .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                                filleul.getId(),
                                StatutPaiement.PAYE,
                                maintenant);

                if (abonnementOpt.isPresent()) {
                    Abonnement abonnement = abonnementOpt.get();
                    // Vérifier si l'abonnement expire dans les 7 prochains jours
                    if (abonnement.getDateFin() != null
                            && abonnement.getDateFin().isAfter(maintenant)
                            && abonnement.getDateFin().isBefore(limite)) {

                        // Récupérer le grade du profil
                        String grade = null;
                        if (filleul.getProfile() != null) {
                            grade = filleul.getProfile().getStatutCbo();
                        }

                        // Récupérer le type d'abonnement
                        String typeAbonnement = null;
                        if (abonnement.getFormuleAbonnement() != null
                                && abonnement.getFormuleAbonnement().getTypeAbonnement() != null) {
                            typeAbonnement = abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle();
                        }

                        FilleulAbonnementExpirantDto dto = FilleulAbonnementExpirantDto.builder()
                                .displayName(filleul.getDisplayName())
                                .grade(grade)
                                .userId(filleul.getId())
                                .dateExpiration(abonnement.getDateFin())
                                .typeAbonnement(typeAbonnement)
                                .build();

                        logger.info("Filleul {} (userId: {}, grade: {}) - Abonnement expire le: {}",
                                filleul.getDisplayName(), filleul.getId(), grade, abonnement.getDateFin());
                        filleulsExpirants.add(dto);
                    }
                }
            }

            logger.info("Nombre de filleuls avec abonnements expirant dans 7 jours: {}", filleulsExpirants.size());
            return filleulsExpirants;

        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des filleuls avec abonnements expirants: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * Traite les abonnements expirés quotidiennement
     * Désactive le membre dans le service commission
     */
    public void traiterAbonnementsExpires() {
        logger.info("=== Début traitement des abonnements expirés ===");

        // 1. Récupérer les abonnements expirés actifs
        List<Abonnement> abonnementsExpires = abonnementRepository
                .findByActifTrueAndStatutPaiementAndDateFinBeforeOrderByDateFinDesc(
                        StatutPaiement.PAYE,
                        LocalDateTime.now());

        logger.info("📊 {} abonnements expirés trouvés à traiter", abonnementsExpires.size());

        int succes = 0;
        int echecs = 0;

        for (Abonnement abonnement : abonnementsExpires) {
            try {
                String accountId = "ACC_USER" + abonnement.getUserId();
                logger.info("Traitement expiration pour userId: {} (Account: {})",
                        abonnement.getUserId(), accountId);

                // 2. Appel système pour désactiver le membre dans commission-service
                boolean desactivationReussie = commissionServiceClient.desactiverMembreSysteme(accountId);

                if (desactivationReussie) {
                    // 3. Mettre à jour localement
                    abonnement.setActif(false);
                    abonnementRepository.save(abonnement);
                    succes++;
                    logger.info("✅ Abonnement {} désactivé avec succès", abonnement.getId());
                } else {
                    echecs++;
                    logger.error("❌ Échec désactivation membre {} dans commission-service", accountId);
                }

            } catch (Exception e) {
                echecs++;
                logger.error("❌ Erreur traitement abonnement {}: {}", abonnement.getId(), e.getMessage());
            }
        }

        logger.info("=== Fin traitement: {} succès, {} échecs ===", succes, echecs);
    }

    // ----------- REABONNEMENT
    @SuppressWarnings("unchecked")
    private String getNumeroCCParentFromDatabase(Long userId, String token) {
        try {
            CommonResponseWs response = userInfosClient.getUserInfos(userId, token);
            if (response == null || !WsOperationResponseStatus.success.equals(response.getStatus())) {
                logger.warn("Impossible de recuperer les infos utilisateur pour userId: {}", userId);
                return null;
            }

            Object data = response.getData();
            if (data == null) {
                logger.warn("Donnees utilisateur vides pour userId: {}", userId);
                return null;
            }

            if (data instanceof Map) {
                Map<String, Object> userInfo = (Map<String, Object>) data;
                Object numeroObj = userInfo.get("numeroCCParent");
                if (numeroObj == null) {
                    Object profileObj = userInfo.get("profile");
                    if (profileObj instanceof Map) {
                        Map<String, Object> profile = (Map<String, Object>) profileObj;
                        numeroObj = profile.get("numeroCCParent");
                        if (numeroObj == null) {
                            numeroObj = profile.get("numeroCcParent");
                        }
                    }
                }

                if (numeroObj != null) {
                    return numeroObj.toString();
                }
            }

            logger.warn("NumeroCCParent non trouve dans le profil pour userId: {}", userId);
            return null;
        } catch (Exception e) {
            logger.error("Erreur lors de la recuperation du numeroCCParent pour userId: {}", userId, e);
            return null;
        }
    }

    private String getNumeroCCParentFromPremierAbonnement(Long userId) {
        List<Abonnement> abonnements = abonnementRepository.findByUserIdOrderByDateCreationDesc(userId);
        if (abonnements == null || abonnements.isEmpty()) {
            return null;
        }
        Abonnement premier = abonnements.get(abonnements.size() - 1);
        ArchiveParent archiveParent = archiveParentRepository
                .findFirstByAbonnementIdOrderByIdDesc(premier.getId());
        if (archiveParent == null) {
            return null;
        }
        String numero = archiveParent.getNumeroCCParent();
        if (numero == null || numero.isEmpty()) {
            return null;
        }
        return numero;
    }

    // REABONNEMENT COMPLet

    public CommonResponseWs reinitialiser(Long formuleId) {
        Long userId = CustomJwtUtils.getCurrentUserId();
        String token = httpServletRequest.getHeader("access-token");
        String jawupayAccess = httpServletRequest.getHeader("jawupay-access");

        logger.info("═══════════════════════════════════════════════════════════════════");
        logger.info("DÉBUT RÉINITIALISATION V1 - UserId: {}, FormuleId: {}", userId, formuleId);
        logger.info("═══════════════════════════════════════════════════════════════════");

        // Récupération du userStep depuis la base (pas depuis le JWT)
        StepInSystem userStep;
        try {
            CommonResponseWs userInfosResponse = userInfosClient.getUserInfos(userId, token);
            if (userInfosResponse == null
                    || !userInfosResponse.getStatus().equals(WsOperationResponseStatus.success)
                    || userInfosResponse.getData() == null) {
                logger.error("❌ Impossible de récupérer les infos utilisateur en base");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Impossible de récupérer vos informations. Veuillez réessayer.",
                        500, null);
            }

            Map<String, Object> userData = (Map<String, Object>) userInfosResponse.getData();
            String userStepStr = null;

            // Le userStep est dans le profil
            if (userData.get("profile") instanceof Map) {
                Map<String, Object> profile = (Map<String, Object>) userData.get("profile");
                userStepStr = (String) profile.get("userStep");
            }

            if (userStepStr == null) {
                logger.error("❌ UserStep introuvable en base pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Impossible de vérifier votre statut. Veuillez réessayer.",
                        500, null);
            }

            userStep = StepInSystem.valueOf(userStepStr);
            logger.info("✅ UserStep récupéré depuis la base: {}", userStep);

        } catch (Exception e) {
            logger.error("❌ Erreur récupération userStep: {}", e.getMessage());
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Erreur lors de la vérification de votre statut.",
                    500, null);
        }

        // CONDITION 1 : User doit être LINKEDIN
        if (!StepInSystem.LINKEDIN.equals(userStep)) {
            logger.warn("❌ User {} non éligible - Step: {}", userId, userStep);
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Vous devez être membre du réseau pour vous réabonner",
                    400, null);
        }

        // CONDITION 2 : Vérification et résolution des abonnements EN_ATTENTE
        logger.info("🔍 Vérification des abonnements EN_ATTENTE pour userId: {}", userId);

        // CAS 1 : Abonnement actif → bloquer
        /*if (abonnementRepository.existsByUserIdAndActifTrueAndStatutPaiementAndDateFinAfter(
                userId, StatutPaiement.PAYE, LocalDateTime.now())) {
            logger.warn("❌ User {} a déjà un abonnement actif", userId);
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Vous avez déjà un abonnement actif en cours",
                    400, null);
        }*/

        // CAS 2 : Boucle sur tous les EN_ATTENTE → donner un statut définitif
        List<Abonnement> abonnementsEnAttente = abonnementRepository
                .findByUserIdAndStatutPaiementOrderByDateCreationDesc(userId, StatutPaiement.EN_ATTENTE);

        logger.info("📊 {} abonnement(s) EN_ATTENTE trouvé(s)", abonnementsEnAttente.size());

        while (!abonnementsEnAttente.isEmpty()) {
            logger.info("⏳ {} pending restant(s) - Traitement...", abonnementsEnAttente.size());

            for (Abonnement abonnementEnAttente : abonnementsEnAttente) {
                logger.info("🔄 Traitement abonnement EN_ATTENTE - ID: {}", abonnementEnAttente.getId());

                TransactionsCcapp transaction = transactionCcappRepository
                        .findOneByAbonnementId(abonnementEnAttente.getId());

                if (transaction != null && transaction.getJawupayId() != null) {
                    try {
                        CallbackCreditRequestWs checkRequest = new CallbackCreditRequestWs(
                                null, transaction.getJawupayId(), jawupayAccess);

                        CommonResponseWs checkResponse = this.check(checkRequest);
                        logger.info("Réponse JawuPay pour jawupayId {}: {}", transaction.getJawupayId(), checkResponse);

                        if (checkResponse != null
                                && checkResponse.getStatus().equals(WsOperationResponseStatus.success)) {

                            // JawuPay dit SUCCESS → appeler le callback
                            logger.info("✅ JawuPay SUCCESS → Traitement callback pour jawupayId: {}",
                                    transaction.getJawupayId());
                            boolean isPaid = (responseStatusUtils
                                    .adapterStatus(WsOperationResponseStatus.success))
                                    .equals(checkResponse.getData());
                            try {
                                Abonnement abonnementMisAJour = this.traiterCallbackPaiementAvecToken(
                                        transaction.getJawupayId(), isPaid, token);
                                logger.info("✅ Callback traité pour jawupayId: {}", transaction.getJawupayId());

                                // Si le callback confirme un abonnement actif → bloquer
                                if (abonnementMisAJour != null
                                        && abonnementMisAJour.getStatutPaiement() == StatutPaiement.PAYE
                                        && Boolean.TRUE.equals(abonnementMisAJour.getActif())
                                        && abonnementMisAJour.getDateFin().isAfter(LocalDateTime.now())) {
                                    logger.warn("❌ Paiement confirmé → abonnement actif, blocage");
                                    return new CommonResponseWs(
                                            WsOperationResponseStatus.failed,
                                            "Votre paiement précédent vient d'être confirmé. Vous avez déjà un abonnement actif.",
                                            400, null);
                                }
                            } catch (OwomiException e) {
                                logger.error("❌ Erreur traitement callback pour jawupayId: {} → {}",
                                        transaction.getJawupayId(), e.getMessage());
                            }

                        } else {

                            // JawuPay dit ECHEC → mettre en ECHEC
                            logger.info("❌ JawuPay ECHEC pour jawupayId: {} → Mise en ECHEC",
                                    transaction.getJawupayId());

                            abonnementEnAttente.setStatutPaiement(StatutPaiement.ECHEC);
                            abonnementEnAttente.setActif(false);
                            CommonResponseWs saveAbo = new CommonResponseWs(
                                    WsOperationResponseStatus.success, "Abonnement mis en ECHEC", 200,
                                    abonnementRepository.save(abonnementEnAttente));
                            logger.info("Save abonnement ECHEC: {}", saveAbo.getStatus());

                            transaction.setStatut(
                                    responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
                            transaction.setDateResponse(new Date());
                            CommonResponseWs saveTx = new CommonResponseWs(
                                    WsOperationResponseStatus.success, "Transaction mise en ECHEC", 200,
                                    transactionCcappRepository.save(transaction));
                            logger.info("Save transaction ECHEC: {}", saveTx.getStatus());
                        }

                    } catch (Exception e) {

                        // Erreur check → ECHEC par sécurité
                        logger.error("❌ Erreur check JawuPay pour abonnementId: {} → ECHEC par sécurité: {}",
                                abonnementEnAttente.getId(), e.getMessage());

                        abonnementEnAttente.setStatutPaiement(StatutPaiement.ECHEC);
                        abonnementEnAttente.setActif(false);
                        CommonResponseWs saveAbo = new CommonResponseWs(
                                WsOperationResponseStatus.success, "Abonnement mis en ECHEC", 200,
                                abonnementRepository.save(abonnementEnAttente));
                        logger.info("Save abonnement ECHEC (erreur): {}", saveAbo.getStatus());

                        transaction.setStatut(
                                responseStatusUtils.adapterStatus(WsOperationResponseStatus.failed));
                        transaction.setDateResponse(new Date());
                        CommonResponseWs saveTx = new CommonResponseWs(
                                WsOperationResponseStatus.success, "Transaction mise en ECHEC", 200,
                                transactionCcappRepository.save(transaction));
                        logger.info("Save transaction ECHEC (erreur): {}", saveTx.getStatus());
                    }

                } else {

                    // Pas de transaction → abonnement orphelin, annuler
                    logger.warn("⚠️ Pas de transaction pour abonnementId: {} → Annulation orphelin",
                            abonnementEnAttente.getId());
                    abonnementEnAttente.setStatutPaiement(StatutPaiement.ANNULE);
                    abonnementEnAttente.setActif(false);
                    CommonResponseWs saveAbo = new CommonResponseWs(
                            WsOperationResponseStatus.success, "Abonnement orphelin annulé", 200,
                            abonnementRepository.save(abonnementEnAttente));
                    logger.info("Save abonnement orphelin: {}", saveAbo.getStatus());
                }
            }

            // Recharger la liste
            abonnementsEnAttente = abonnementRepository
                    .findByUserIdAndStatutPaiementOrderByDateCreationDesc(userId, StatutPaiement.EN_ATTENTE);
            logger.info("🔄 Rechargement liste - {} pending restant(s)", abonnementsEnAttente.size());
        }

        logger.info("✅ Liste EN_ATTENTE vide - Poursuite réinitialisation");

        // INITIALISATION DU RÉABONNEMENT
        try {

            // Récupérer le numéro du parrain depuis le premier abonnement
            String numeroCCParent = getNumeroCCParentFromPremierAbonnement(userId);
            if (numeroCCParent == null || numeroCCParent.isEmpty()) {
                numeroCCParent = getNumeroCCParentFromDatabase(userId, token);
            }

            if (numeroCCParent != null && !numeroCCParent.isEmpty()) {
                logger.info("✅ Parrain trouvé pour réabonnement - Numéro: {}", numeroCCParent);
            } else {
                logger.info("ℹ️ Aucun parrain trouvé pour réabonnement userId: {}", userId);
            }

            // Passer le numéro récupéré
            Abonnement abonnement = initialiserAchatAbonnement(
                    Optional.ofNullable(numeroCCParent), userId, formuleId);

            double montantCalcule = abonnement.getFormuleAbonnement().getPrix().doubleValue()
                    * calculDuree(abonnement);

            CreatePaymentWs paymentWs = new CreatePaymentWs();
            paymentWs.setDevise(TypeDevise.XOF.toString());
            paymentWs.setTypePayment(TypeOperation.RENOUVELLEMENT.toString());
            paymentWs.setUrlCallback(
                    "https://api-gateway.clubchretien.org/abonnement-service/api/abonnements/callback");
            paymentWs.setMessage("Réinitialisation de votre abonnement");
            paymentWs.setMontant(montantCalcule);

            CommonResponseWs initResp = initPaiement(paymentWs, jawupayAccess);

            if (initResp == null || !initResp.getStatus().equals(WsOperationResponseStatus.success)) {
                abonnementRepository.delete(abonnement);
                return new CommonResponseWs(WsOperationResponseStatus.failed,
                        "Erreur lors de l'initialisation du paiement", 500, null);
            }

            Long idPaiement;
            String tokenTransaction = null;

            if (initResp.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) initResp.getData();
                idPaiement = convertToLong(data.get("paymentId"));
                tokenTransaction = (String) data.get("transactionToken");
            } else {
                idPaiement = convertToLong(initResp.getData());
            }

            String ref = "PYM-" + UUID.randomUUID().toString().toUpperCase();
            TransactionsCcapp nouvelleTransaction = new TransactionsCcapp();
            nouvelleTransaction.setDevise(paymentWs.getDevise());
            nouvelleTransaction.setMontant(BigDecimal.valueOf(montantCalcule));
            nouvelleTransaction.setType(paymentWs.getTypePayment());
            nouvelleTransaction.setCallbackUrl(paymentWs.getUrlCallback());
            nouvelleTransaction.setJawupayId(idPaiement);
            nouvelleTransaction.setAbonnementId(abonnement.getId());
            nouvelleTransaction.setStatut(responseStatusUtils.adapterStatus(WsOperationResponseStatus.pending));
            nouvelleTransaction.setReference(ref);
            CommonResponseWs saveTx = new CommonResponseWs(
                    WsOperationResponseStatus.success, "Transaction créée", 200,
                    transactionCcappRepository.save(nouvelleTransaction));
            logger.info("✅ Transaction sauvegardée: {}", saveTx.getStatus());

            abonnement.setPaymentId(idPaiement);
            CommonResponseWs saveAbo = new CommonResponseWs(
                    WsOperationResponseStatus.success, "Abonnement mis à jour", 200,
                    abonnementRepository.save(abonnement));
            logger.info("✅ Abonnement sauvegardé: {}", saveAbo.getStatus());

            Map<String, Object> resultat = new HashMap<>();
            resultat.put("idTransaction", nouvelleTransaction.getId());
            resultat.put("reference", ref);
            resultat.put("idAbonnement", abonnement.getId());
            resultat.put("montant", montantCalcule);
            resultat.put("idPaiement", idPaiement);
            resultat.put("formuleId", formuleId);
            if (tokenTransaction != null) resultat.put("tokenTransaction", tokenTransaction);

            logger.info("✅ Réabonnement initialisé - AbonnementId: {}, PaymentId: {}",
                    abonnement.getId(), idPaiement);

            return new CommonResponseWs(WsOperationResponseStatus.success,
                    "Réabonnement initialisé avec succès", 200, resultat);

        } catch (IllegalArgumentException e) {
            logger.error("Erreur de validation: {}", e.getMessage(), e);
            return new CommonResponseWs(WsOperationResponseStatus.failed, e.getMessage(), 400, null);
        } catch (Exception e) {
            logger.error("Erreur inattendue: {}", e.getMessage(), e);
            return new CommonResponseWs(WsOperationResponseStatus.error,
                    "Erreur lors du traitement. Veuillez réessayer.", 500, null);
        }
    }

}
