package com.clubchretien.abonnement.abonnement.controllers;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.TypeAbonnement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clubchretien.abonnement.abonnement.services.FormuleAbonnementService;
import com.clubchretien.abonnement.abonnement.services.GestionNiveauxService;

import java.util.Optional;

@RestController
@RequestMapping("/api/gestion-niveaux")
@CrossOrigin(origins = "*")
public class GestionNiveauxController {

    @Autowired
    private GestionNiveauxService gestionNiveauxService;

    @Autowired
    private FormuleAbonnementService formuleAbonnementService;

    /**
     * API pour vérifier si un utilisateur peut upgrader vers un type d'abonnement
     */
    @GetMapping("/peut-upgrader/{userId}/{typeAbonnement}")
    public ResponseEntity<CommonResponseWs> peutUpgrader(
            @PathVariable Long userId,
            @PathVariable TypeAbonnement typeAbonnement) {
        try {
            boolean peutUpgrader = gestionNiveauxService.peutUpgrader(userId, typeAbonnement);
            String message = peutUpgrader ? 
                "Upgrade possible vers " + typeAbonnement.getLibelle() :
                "Upgrade impossible - niveau égal ou inférieur à l'abonnement actuel";
            
            UpgradeCheckData data = new UpgradeCheckData(peutUpgrader, message);
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, data));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la vérification de l'upgrade: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour simuler l'application d'un nouvel abonnement
     */
    @PostMapping("/simuler-abonnement")
    public ResponseEntity<CommonResponseWs> simulerNouvelAbonnement(
            @RequestBody SimulationRequest request) {
        try {
            Optional<FormuleAbonnement> formuleOpt = formuleAbonnementService.getFormuleById(request.getFormuleId());
            if (formuleOpt.isEmpty()) {
                return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.rejected, "Formule d'abonnement introuvable", 404, null));
            }
            FormuleAbonnement formule = formuleOpt.get();

            GestionNiveauxService.ResultatGestionNiveau resultat = 
                    gestionNiveauxService.gererNouvelAbonnement(request.getUserId(), formule);

            SimulationData data = new SimulationData(
                    resultat.isAutorise(),
                    resultat.getMessage(),
                    resultat.getTypeAction().toString()
            );

            WsOperationResponseStatus status = resultat.isAutorise() ? 
                WsOperationResponseStatus.success : WsOperationResponseStatus.rejected;
            
            return ResponseEntity.ok(new CommonResponseWs(status, resultat.getMessage(), resultat.isAutorise() ? 200 : 400, data));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la simulation de l'abonnement: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour obtenir les règles de gestion des niveaux
     */
    @GetMapping("/regles")
    public ResponseEntity<CommonResponseWs> getReglesGestion() {
        try {
            ReglesGestionData regles = new ReglesGestionData(
                "Règles de gestion des niveaux d'abonnement",
                new String[]{
                    "Pas d'abonnement en cours: grade acheté et date d'achat comme date de début",
                    "Abonnement existant inférieur: considérer date d'achat comme date de début",
                    "Abonnement identique: considérer date de fin de l'abonnement comme date de début",
                    "Abonnement en cours supérieur: interdit"
                },
                new String[]{
                    "Classic (niveau 1): Abonnement de base",
                    "Gold (niveau 2): Abonnement intermédiaire", 
                    "Diamond (niveau 3): Abonnement premium"
                }
            );
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, "Règles de gestion des niveaux récupérées avec succès", 200, regles));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des règles de gestion: " + e.getMessage(), 500, e));
        }
    }

    // Classes pour les données
    public static class UpgradeCheckData {
        private boolean peutUpgrader;
        private String message;

        public UpgradeCheckData(boolean peutUpgrader, String message) {
            this.peutUpgrader = peutUpgrader;
            this.message = message;
        }

        public boolean isPeutUpgrader() { return peutUpgrader; }
        public String getMessage() { return message; }
    }

    public static class SimulationRequest {
        private Long userId;
        private Long formuleId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getFormuleId() { return formuleId; }
        public void setFormuleId(Long formuleId) { this.formuleId = formuleId; }
    }

    public static class SimulationData {
        private boolean autorise;
        private String message;
        private String typeAction;

        public SimulationData(boolean autorise, String message, String typeAction) {
            this.autorise = autorise;
            this.message = message;
            this.typeAction = typeAction;
        }

        public boolean isAutorise() { return autorise; }
        public String getMessage() { return message; }
        public String getTypeAction() { return typeAction; }
    }

    public static class ReglesGestionData {
        private String titre;
        private String[] regles;
        private String[] niveaux;

        public ReglesGestionData(String titre, String[] regles, String[] niveaux) {
            this.titre = titre;
            this.regles = regles;
            this.niveaux = niveaux;
        }

        public String getTitre() { return titre; }
        public String[] getRegles() { return regles; }
        public String[] getNiveaux() { return niveaux; }
    }
}