package com.clubchretien.abonnement.abonnement.services;

import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.TypeAbonnement;
import com.owomi.database.repositories.repositories.FormuleAbonnementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FormuleAbonnementService {

    @Autowired
    private FormuleAbonnementRepository formuleAbonnementRepository;

    /**
     * Récupère toutes les formules actives pour un pays donné
     */
    public List<FormuleAbonnement> getFormulesByPays(String pays) {
        return formuleAbonnementRepository.findByPaysAndActifTrueOrderByTypeAbonnementAscDureeAsc(pays);
    }

    /**
     * Récupère toutes les formules actives
     */
    public List<FormuleAbonnement> getAllFormules() {
        return formuleAbonnementRepository.findByActifTrueOrderByPaysAscTypeAbonnementAscDureeAsc();
    }

    /**
     * Récupère une formule spécifique
     */
    public Optional<FormuleAbonnement> getFormule(String pays, TypeAbonnement typeAbonnement, Duree duree) {
        return formuleAbonnementRepository.findByPaysAndTypeAbonnementAndDureeAndActifTrue(pays, typeAbonnement, duree);
    }

    /**
     * Récupère une formule par son ID
     */
    public Optional<FormuleAbonnement> getFormuleById(Long id) {
        return formuleAbonnementRepository.findById(id);
    }

    /**
     * Récupère les pays disponibles (valeurs distinctes)
     */
    public List<String> getPaysDisponibles() {
        return formuleAbonnementRepository.findDistinctByActifTrueOrderByPaysAsc()
                .stream()
                .map(FormuleAbonnement::getPays)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Récupère les types d'abonnement disponibles pour un pays (valeurs distinctes)
     */
    public List<TypeAbonnement> getTypesAbonnementByPays(String pays) {
        return formuleAbonnementRepository.findByPaysAndActifTrueOrderByTypeAbonnementAsc(pays)
                .stream()
                .map(FormuleAbonnement::getTypeAbonnement)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Récupère les durées disponibles pour un pays et un type (valeurs distinctes)
     */
    public List<Duree> getDureesByPaysAndType(String pays, TypeAbonnement type) {
        return formuleAbonnementRepository.findByPaysAndTypeAbonnementAndActifTrueOrderByDureeAsc(pays, type)
                .stream()
                .map(FormuleAbonnement::getDuree)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si une formule existe
     */
    public boolean formuleExists(String pays, TypeAbonnement typeAbonnement, Duree duree) {
        return formuleAbonnementRepository.existsByPaysAndTypeAbonnementAndDureeAndActifTrue(pays, typeAbonnement, duree);
    }

    /**
     * Crée une nouvelle formule d'abonnement
     */
    public FormuleAbonnement creerFormule(FormuleAbonnement formule) {
        if (formuleExists(formule.getPays(), formule.getTypeAbonnement(), formule.getDuree())) {
            throw new IllegalArgumentException("Une formule existe déjà pour ces critères");
        }
        return formuleAbonnementRepository.save(formule);
    }

    /**
     * Met à jour une formule d'abonnement
     */
    public FormuleAbonnement updateFormule(FormuleAbonnement formule) {
        if (formule.getId() == null || !formuleAbonnementRepository.existsById(formule.getId())) {
            throw new IllegalArgumentException("Formule introuvable");
        }
        return formuleAbonnementRepository.save(formule);
    }

    /**
     * Désactive une formule (soft delete)
     */
    public void desactiverFormule(Long id) {
        Optional<FormuleAbonnement> formule = formuleAbonnementRepository.findById(id);
        if (formule.isPresent()) {
            formule.get().setActif(false);
            formuleAbonnementRepository.save(formule.get());
        } else {
            throw new IllegalArgumentException("Formule introuvable");
        }
    }

    /**
     * Réactive une formule
     */
    public void reactiverFormule(Long id) {
        Optional<FormuleAbonnement> formule = formuleAbonnementRepository.findById(id);
        if (formule.isPresent()) {
            formule.get().setActif(true);
            formuleAbonnementRepository.save(formule.get());
        } else {
            throw new IllegalArgumentException("Formule introuvable");
        }
    }

    /**
     * Récupère les formules par type d'abonnement
     */
    public List<FormuleAbonnement> getFormulesByType(TypeAbonnement type) {
        return formuleAbonnementRepository.findByTypeAbonnementAndActifTrueOrderByPaysAscDureeAsc(type);
    }

    /**
     * Récupère les formules par durée
     */
    public List<FormuleAbonnement> getFormulesByDuree(Duree duree) {
        return formuleAbonnementRepository.findByDureeAndActifTrueOrderByPaysAscTypeAbonnementAsc(duree);
    }

    /**
     * Rechercher des formules avec critères multiples
     */
    public List<FormuleAbonnement> rechercherFormules(String pays, TypeAbonnement type, Duree duree) {
        if (pays != null && type != null && duree != null) {
            return formuleAbonnementRepository.findByPaysAndTypeAbonnementAndDureeAndActifTrue(pays, type, duree)
                    .map(List::of)
                    .orElse(List.of());
        } else if (pays != null && type != null) {
            return formuleAbonnementRepository.findByPaysAndTypeAbonnementAndActifTrueOrderByDureeAsc(pays, type);
        } else if (pays != null && duree != null) {
            return formuleAbonnementRepository.findByPaysAndDureeAndActifTrueOrderByTypeAbonnementAsc(pays, duree);
        } else if (type != null && duree != null) {
            return formuleAbonnementRepository.findByTypeAbonnementAndDureeAndActifTrueOrderByPaysAsc(type, duree);
        } else if (pays != null) {
            return getFormulesByPays(pays);
        } else if (type != null) {
            return getFormulesByType(type);
        } else if (duree != null) {
            return getFormulesByDuree(duree);
        } else {
            return getAllFormules();
        }
    }

    /**
     * Récupère tous les types d'abonnement disponibles
     */
    public List<TypeAbonnement> getTypesDisponibles() {
        return List.of(TypeAbonnement.values());
    }

    /**
     * Récupère toutes les durées disponibles
     */
    public List<Duree> getDureesDisponibles() {
        return List.of(Duree.values());
    }
}