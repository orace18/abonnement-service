package com.clubchretien.abonnement.abonnement.services;

import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeAbonnement;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class GestionNiveauxService {

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private FormuleAbonnementService formuleAbonnementService;

    /**
     * Détermine la logique d'application d'un nouvel abonnement selon les règles métier
     */
    public ResultatGestionNiveau gererNouvelAbonnement(Long userId, FormuleAbonnement nouvelleFormule) {
        // Récupérer l'abonnement actuel de l'utilisateur
        Optional<Abonnement> abonnementActuelOpt = abonnementRepository
                .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                        userId,
                        StatutPaiement.PAYE,
                        LocalDateTime.now()
                );

        if (abonnementActuelOpt.isEmpty()) {
            // Pas d'abonnement en cours => grade acheté et date d'achat comme date de début
            return new ResultatGestionNiveau(
                    TypeAction.NOUVEAU_ABONNEMENT,
                    "Nouvel abonnement créé",
                    LocalDateTime.now(), // Date de début = maintenant
                    calculerDateFin(LocalDateTime.now(), nouvelleFormule.getDuree()),
                    true
            );
        }

        Abonnement abonnementActuel = abonnementActuelOpt.get();
        TypeAbonnement typeActuel = abonnementActuel.getFormuleAbonnement().getTypeAbonnement();
        TypeAbonnement nouveauType = nouvelleFormule.getTypeAbonnement();

        // Comparer les niveaux d'abonnement
        int comparaison = compareNiveaux(typeActuel, nouveauType);

        if (comparaison > 0) {
            // Abonnement en cours est supérieur => interdit
            return new ResultatGestionNiveau(
                    TypeAction.INTERDITE,
                    "Impossible de passer à un abonnement de niveau inférieur",
                    null,
                    null,
                    false
            );
        } else if (comparaison < 0) {
            // Abonnement existant inférieur => considérer date d'achat comme date de début
            return new ResultatGestionNiveau(
                    TypeAction.UPGRADE,
                    "Upgrade d'abonnement - activation immédiate",
                    LocalDateTime.now(), // Date de début = maintenant
                    calculerDateFin(LocalDateTime.now(), nouvelleFormule.getDuree()),
                    true
            );
        } else {
            // Abonnement identique => considérer date de fin de l'abonnement comme date de début
            LocalDateTime dateFinActuelle = abonnementActuel.getDateFin();
            return new ResultatGestionNiveau(
                    TypeAction.RENOUVELLEMENT,
                    "Renouvellement d'abonnement - activation à la fin de l'abonnement actuel",
                    dateFinActuelle, // Date de début = fin de l'abonnement actuel
                    calculerDateFin(dateFinActuelle, nouvelleFormule.getDuree()),
                    true
            );
        }
    }

    /**
     * Compare les niveaux d'abonnement
     * @return < 0 si type1 < type2, 0 si égaux, > 0 si type1 > type2
     */
    private int compareNiveaux(TypeAbonnement type1, TypeAbonnement type2) {
        return Integer.compare(type1.getNiveau(), type2.getNiveau());
    }

    /**
     * Calcule la date de fin en fonction de la date de début et de la durée
     */
    private LocalDateTime calculerDateFin(LocalDateTime dateDebut, Duree duree) {
        return dateDebut.plusMonths(duree.getMois());
    }

    /**
     * Applique la logique de gestion des niveaux lors de l'activation d'un abonnement
     */
    public boolean appliquerGestionNiveau(Abonnement nouvelAbonnement) {
        try {
            Long userId = nouvelAbonnement.getUserId();
            FormuleAbonnement nouvelleFormule = nouvelAbonnement.getFormuleAbonnement();

            ResultatGestionNiveau resultat = gererNouvelAbonnement(userId, nouvelleFormule);

            if (!resultat.isAutorise()) {
                return false; // Action interdite
            }

            // Appliquer les dates calculées
            nouvelAbonnement.setDateDebut(resultat.getDateDebut());
            nouvelAbonnement.setDateFin(resultat.getDateFin());

            // Gérer l'abonnement précédent selon le type d'action
            switch (resultat.getTypeAction()) {
                case UPGRADE:
                    // Désactiver l'abonnement actuel
                    desactiverAbonnementActuel(userId);
                    nouvelAbonnement.setActif(true);
                    break;

                case NOUVEAU_ABONNEMENT:
                    nouvelAbonnement.setActif(true);
                    break;

                case RENOUVELLEMENT:
                    // L'abonnement sera activé automatiquement à la date de début
                    nouvelAbonnement.setActif(false); // Sera activé plus tard
                    break;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Désactive l'abonnement actuel d'un utilisateur
     */
    private void desactiverAbonnementActuel(Long userId) {
        Optional<Abonnement> abonnementActuelOpt = abonnementRepository
                .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                        userId,
                        StatutPaiement.PAYE,
                        LocalDateTime.now()
                );
        if (abonnementActuelOpt.isPresent()) {
            Abonnement abonnementActuel = abonnementActuelOpt.get();
            abonnementActuel.setActif(false);
            abonnementActuel.setDateFin(LocalDateTime.now()); // Terminer maintenant
            abonnementRepository.save(abonnementActuel);
        }
    }

    /**
     * Vérifie si un upgrade est possible
     */
    public boolean peutUpgrader(Long userId, TypeAbonnement nouveauType) {
        Optional<Abonnement> abonnementActuelOpt = abonnementRepository
                .findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                        userId,
                        StatutPaiement.PAYE,
                        LocalDateTime.now()
                );

        if (abonnementActuelOpt.isEmpty()) {
            return true; // Pas d'abonnement actuel, upgrade possible
        }

        TypeAbonnement typeActuel = abonnementActuelOpt.get().getFormuleAbonnement().getTypeAbonnement();
        return compareNiveaux(typeActuel, nouveauType) < 0; // Upgrade seulement si niveau supérieur
    }

    /**
     * Énumération des types d'actions possibles
     */
    public enum TypeAction {
        NOUVEAU_ABONNEMENT,
        UPGRADE,
        RENOUVELLEMENT,
        INTERDITE
    }

    /**
     * Classe pour encapsuler le résultat de la gestion des niveaux
     */
    public static class ResultatGestionNiveau {
        private TypeAction typeAction;
        private String message;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private boolean autorise;

        public ResultatGestionNiveau(TypeAction typeAction, String message, LocalDateTime dateDebut, LocalDateTime dateFin, boolean autorise) {
            this.typeAction = typeAction;
            this.message = message;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.autorise = autorise;
        }

        // Getters
        public TypeAction getTypeAction() { return typeAction; }
        public String getMessage() { return message; }
        public LocalDateTime getDateDebut() { return dateDebut; }
        public LocalDateTime getDateFin() { return dateFin; }
        public boolean isAutorise() { return autorise; }
    }
}