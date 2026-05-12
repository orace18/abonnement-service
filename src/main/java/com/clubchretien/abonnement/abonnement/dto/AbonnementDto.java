package com.clubchretien.abonnement.abonnement.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.enums.StatutPaiement;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AbonnementDto {
    private Long id;
    private FormuleAbonnementDto formule;
    private BigDecimal prixAbonnement;
    private LocalDateTime dateAchat;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutPaiement statutPaiement;
    private Long userId;
    private Long paymentId;      // 👈 String, pas Long
    private Long commandeId;     // 👈 String, pas Long
    private Boolean actif;
    private Boolean valide;
    private Boolean expire;

    // 👇 Constructeur de conversion CORRIGÉ
    public AbonnementDto(Abonnement abonnement) {
        if (abonnement != null) {
            this.id = abonnement.getId();
            this.formule = abonnement.getFormuleAbonnement() != null ?  // 👈 getFormuleAbonnement()
                    new FormuleAbonnementDto(abonnement.getFormuleAbonnement()) : null;
            this.prixAbonnement = abonnement.getPrixAbonnement();
            this.dateAchat = abonnement.getDateAchat();
            this.dateDebut = abonnement.getDateDebut();
            this.dateFin = abonnement.getDateFin();
            this.statutPaiement = abonnement.getStatutPaiement();
            this.userId = abonnement.getUserId();
            this.paymentId = abonnement.getPaymentId();
            this.commandeId = abonnement.getCommandeId();
            this.actif = abonnement.getActif();
            this.valide = abonnement.isValide();    // 👈 isValide() (méthode)
            this.expire = abonnement.isExpire();    // 👈 isExpire() (méthode)
        }
    }
}