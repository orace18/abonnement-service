package com.clubchretien.abonnement.abonnement.services;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.configs.JwtConfig;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserProfileWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.UserRepository;
import com.owomi.database.repositories.UserTerminalRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.AbonnementClient;
import com.owomi.rest.client.abonnementClient.AuthenticationRestClient;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.auth.UserDataClient;
import com.owomi.rest.client.commissionService.CommissionServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class ReglementDeCompteService {

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FormuleAbonnementService formuleAbonnementService;

    @Autowired
    private GestionNiveauxService gestionNiveauxService;

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

    @Autowired
    private UserDataClient userDataClient;
    @Autowired
    private SecurityUtils securityUtils;
    @Autowired
    private CustomJwtUtils customJwtUtils;
    @Autowired
    AuthenticationRestClient authenticationRestClient;
    @Autowired
    TransactionCcappRepository transactionCcappRepository;

    @Autowired
    private FirebaseProviderService firebaseProviderService;
    @Value("${clubchretien.prefix}")
    String prefice;
    @Value("${clubchretien.appname}")
    String appName;
    @Value("${clubchretien.appidentifier}")
    String appIdentifier;

    @Autowired
    Mapper mapper;

    private static Logger log = LogManager.getLogger(ReglementDeCompteService.class);

    public boolean creeMembre(Abonnement abonnement, String token) throws OwomiException {
        // ✅ CORRECTION : Utiliser l'userId de l'abonnement
        Long userId = abonnement.getUserId();
        // Le token est désormais passé en paramètre
        String accountId = "ACC_USER" + userId;

        log.debug("Création membre sans parrain - userId: {}, accountId: {}", userId, accountId);

        // ✅ RÉCUPÉRER LE BON PROFIL
        UserProfileWs userProfileWs = this.getUserProfile(userId, token);

        boolean membreCreated = commissionServiceClient.creerMembreNeo4j(
                userId,
                accountId,
                abonnement,
                userProfileWs,
                token);

        if (membreCreated) {
            log.info("✅ Membre créé avec succès: {}", accountId);
            return true;
        } else {
            log.error("❌ Échec création membre: {}", accountId);
            return false;
        }
    }

    public CommonResponseWs traiterCommissionsAbonnementNew(Abonnement abonnement, Long parentId, String token) {
        try {
            log.info("Début traitement commissions pour abonnement ID: {}", abonnement.getId());

            Long userId = abonnement.getUserId();
            String accountId = "ACC_USER" + userId;

            log.debug("L'id numero du compte {}", accountId);
            log.debug("UserId de l'abonnement: {}", userId);

            // Récupération du profil utilisateur
            UserProfileWs userProfileWs = this.getUserProfile(userId, token);
            if (userProfileWs == null) {
                log.error("Impossible de récupérer le profil pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Profil utilisateur introuvable",
                        404,
                        null
                );
            }

            log.info("Création membre Neo4j pour userId: {}, accountId: {}", userId, accountId);

            // Création du membre Neo4j
            boolean membreCreated = commissionServiceClient.creerMembreNeo4j(
                    userId,
                    accountId,
                    abonnement,
                    userProfileWs,
                    token
            );

            if (!membreCreated) {
                log.error("Échec création membre Neo4j pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Échec de la création du membre dans Neo4j",
                        500,
                        null
                );
            }

            // Gestion du parrainage si parentId existe
            if (parentId != null) {
                log.info("ParentId détecté: {}, création relation parrainage", parentId);
                UserProfileWs parentProfile = this.getUserProfile(parentId, token);

                if (parentProfile == null) {
                    log.warn("Profil parent introuvable pour parentId: {}", parentId);
                    // On continue quand même le traitement
                } else {
                    boolean parrainageCreated = commissionServiceClient.creerRelationParrainage(
                            parentProfile,
                            userId,
                            token
                    );

                    if (parrainageCreated) {
                        log.info("✅ Relation parrainage créée avec succès: {} → {}", parentId, userId);
                    } else {
                        log.warn("❌ Échec création relation parrainage: {} → {}", parentId, userId);
                        // On continue le traitement même si le parrainage échoue
                    }
                }
            } else {
                log.info("Aucun parentId fourni, création membre sans parrainage");
            }

            // Déclenchement du calcul des commissions
            log.info("Déclenchement calcul commissions pour abonnement: {}", abonnement.getId());
            CommonResponseWs commissionsCalculees = commissionServiceClient.declencherCalculCommissionsNew(
                    abonnement,
                    accountId,
                    token
            );

            if (commissionsCalculees.getStatus().equals(WsOperationResponseStatus.failed)) {
                log.error("Échec du calcul des commissions pour abonnement: {}", abonnement.getId());
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Échec du calcul des commissions",
                        500,
                        null
                );
            }

            log.info("✅ Traitement commissions terminé avec succès pour userId: {}", userId);

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Commissions traitées avec succès",
                    200,
                    Map.of(
                            "userId", userId,
                            "accountId", accountId,
                            "abonnementId", abonnement.getId(),
                            "parrainageCreated", parentId != null
                    )
            );

        } catch (Exception e) {
            log.error("❌ Erreur traitement commissions pour abonnement: {}", abonnement.getId(), e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.failed,
                    "Erreur lors du traitement des commissions: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    public void traiterCommissionsAbonnement(Abonnement abonnement, Long parentId, String token) {
        try {
            log.info("Début traitement commissions pour abonnement ID: {}", abonnement.getId());

            Long userId = abonnement.getUserId();
            String accountId = "ACC_USER" + userId;

            log.debug("L'id numero du compte {}", accountId);
            log.debug("UserId de l'abonnement: {}", userId);
            UserProfileWs userProfileWs = this.getUserProfile(userId, token);
            log.info("Création membre Neo4j pour userId: {}, accountId: {}", userId, accountId);

            boolean membreCreated = commissionServiceClient.creerMembreNeo4j(
                    userId,
                    accountId,
                    abonnement,
                    userProfileWs,
                    token);
            if (!membreCreated) {
                log.error("Échec création membre Neo4j pour userId: {}", userId);
                return;
            }

            if (parentId != null) {
                log.info("ParentId détecté: {}, création relation parrainage", parentId);
                UserProfileWs parentProfile = this.getUserProfile(parentId, token);
                boolean parrainageCreated = commissionServiceClient.creerRelationParrainage(
                        parentProfile,
                        userId,
                        token);

                if (parrainageCreated) {
                    log.info("✅ Relation parrainage créée avec succès: {} → {}", parentId, userId);
                } else {
                    log.warn("❌ Échec création relation parrainage: {} → {}", parentId, userId);
                }
            } else {
                log.info("Aucun parentId fourni, création membre sans parrainage");
            }

            log.info("Déclenchement calcul commissions pour abonnement: {}", abonnement.getId());
            commissionServiceClient.declencherCalculCommissions(abonnement, accountId, token);
            log.info("✅ Traitement commissions terminé avec succès pour userId: {}", userId);

        } catch (Exception e) {
            log.error("❌ Erreur traitement commissions pour abonnement: {}", abonnement.getId(), e);
        }
    }

    public void rollbackCommissionsAbonnement(Abonnement abonnement, Long parentId, String token) {
        try {
            log.info("Rollback commissions pour abonnement ID: {}", abonnement.getId());

            Long userId = abonnement.getUserId();

            // 1. Annuler les commissions
            commissionServiceClient.annulerCalculCommissions(abonnement, token);

            // 2. Annuler le parrainage si nécessaire
            if (parentId != null) {
                log.info("Annulation parrainage pour parentId: {}", parentId);
                commissionServiceClient.annulerRelationParrainage(parentId, userId, token);
            }

            log.info("Rollback commissions terminé avec succès pour userId: {}", userId);

        } catch (Exception e) {
            log.error("Erreur rollback commissions pour abonnement: {}", abonnement.getId(), e);
        }
    }

    /*
     * public void traiterCommissionsAbonnement(Abonnement abonnement, Long
     * parentId) {
     * try {
     * log.info("Début traitement commissions pour abonnement ID: {}",
     * abonnement.getId());
     * 
     * UserProfileWs userProfileWs = CustomJwtUtils.getCurrentUser();
     * Long userId = userProfileWs.getId();
     * String token = CustomJwtUtils.getAccessToken(httpServletRequest);
     * String accountId = "ACC_USER" + userId;
     * log.debug("L'id numero du compte {}", accountId);
     * boolean membreCreated = commissionServiceClient.creerMembreNeo4j(
     * abonnement.getUserId(),
     * accountId,
     * abonnement,
     * userProfileWs,
     * token
     * );
     * 
     * if (!membreCreated) {
     * log.error("Échec création membre Neo4j");
     * return;
     * }
     * 
     * if (parentId != null) {
     * log.info("ParentId détecté: {}", parentId);
     * boolean parrainageCreated = commissionServiceClient.creerRelationParrainage(
     * this.getUserProfile(parentId),
     * abonnement.getUserId(),
     * token
     * );
     * 
     * if (parrainageCreated) {
     * log.info("Relation parrainage créée avec succès");
     * } else {
     * log.warn("Échec création relation parrainage");
     * }
     * }
     * 
     * commissionServiceClient.declencherCalculCommissions(abonnement, accountId,
     * token);
     * 
     * log.info("Traitement commissions terminé avec succès");
     * 
     * } catch (Exception e) {
     * log.error("Erreur traitement commissions", e);
     * }
     * }
     */
    /**
     * Helper: Récupère le profil utilisateur
     */
    private UserProfileWs getUserProfile(Long userId, String token) {
        // Le token est passé en paramètre

        log.debug("Récupération profil pour userId: {}", userId);

        CommonResponseWs response = userInfosClient.getUserInfos(userId, token);

        if (response != null && response.getStatus().equals(WsOperationResponseStatus.success)) {
            Map<String, Object> data = (Map<String, Object>) response.getData();

            UserProfileWs profile = new UserProfileWs();
            profile.setId(userId);
            profile.setNom((String) data.get("nom"));
            profile.setPrenom((String) data.get("prenom"));
            profile.setEmail((String) data.get("email"));

            // ✅ CORRECTION : Gérer plusieurs clés possibles
            String phone = (String) data.get("phoneFullNumber");
            if (phone == null) {
                phone = (String) data.get("telephone");
            }
            if (phone == null) {
                phone = (String) data.get("phone");
            }
            profile.setPhoneFullNumber(phone);

            // ✅ AJOUTER displayName si disponible
            String displayName = (String) data.get("displayName");
            if (displayName != null) {
                profile.setDisplayName(displayName);
            }

            log.debug("✅ Profil récupéré: {} {}, email: {}, phone: {}",
                    profile.getPrenom(), profile.getNom(), profile.getEmail(), phone);

            return profile;
        }

        log.error("❌ Impossible de récupérer le profil pour userId: {}", userId);
        throw new RuntimeException("Impossible de récupérer le profil utilisateur " + userId);
    }
}
