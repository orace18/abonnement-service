package com.clubchretien.abonnement.abonnement.controllers;

import com.clubchretien.abonnement.abonnement.dto.AbonnementDto;
import com.clubchretien.abonnement.abonnement.dto.AchatAbonnementRequest;
import com.clubchretien.abonnement.abonnement.dto.CallbackV2Request;
import com.clubchretien.abonnement.abonnement.dto.FilleulAbonnementExpirantDto;
import com.clubchretien.abonnement.abonnement.dto.PaymentCallbackRequest;
import com.clubchretien.abonnement.abonnement.dto.StatutAbonnementDto;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.CallbackCreditRequestWs;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserProfileWs;
import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.SubscriptionPaymentReconciliationService;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeAbonnement;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api/abonnements")
@CrossOrigin(origins = "*")
@Tag(name = "Abonnements", description = "API pour la gestion des abonnements")
public class AbonnementController {

        @Autowired
        private AbonnementService abonnementService;
        @Autowired
        private SubscriptionPaymentReconciliationService subscriptionPaymentReconciliationService;
        @Autowired
        ResponseStatusUtils responseStatusUtils;

        @Autowired
        private HttpServletRequest httpServletRequest;

        @GetMapping("/initialiser")
        public CommonResponseWs initializer(
                        @RequestParam Long idFormuleAbonnement,
                        @RequestParam(required = false) String numeroCCParent) {
                CommonResponseWs userInfo = abonnementService.initializer(
                                Optional.ofNullable(numeroCCParent),
                                idFormuleAbonnement);

                return new CommonResponseWs(
                                userInfo.getStatus(),
                                userInfo.getMessage(),
                                userInfo.getCode(),
                                userInfo.getData());
        }

        @GetMapping("/reinitialiser")
        public CommonResponseWs reinitialiser(
                @RequestParam Long idFormuleAbonnement) {

                CommonResponseWs response = abonnementService.reinitialiser(
                        idFormuleAbonnement);

                return new CommonResponseWs(
                        response.getStatus(),
                        response.getMessage(),
                        response.getCode(),
                        response.getData());
        }

        @GetMapping("/get-jawupay-token")
        public CommonResponseWs initAbonnement() {
                return abonnementService.makeCompletLoginRegisterJawuPay();
        }

