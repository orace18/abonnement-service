package com.clubchretien.abonnement.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO représentant un filleul dont l'abonnement expire bientôt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilleulAbonnementExpirantDto {

    /**
     * Nom d'affichage du filleul
     */
    private String displayName;

    /**
     * Grade/statut du filleul (ex: DEBUTANT, BRONZE, SILVER, etc.)
     */
    private String grade;

    /**
     * ID de l'utilisateur
     */
    private Long userId;

    /**
     * Date d'expiration de l'abonnement
     */
    private LocalDateTime dateExpiration;

    /**
     * Type d'abonnement (ex: BASIC, PREMIUM)
     */
    private String typeAbonnement;
}
