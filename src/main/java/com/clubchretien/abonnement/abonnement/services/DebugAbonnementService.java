package com.clubchretien.abonnement.abonnement.services;


import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DebugAbonnementService {

    @Autowired
    private AbonnementService abonnementService;

    /**
     * Endpoint de debug pour comprendre pourquoi hasAbonnement = false
     */
    public CommonResponseWs debugAbonnementUtilisateur() {
        try {
            Long userId = CustomJwtUtils.getCurrentUserId();
            log.info(" === DEBUG ABONNEMENT pour userId: {} ===", userId);

            // 1. Récupérer TOUS les abonnements de l'utilisateur
            List<Abonnement> tousAbonnements = abonnementService.getHistoriqueAbonnements(userId);

            log.info("📊 Nombre total d'abonnements trouvés: {}", tousAbonnements.size());

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", userId);
            debugInfo.put("nombreAbonnements", tousAbonnements.size());
            debugInfo.put("dateActuelle", LocalDateTime.now());

            List<Map<String, Object>> detailsAbonnements = new ArrayList<>();

            for (Abonnement abo : tousAbonnements) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", abo.getId());
                detail.put("commandeId", abo.getCommandeId());
                detail.put("paymentId", abo.getPaymentId());
                detail.put("statutPaiement", abo.getStatutPaiement());
                detail.put("actif", abo.getActif());
                detail.put("dateAchat", abo.getDateAchat());
                detail.put("dateDebut", abo.getDateDebut());
                detail.put("dateFin", abo.getDateFin());
                detail.put("prix", abo.getPrixAbonnement());

                // Vérifications détaillées
                boolean estActif = abo.getActif();
                boolean estPaye = abo.getStatutPaiement().name().equals("PAYE");
                boolean nonExpire = LocalDateTime.now().isBefore(abo.getDateFin());
                boolean isValide = abo.isValide();

                detail.put("checks", Map.of(
                        "estActif", estActif,
                        "estPaye", estPaye,
                        "nonExpire", nonExpire,
                        "isValide", isValide
                ));

                detail.put("typeAbonnement",
                        abo.getFormuleAbonnement() != null ?
                                abo.getFormuleAbonnement().getTypeAbonnement() : "NULL");

                log.info("Abonnement ID: {} | Statut: {} | Actif: {} | Payé: {} | Expiré: {} | isValide: {}",
                        abo.getId(),
                        abo.getStatutPaiement(),
                        estActif,
                        estPaye,
                        !nonExpire,
                        isValide
                );

                detailsAbonnements.add(detail);
            }

            debugInfo.put("abonnements", detailsAbonnements);

            // 2. Récupérer l'abonnement "actuel" selon la logique métier
            Optional<Abonnement> abonnementActuel = abonnementService.getAbonnementActuel(userId);
            debugInfo.put("abonnementActuelTrouve", abonnementActuel.isPresent());

            if (abonnementActuel.isPresent()) {
                Abonnement abo = abonnementActuel.get();
                debugInfo.put("abonnementActuel", Map.of(
                        "id", abo.getId(),
                        "statut", abo.getStatutPaiement(),
                        "isValide", abo.isValide()
                ));
                log.info("Abonnement actuel trouvé: ID={}, isValide={}",
                        abo.getId(), abo.isValide());
            } else {
                log.warn("Aucun abonnement actuel trouvé par getAbonnementActuel()");
            }

            // 3. Vérification avec hasAbonnementActif
            boolean hasActif = abonnementService.hasAbonnementActif(userId);
            debugInfo.put("hasAbonnementActif", hasActif);
            log.info(" hasAbonnementActif() retourne: {}", hasActif);

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Debug abonnement effectué",
                    200,
                    debugInfo
            );

        } catch (Exception e) {
            log.error("Erreur lors du debug: ", e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur debug: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    /**
     * Debug pour un userId spécifique (admin)
     */
    public CommonResponseWs debugAbonnementUtilisateurById(Long userId) {
        try {
            log.info(" === DEBUG ABONNEMENT pour userId: {} (par admin) ===", userId);

            List<Abonnement> tousAbonnements = abonnementService.getHistoriqueAbonnements(userId);

            log.info("📊 Nombre total d'abonnements trouvés: {}", tousAbonnements.size());

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", userId);
            debugInfo.put("nombreAbonnements", tousAbonnements.size());
            debugInfo.put("dateActuelle", LocalDateTime.now());

            List<Map<String, Object>> detailsAbonnements = new ArrayList<>();

            for (Abonnement abo : tousAbonnements) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", abo.getId());
                detail.put("commandeId", abo.getCommandeId());
                detail.put("paymentId", abo.getPaymentId());
                detail.put("statutPaiement", abo.getStatutPaiement());
                detail.put("actif", abo.getActif());
                detail.put("dateAchat", abo.getDateAchat());
                detail.put("dateDebut", abo.getDateDebut());
                detail.put("dateFin", abo.getDateFin());
                detail.put("prix", abo.getPrixAbonnement());
                detail.put("isValide", abo.isValide());
                detail.put("typeAbonnement",
                        abo.getFormuleAbonnement() != null ?
                                abo.getFormuleAbonnement().getTypeAbonnement() : "NULL");

                log.info("Abonnement ID: {} | Statut: {} | Actif: {} | isValide: {}",
                        abo.getId(),
                        abo.getStatutPaiement(),
                        abo.getActif(),
                        abo.isValide()
                );

                detailsAbonnements.add(detail);
            }

            debugInfo.put("abonnements", detailsAbonnements);

            Optional<Abonnement> abonnementActuel = abonnementService.getAbonnementActuel(userId);
            debugInfo.put("abonnementActuelTrouve", abonnementActuel.isPresent());

            if (abonnementActuel.isPresent()) {
                Abonnement abo = abonnementActuel.get();
                debugInfo.put("abonnementActuel", Map.of(
                        "id", abo.getId(),
                        "statut", abo.getStatutPaiement(),
                        "isValide", abo.isValide()
                ));
            }

            boolean hasActif = abonnementService.hasAbonnementActif(userId);
            debugInfo.put("hasAbonnementActif", hasActif);

            return new CommonResponseWs(
                    WsOperationResponseStatus.success,
                    "Debug abonnement effectué",
                    200,
                    debugInfo
            );

        } catch (Exception e) {
            log.error("Erreur lors du debug: ", e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur debug: " + e.getMessage(),
                    500,
                    null
            );
        }
    }
}