        /**
         * Endpoint de test pour rafraîchir le token Jawupay
         * Utilise le refresh token stocké en BDD avec fallback Firebase si nécessaire
         */
        @Operation(summary = "Rafraîchir le token Jawupay", description = "Rafraîchit le token Jawupay de l'utilisateur connecté. Utilise le refresh token Jawupay, et en cas d'échec, utilise le refresh token Firebase comme fallback.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Token rafraîchi avec succès"),
                        @ApiResponse(responseCode = "404", description = "Aucun provider trouvé pour l'utilisateur"),
                        @ApiResponse(responseCode = "500", description = "Erreur lors du refresh")
        })
        @GetMapping("/refresh-jawupay-token")
        public CommonResponseWs refreshJawupayToken() {
                try {
                        UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                        Long userId = currentUser.getId();

                        log.info("=== Demande de refresh token Jawupay pour userId: {} ===", userId);

                        return abonnementService.refreshJawupayToken(userId);
                } catch (Exception e) {
                        log.error("Erreur lors du refresh du token Jawupay", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors du refresh du token: " + e.getMessage(),
                                        500,
                                        null);
                }
        }

        /**
         * API pour l'achat d'un abonnement
         */
        @PostMapping("/init-abonnement")
        public CommonResponseWs achatAbonnement(
                        @Valid @RequestBody AchatAbonnementRequest request) {
                try {

                        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
                        Long authenticatedUserId = userProfile.getId();

                        Abonnement abonnement = abonnementService.initialiserAchatAbonnement(
                                        String.valueOf(request.getNumeroCCParent()).describeConstable(),
                                        authenticatedUserId, request.getFormuleId());

                        return new CommonResponseWs(WsOperationResponseStatus.success,
                                        "Abonnement initialisé", 201, abonnement);

                } catch (Exception e) {
                        log.error("Erreur lors de l'initialisation de l'achat", e);
                        return new CommonResponseWs(WsOperationResponseStatus.rejected,
                                        "Erreur lors de l'initialisation de l'achat: " + e.getMessage(), 401, e);
                }
        }

        /**
         * Récupère tous les types d'abonnement disponibles
         * GET /api/abonnements/types
         */
        @GetMapping("/types")
        public CommonResponseWs getAllTypes() {
                List<Map<String, Object>> types = Arrays.stream(TypeAbonnement.values())
                                .map(type -> {
                                        Map<String, Object> typeMap = new HashMap<>();
                                        typeMap.put("name", type.name());
                                        typeMap.put("niveau", type.getNiveau());
                                        typeMap.put("libelle", type.getLibelle());
                                        return typeMap;
                                })
                                .collect(Collectors.toList());

                return new CommonResponseWs(WsOperationResponseStatus.success, "Les types d'abonnement", 200, types);
        }

        /**
         * API pour obtenir l'abonnement actuel de l'utilisateur authentifié
         */
        @GetMapping("/mon-abonnement/actuel")
        public CommonResponseWs getAbonnementActuel() {
                try {
                        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
                        Long authenticatedUserId = userProfile.getId();

                        Optional<Abonnement> abonnementOpt = abonnementService.getAbonnementActuel(authenticatedUserId);
                        if (abonnementOpt.isPresent()) {
                                AbonnementDto abonnementDto = new AbonnementDto(abonnementOpt.get());
                                return new CommonResponseWs(WsOperationResponseStatus.success,
                                                "Abonnement actuel récupéré avec succès", 200, abonnementDto);
                        } else {
                                return new CommonResponseWs(WsOperationResponseStatus.success,
                                                "Aucun abonnement actuel trouvé", 200, null);
                        }
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'abonnement actuel", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération de l'abonnement actuel: " + e.getMessage(), 500,
                                        e);
                }
        }

        /**
         * API pour activer un abonnement (seulement ses propres abonnements)
         */
        @PutMapping("/{abonnementId}/activer")
        public CommonResponseWs activerAbonnement(@PathVariable Long abonnementId) {
                try {
                        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
                        Long authenticatedUserId = userProfile.getId();

                        if (!abonnementService.isAbonnementOwnedByUser(abonnementId, authenticatedUserId)) {
                                return new CommonResponseWs(WsOperationResponseStatus.rejected,
                                                "Vous n'êtes pas autorisé à modifier cet abonnement", 401, null);
                        }

                        boolean success = abonnementService.activerAbonnement(abonnementId);
                        if (success) {
                                return new CommonResponseWs(WsOperationResponseStatus.success,
                                                "Abonnement activé avec succès", 200, null);
                        }

                        return new CommonResponseWs(WsOperationResponseStatus.failed,
                                        "Impossible d'activer l'abonnement", 400, null);
                } catch (Exception e) {
                        log.error("Erreur lors de l'activation de l'abonnement", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur lors de l'activation de l'abonnement: " + e.getMessage(), 500, e);
                }
        }

        /**
         * API pour annuler un abonnement en attente (seulement ses propres abonnements)
         */
        @PutMapping("/{abonnementId}/annuler")
        public CommonResponseWs annulerAbonnement(@PathVariable Long abonnementId) {
                try {
                        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
                        Long authenticatedUserId = userProfile.getId();

                        if (!abonnementService.isAbonnementOwnedByUser(abonnementId, authenticatedUserId)) {
                                return new CommonResponseWs(WsOperationResponseStatus.rejected,
                                                "Vous n'êtes pas autorisé à modifier cet abonnement", 401, null);
                        }

                        boolean success = abonnementService.annulerAbonnementEnAttente(abonnementId);
                        if (success) {
                                return new CommonResponseWs(WsOperationResponseStatus.success,
                                                "Abonnement annulé avec succès", 200, null);
                        }

                        return new CommonResponseWs(WsOperationResponseStatus.failed,
                                        "Impossible d'annuler l'abonnement", 400, null);
                } catch (Exception e) {
                        log.error("Erreur lors de l'annulation de l'abonnement", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur lors de l'annulation de l'abonnement: " + e.getMessage(), 500, e);
                }
        }

        /**
         * API pour récupérer tous les abonnements (admin seulement)
         */
        /*
         * @GetMapping("/admin/tous")
         * public CommonResponseWs getAllAbonnements() {
         * try {
         * List<Abonnement> abonnements = abonnementService.getAllAbonnements();
         * List<AbonnementDto> abonnementsDto = abonnements.stream()
         * .map(AbonnementDto::new)
         * .collect(Collectors.toList());
         * 
         * String message = abonnementsDto.isEmpty() ?
         * "Aucun abonnement trouvé dans le système"
         * : String.
         * format("Liste de tous les abonnements récupérée avec succès (%d abonnement(s))"
         * ,
         * abonnementsDto.size());
         * 
         * return new CommonResponseWs(WsOperationResponseStatus.success, message, 200,
         * abonnementsDto);
         * } catch (Exception e) {
         * log.error("Erreur lors de la récupération de tous les abonnements", e);
         * return new CommonResponseWs(WsOperationResponseStatus.error,
         * "Erreur lors de la récupération de tous les abonnements: " + e.getMessage(),
         * 500, e);
         * }
         * }
         */

        /**
         * API pour obtenir les abonnements qui expirent bientôt (admin seulement)
         */
        @GetMapping("/admin/expirent-bientot")
        public CommonResponseWs getAbonnementsExpirentBientot(
                        @RequestParam(defaultValue = "7") int jours) {
                try {
                        List<Abonnement> abonnements = abonnementService.getAbonnementsExpirentBientot(jours);
                        List<AbonnementDto> abonnementsDto = abonnements.stream()
                                        .map(AbonnementDto::new)
                                        .collect(Collectors.toList());

                        String message = abonnementsDto.isEmpty()
                                        ? String.format("Aucun abonnement n'expire dans les %d prochains jours", jours)
                                        : String.format("%d abonnement(s) expire(nt) dans les %d prochains jours",
                                                        abonnementsDto.size(), jours);

                        return new CommonResponseWs(WsOperationResponseStatus.success, message, 200, abonnementsDto);
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération des abonnements qui expirent bientôt", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération des abonnements qui expirent bientôt: "
                                                        + e.getMessage(),
                                        500, e);
                }
        }

        /**
         * Récupère la liste des filleuls dont l'abonnement expire dans les 7 prochains
         * jours
         *
         * @return Liste des filleuls avec leurs informations d'abonnement
         */
        @Operation(summary = "Liste des filleuls avec abonnements expirant bientôt", description = "Retourne la liste des filleuls de l'utilisateur connecté dont l'abonnement expire dans les 7 prochains jours. "
                        +
                        "Permet aux parrains de suivre les renouvellements à venir de leurs filleuls.", tags = {
                                        "Abonnements", "Filleuls" })
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès (peut être vide)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponseWs.class), examples = {
                                        @ExampleObject(name = "Avec filleuls", summary = "Exemple avec 2 filleuls", value = """
                                                        {
                                                          "status": "success",
                                                          "message": "Liste des filleuls dont l'abonnement expire dans 7 jours",
                                                          "code": 200,
                                                          "data": [
                                                            {
                                                              "userId": 123,
                                                              "displayName": "Jean Dupont",
                                                              "grade": "Bronze",
                                                              "typeAbonnement": "Premium",
                                                              "dateExpiration": "2026-01-22T23:59:59"
                                                            },
                                                            {
                                                              "userId": 456,
                                                              "displayName": "Marie Martin",
                                                              "grade": "Silver",
                                                              "typeAbonnement": "Standard",
                                                              "dateExpiration": "2026-01-20T23:59:59"
                                                            }
                                                          ]
                                                        }
                                                        """),
                                        @ExampleObject(name = "Sans filleuls", summary = "Aucun filleul avec abonnement expirant", value = """
                                                        {
                                                          "status": "success",
                                                          "message": "Aucun filleul pour le moment",
                                                          "code": 200,
                                                          "data": null
                                                        }
                                                        """)
                        })),
                        @ApiResponse(responseCode = "401", description = "Non authentifié - Token JWT manquant ou invalide", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                        {
                                          "status": "error",
                                          "message": "Authentification requise",
                                          "code": 401
                                        }
                                        """))),
                        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des données", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                        {
                                          "status": "error",
                                          "message": "Erreur lors de la récupération des filleuls",
                                          "code": 500
                                        }
                                        """)))
        })
        @GetMapping("/filleuls/abonnements-expirant")
        public CommonResponseWs getFilleulsAbonnementsExpirantDans7Jours() {
                List<FilleulAbonnementExpirantDto> filleuls = abonnementService
                                .getFilleulsAbonnementsExpirantDans7Jours();

                if (filleuls.isEmpty()) {
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.success,
                                        "Aucun filleul pour le moment",
                                        200,
                                        null);
                } else {
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.success,
                                        "Liste des filleuls dont l'abonnement expire dans 7 jours",
                                        200,
                                        filleuls);
                }
        }

        /**
         * API pour obtenir un abonnement par ID (ses propres abonnements ou admin)
         */
        @GetMapping("/{id}")
        public CommonResponseWs getAbonnementById(@PathVariable Long id) {
                try {
                        UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
                        Long authenticatedUserId = userProfile.getId();
                        // String userType = userProfile.getUserType();

                        Abonnement abonnement = abonnementService.getAbonnementById(id);
                        if (abonnement == null) {
                                return new CommonResponseWs(WsOperationResponseStatus.success,
                                                "Abonnement non trouvé", 404, null);
                        }

                        // Vérifier permissions
                        if (!abonnementService.isAbonnementOwnedByUser(id, authenticatedUserId)) {
                                return new CommonResponseWs(WsOperationResponseStatus.rejected,
                                                "Accès non autorisé à cet abonnement", 401, null);
                        }

                        AbonnementDto abonnementDto = new AbonnementDto(abonnement);
                        return new CommonResponseWs(WsOperationResponseStatus.success,
                                        "Abonnement récupéré avec succès", 200, abonnementDto);
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'abonnement", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération de l'abonnement: " + e.getMessage(), 500, e);
                }
        }

        @Operation(summary = "Obtenir le statut d'abonnement actuel", description = "Récupère toutes les informations sur l'abonnement actuel de l'utilisateur connecté")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statut d'abonnement récupéré avec succès"),
                        @ApiResponse(responseCode = "404", description = "Aucun abonnement actif trouvé")
        })
        @GetMapping("/statut")
        public CommonResponseWs getStatutAbonnement() {
                try {
                        log.info("=== Récupération du statut d'abonnement ===");

                        // Récupérer l'utilisateur connecté
                        UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                        Long userId = currentUser.getId();

                        log.info("UserId connecté: {}", userId);

                        // Récupérer l'abonnement actuel
                        Optional<Abonnement> abonnementOpt = abonnementService.getAbonnementActuel(userId);

                        if (abonnementOpt.isPresent()) {
                                StatutAbonnementDto statutDto = new StatutAbonnementDto(abonnementOpt.get());
                                log.info("Abonnement trouvé - Type: {}, Statut: {}",
                                                statutDto.getTypeAbonnement(),
                                                statutDto.getStatutPaiement());

                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Statut d'abonnement récupéré avec succès",
                                                200,
                                                statutDto);
                        } else {
                                log.info("Aucun abonnement actif trouvé pour userId: {}", userId);
                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Aucun abonnement actif trouvé pour cet utilisateur",
                                                404,
                                                null);
                        }
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération du statut d'abonnement: ", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération du statut d'abonnement: " + e.getMessage(),
                                        500,
                                        null);
                }
        }

        @Operation(summary = "Obtenir le statut d'abonnement par ID utilisateur", description = "Récupère toutes les informations sur l'abonnement actuel d'un utilisateur spécifique (admin)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statut d'abonnement récupéré avec succès"),
                        @ApiResponse(responseCode = "404", description = "Aucun abonnement actif trouvé")
        })
        @GetMapping("/statut/user/{userId}")
        public CommonResponseWs getStatutAbonnementByUserId(
                        @Parameter(description = "ID de l'utilisateur", example = "1") @PathVariable Long userId) {
                try {
                        log.info("=== Récupération du statut d'abonnement pour userId: {} ===", userId);

                        Optional<Abonnement> abonnementOpt = abonnementService.getAbonnementActuel(userId);

                        if (abonnementOpt.isPresent()) {
                                StatutAbonnementDto statutDto = new StatutAbonnementDto(abonnementOpt.get());
                                log.info("Abonnement trouvé pour userId: {} - Type: {}",
                                                userId,
                                                statutDto.getTypeAbonnement());

                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Statut d'abonnement récupéré avec succès",
                                                200,
                                                statutDto);
                        } else {
                                log.info("Aucun abonnement actif trouvé pour userId: {}", userId);
                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Aucun abonnement actif trouvé pour cet utilisateur",
                                                404,
                                                null);
                        }
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération du statut d'abonnement pour userId {}: ", userId, e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération du statut d'abonnement: " + e.getMessage(),
                                        500,
                                        null);
                }
        }

        @Operation(summary = "Vérifier si l'utilisateur a un abonnement actif", description = "Retourne true si l'utilisateur connecté a un abonnement actif, false sinon")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vérification effectuée avec succès")
        })
        @GetMapping("/has-active")
        public CommonResponseWs hasAbonnementActif() {
                try {
                        log.info("=== Vérification abonnement actif ===");

                        UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                        Long userId = currentUser.getId();
                        boolean hasActif = false;
                        boolean abonnementPresent = abonnementService.hasAbonnementActif(userId);
                        boolean inSystem = currentUser.getInSystem();

                        if (abonnementPresent && inSystem) {
                                hasActif = true;
                        }

                        String message = hasActif ? "L'utilisateur possède un abonnement actif"
                                        : "L'utilisateur ne possède pas d'abonnement actif";

                        log.info("UserId: {} - Has Active: {}", userId, hasActif);

                        return new CommonResponseWs(
                                        WsOperationResponseStatus.success,
                                        message,
                                        200,
                                        hasActif);
                } catch (Exception e) {
                        log.error("Erreur lors de la vérification de l'abonnement: ", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la vérification de l'abonnement: " + e.getMessage(),
                                        500,
                                        false);
                }
        }

        @Operation(summary = "Obtenir l'historique des abonnements", description = "Récupère tous les abonnements passés et actuels de l'utilisateur connecté")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès")
        })
        @GetMapping("/historique")
        public CommonResponseWs getHistoriqueAbonnements() {
                try {
                        log.info("=== Récupération historique abonnements ===");

                        UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                        Long userId = currentUser.getId();

                        var historique = abonnementService.getHistoriqueAbonnements(userId);
                        var historiqueDto = historique.stream()
                                        .map(StatutAbonnementDto::new)
                                        .toList();

                        log.info("Historique récupéré - {} abonnement(s)", historiqueDto.size());

                        return new CommonResponseWs(
                                        WsOperationResponseStatus.success,
                                        String.format("Historique des abonnements récupéré avec succès (%d abonnement(s))",
                                                        historiqueDto.size()),
                                        200,
                                        historiqueDto);
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'historique: ", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération de l'historique: " + e.getMessage(),
                                        500,
                                        null);
                }
        }

        @Operation(summary = "Obtenir le type d'abonnement actuel", description = "Retourne uniquement le type d'abonnement de l'utilisateur connecté")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Type d'abonnement récupéré avec succès")
        })
        @GetMapping("/type-actuel")
        public CommonResponseWs getTypeAbonnementActuel() {
                try {
                        log.info("=== Récupération type abonnement actuel ===");

                        UserProfileWs currentUser = CustomJwtUtils.getCurrentUser();
                        Long userId = currentUser.getId();

                        Optional<TypeAbonnement> typeOpt = abonnementService.getTypeAbonnementActuel(userId);

                        if (typeOpt.isPresent()) {
                                var type = typeOpt.get();
                                log.info("Type trouvé: {}", type.getLibelle());

                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Type d'abonnement actuel récupéré avec succès",
                                                200,
                                                type);
                        } else {
                                log.info("Aucun type d'abonnement trouvé");
                                return new CommonResponseWs(
                                                WsOperationResponseStatus.success,
                                                "Aucun abonnement actif",
                                                404,
                                                null);
                        }
                } catch (Exception e) {
                        log.error("Erreur lors de la récupération du type d'abonnement: ", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la récupération du type d'abonnement: " + e.getMessage(),
                                        500,
                                        null);
                }
        }

        /**
         * Pour faire le check
         * 
         * @param
         * @return
         */
        @GetMapping("/check")
        public CommonResponseWs check(@RequestParam(required = false) Long idProjet,
                        @RequestParam Long idPaiement,
                        @RequestParam String secretClient) throws OwomiException {

                CallbackCreditRequestWs ws = new CallbackCreditRequestWs();
                ws.setIdProjet(idProjet);
                ws.setIdPaiement(idPaiement);
                ws.setSecretClient(secretClient);
                ws.setPartnerClient(CustomJwtUtils.getAccessToken(httpServletRequest));

                try {
                        CommonResponseWs responseWs = this.abonnementService.check(ws);
                        log.debug("Resultat du check chez jawupay:{}", responseWs);
                        log.debug("Le contenu de data:{}", responseWs.getData());

                        boolean isPaid = (responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))
                                        .equals(responseWs.getData());

                        Abonnement abonnement = abonnementService.traiterCallbackPaiement(
                                        ws.getIdPaiement(),
                                        isPaid);

                        if (abonnement == null) {
                                return new CommonResponseWs(WsOperationResponseStatus.failed,
                                                "Abonnement introuvable pour ce paiement", 404, null);
                        }

                        // ============================================
                        // 🎯 RÉPONSE ADAPTÉE AU STATUT DU PAIEMENT
                        // ============================================
                        StatutPaiement statutPaiement = abonnement.getStatutPaiement();

                        switch (statutPaiement) {
                                case PAYE:
                                        log.info("✅ Paiement {} réussi - Abonnement {} activé", idPaiement,
                                                        abonnement.getId());
                                        return new CommonResponseWs(
                                                        WsOperationResponseStatus.success,
                                                        "Paiement réussi ! Votre abonnement " +
                                                                        abonnement.getFormuleAbonnement()
                                                                                        .getTypeAbonnement()
                                                                                        .getLibelle()
                                                                        +
                                                                        " est maintenant actif.",
                                                        200,
                                                        Map.of(
                                                                        "abonnement", abonnement,
                                                                        "statutPaiement", "PAYE",
                                                                        "actif", abonnement.getActif(),
                                                                        "typeAbonnement",
                                                                        abonnement.getFormuleAbonnement()
                                                                                        .getTypeAbonnement()
                                                                                        .getLibelle(),
                                                                        "dateDebut", abonnement.getDateDebut(),
                                                                        "dateFin", abonnement.getDateFin(),
                                                                        "montant", abonnement.getPrixAbonnement(),
                                                                        "idPaiement", idPaiement));

                                case ECHEC:
                                        log.warn("❌ Paiement {} échoué - Abonnement {} non activé", idPaiement,
                                                        abonnement.getId());
                                        return new CommonResponseWs(
                                                        WsOperationResponseStatus.failed,
                                                        "Le paiement a échoué. Votre abonnement n'a pas pu être activé. Veuillez réessayer.",
                                                        400,
                                                        Map.of(
                                                                        "abonnement", abonnement,
                                                                        "statutPaiement", "ECHEC",
                                                                        "actif", false,
                                                                        "montant", abonnement.getPrixAbonnement(),
                                                                        "idPaiement", idPaiement,
                                                                        "commandeId", abonnement.getCommandeId()));

                                case EN_ATTENTE:
                                        log.info("⏳ Paiement {} en attente - Abonnement {} non encore activé",
                                                        idPaiement, abonnement.getId());
                                        return new CommonResponseWs(
                                                        WsOperationResponseStatus.pending,
                                                        "Votre paiement est en cours de traitement. Veuillez patienter quelques instants.",
                                                        202,
                                                        Map.of(
                                                                        "abonnement", abonnement,
                                                                        "statutPaiement", "EN_ATTENTE",
                                                                        "actif", false,
                                                                        "montant", abonnement.getPrixAbonnement(),
                                                                        "idPaiement", idPaiement,
                                                                        "commandeId", abonnement.getCommandeId()));

                                case ANNULE:
                                        log.info("🚫 Paiement {} annulé - Abonnement {} annulé", idPaiement,
                                                        abonnement.getId());
                                        return new CommonResponseWs(
                                                        WsOperationResponseStatus.failed,
                                                        "Ce paiement a été annulé.",
                                                        400,
                                                        Map.of(
                                                                        "abonnement", abonnement,
                                                                        "statutPaiement", "ANNULE",
                                                                        "actif", false,
                                                                        "idPaiement", idPaiement,
                                                                        "commandeId", abonnement.getCommandeId()));

                                default:
                                        log.warn("⚠️ Statut paiement inconnu: {} pour paiement {}", statutPaiement,
                                                        idPaiement);
                                        return new CommonResponseWs(
                                                        WsOperationResponseStatus.failed,
                                                        "Statut de paiement inconnu",
                                                        400,
                                                        Map.of(
                                                                        "abonnement", abonnement,
                                                                        "statutPaiement", statutPaiement,
                                                                        "idPaiement", idPaiement));
                        }

                } catch (OwomiException e) {
                        log.error("❌ Erreur métier lors du traitement du callback pour paiement {}", idPaiement, e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors du traitement du paiement : " + e.getMessage(),
                                        500,
                                        Map.of(
                                                        "idPaiement", idPaiement,
                                                        "erreur", e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Erreur interne lors du traitement du callback pour paiement {}", idPaiement, e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur interne lors du traitement du paiement",
                                        500,
                                        Map.of(
                                                        "idPaiement", idPaiement,
                                                        "erreur", "Erreur système"));
                }
        }
        // @GetMapping("/check")
        // public CommonResponseWs check(@RequestParam(required = false) Long idProjet,
        // @RequestParam Long idPaiement,
        // @RequestParam String secretClient) throws OwomiException {
        //
        // CallbackCreditRequestWs ws = new CallbackCreditRequestWs(idProjet,
        // idPaiement, secretClient);
        //
        // try {
        // CommonResponseWs responseWs = this.abonnementService.check(ws);
        // log.debug("Resultat du check chez jawupay:{}", responseWs);
        // log.debug("Le contenu de data:{}", responseWs.getData());
        // Abonnement abonnement = abonnementService.traiterCallbackPaiement(
        // ws.getIdPaiement(),
        // (responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))
        // .equals(responseWs.getData()));
        //
        // if (abonnement != null) {
        // return new CommonResponseWs(WsOperationResponseStatus.success,
        // "Callback traité avec succès", 200, abonnement);
        // } else {
        // return new CommonResponseWs(WsOperationResponseStatus.failed,
        // "Erreur lors du traitement du callback", 410, null);
        // }
        // } catch (Exception e) {
        // log.error("Erreur interne lors du traitement du callback", e);
        // return new CommonResponseWs(WsOperationResponseStatus.error,
        // "Erreur interne lors du traitement du callback", 500, null);
        // }
        // }

        @GetMapping("/has-active/partenaire/abonnement")
        public CommonResponseWs hasAbonnementActifForAnyPartenaire(@RequestParam Long userId) {
                try {
                        log.info("=== Vérification abonnement actif ===");
                        boolean hasActif = false;
                        boolean abonnementPresent = abonnementService.hasAbonnementActif(userId);
                        if (abonnementPresent) {
                                hasActif = true;
                        }

                        String message = hasActif ? "L'utilisateur possède un abonnement actif"
                                        : "L'utilisateur ne possède pas d'abonnement actif";

                        log.info("UserId: {} - Has Active: {}", userId, hasActif);

                        return new CommonResponseWs(
                                        WsOperationResponseStatus.success,
                                        message,
                                        200,
                                        hasActif);
                } catch (Exception e) {
                        log.error("Erreur lors de la vérification de l'abonnement: ", e);
                        return new CommonResponseWs(
                                        WsOperationResponseStatus.error,
                                        "Erreur lors de la vérification de l'abonnement: " + e.getMessage(),
                                        500,
                                        false);
                }
        }

        /**
         * Le callback proprement dite
         * 
         * @param ws
         * @return
         */
        @PostMapping("/callback")
        public CommonResponseWs callback(@RequestBody PaymentCallbackRequest ws) {
                log.debug("Callback JawuPay recu:{}", ws);
                try {
                        Abonnement abonnement = this.abonnementService.traiterCallbackPaiement(
                                        ws.getPaymentId(),
                                        (responseStatusUtils.adapterStatus(WsOperationResponseStatus.success))
                                                        .equals(ws.getStatutPaiement()));

                        return new CommonResponseWs(WsOperationResponseStatus.success,
                                        "Callback traite avec succes", 200, abonnement);
                } catch (OwomiException e) {
                        log.warn("Callback JawuPay non reconciliable pour paymentId {}", ws.getPaymentId(), e);
                        return new CommonResponseWs(WsOperationResponseStatus.failed,
                                        "Callback non traite: " + e.getMessage(), 400, null);
                } catch (Exception e) {
                        log.error("Erreur interne callback JawuPay pour paymentId {}", ws.getPaymentId(), e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur interne lors du callback", 500, null);
                }
        }

        @PostMapping("/admin/reconciliation/renouvellements-payes")
        public CommonResponseWs reconcilierRenouvellementsPayes(
                        @RequestHeader(value = "jawupay-access", required = false) String jawupayAccess,
                        @RequestParam(defaultValue = "200") int limit) {
                try {
                        Map<String, Object> result = subscriptionPaymentReconciliationService
                                        .reconcilierRenouvellementsPayesNonFinalises(
                                                        jawupayAccess,
                                                        CustomJwtUtils.getAccessToken(httpServletRequest),
                                                        limit);
                        return new CommonResponseWs(WsOperationResponseStatus.success,
                                        "Reconciliation terminee", 200, result);
                } catch (Exception e) {
                        log.error("Erreur reconciliation renouvellements payes", e);
                        return new CommonResponseWs(WsOperationResponseStatus.error,
                                        "Erreur reconciliation: " + e.getMessage(), 500, null);
                }
        }

        @Operation(summary = "Route de test pour le callback v2", description = "Permet de tester manuellement le callback avec les IDs fournis et vérification Jawupay")
        @PostMapping("/callback/test")
        public CommonResponseWs testCallback(@RequestBody CallbackV2Request body) {
                try {
                        log.info("=== Test Callback reçu - PayId: {}, UserId: {} ===", body.getPaymentId(),
                                        body.getUserId());
                        Abonnement abonnement = abonnementService.traiterCallbackPaiementParId(
                                        body.getPaymentId(),
                                        body.isSuccess(),
                                        body.getUserId(),
                                        body.getParentId(),
                                        body.getToken(),
                                        body.getTokenCcApp());
                        return new CommonResponseWs(WsOperationResponseStatus.success, "Test callback réussi", 200,
                                        abonnement);
                } catch (Exception e) {
                        log.error("Erreur durant le test callback: ", e);
                        return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur: " + e.getMessage(), 500,
                                        null);
                }
        }

        @Operation(summary = "Route de ROLLBACK pour le test callback v2", description = "Annule les effets du test callback (commissions, parrainage, statut)")
        @PostMapping("/callback/test/rollback")
        public CommonResponseWs rollbackTestCallback(@RequestBody CallbackV2Request body) {
                try {
                        log.info("=== Rollback Test Callback reçu - PayId: {} ===", body.getPaymentId());
                        Abonnement abonnement = abonnementService.rollbackCallbackTest(
                                        body.getPaymentId(),
                                        body.getParentId(),
                                        body.getTokenCcApp());
                        return new CommonResponseWs(WsOperationResponseStatus.success, "Rollback test callback réussi",
                                        200,
                                        abonnement);
                } catch (Exception e) {
                        log.error("Erreur durant le rollback test callback: ", e);
                        return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur: " + e.getMessage(), 500,
                                        null);
                }
        }

}
