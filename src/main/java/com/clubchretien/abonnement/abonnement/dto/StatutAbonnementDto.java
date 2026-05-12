package com.clubchretien.abonnement.abonnement.dto;

import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeAbonnement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StatutAbonnementDto {

    private Long abonnementId;
    private TypeAbonnement typeAbonnement;
    private String libelleType;
    private Duree duree;
    private String libelleDuree;
    private StatutPaiement statutPaiement;
    private BigDecimal prixAbonnement;
    private LocalDateTime dateAchat;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Boolean actif;
    private Boolean valide;
    private Boolean expire;
    private Long joursRestants;
    private Long paymentId;
    private Long commandeId;

    // Informations de la formule
    private Long formuleId;
    private String nomFormule;
    private String descriptionFormule;
    private String pays;

    public StatutAbonnementDto() {
    }

    public StatutAbonnementDto(Abonnement abonnement) {
        if (abonnement != null) {
            this.abonnementId = abonnement.getId();
            this.statutPaiement = abonnement.getStatutPaiement();
            this.prixAbonnement = abonnement.getPrixAbonnement();
            this.dateAchat = abonnement.getDateAchat();
            this.dateDebut = abonnement.getDateDebut();
            this.dateFin = abonnement.getDateFin();
            this.actif = abonnement.getActif();
            this.valide = abonnement.isValide();
            this.expire = abonnement.isExpire();
            this.joursRestants = abonnement.getJoursRestants();
            this.paymentId = abonnement.getPaymentId();
            this.commandeId = abonnement.getCommandeId();

            if (abonnement.getFormuleAbonnement() != null) {
                this.formuleId = abonnement.getFormuleAbonnement().getId();
                this.nomFormule = abonnement.getFormuleAbonnement().getTypeAbonnement().name();
                this.descriptionFormule = abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle();
                this.typeAbonnement = abonnement.getFormuleAbonnement().getTypeAbonnement();
                this.libelleType = abonnement.getFormuleAbonnement().getTypeAbonnement().getLibelle();
                this.duree = abonnement.getFormuleAbonnement().getDuree();
                this.libelleDuree = abonnement.getFormuleAbonnement().getDuree().getLibelle();
                this.pays = abonnement.getFormuleAbonnement().getPays();
            }
        }
    }

    // Getters et Setters
    public Long getAbonnementId() {
        return abonnementId;
    }

    public void setAbonnementId(Long abonnementId) {
        this.abonnementId = abonnementId;
    }

    public TypeAbonnement getTypeAbonnement() {
        return typeAbonnement;
    }

    public void setTypeAbonnement(TypeAbonnement typeAbonnement) {
        this.typeAbonnement = typeAbonnement;
    }

    public String getLibelleType() {
        return libelleType;
    }

    public void setLibelleType(String libelleType) {
        this.libelleType = libelleType;
    }

    public Duree getDuree() {
        return duree;
    }

    public void setDuree(Duree duree) {
        this.duree = duree;
    }

    public String getLibelleDuree() {
        return libelleDuree;
    }

    public void setLibelleDuree(String libelleDuree) {
        this.libelleDuree = libelleDuree;
    }

    public StatutPaiement getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutPaiement statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public BigDecimal getPrixAbonnement() {
        return prixAbonnement;
    }

    public void setPrixAbonnement(BigDecimal prixAbonnement) {
        this.prixAbonnement = prixAbonnement;
    }

    public LocalDateTime getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDateTime dateAchat) {
        this.dateAchat = dateAchat;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Boolean getValide() {
        return valide;
    }

    public void setValide(Boolean valide) {
        this.valide = valide;
    }

    public Boolean getExpire() {
        return expire;
    }

    public void setExpire(Boolean expire) {
        this.expire = expire;
    }

    public Long getJoursRestants() {
        return joursRestants;
    }

    public void setJoursRestants(Long joursRestants) {
        this.joursRestants = joursRestants;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getFormuleId() {
        return formuleId;
    }

    public void setFormuleId(Long formuleId) {
        this.formuleId = formuleId;
    }

    public String getNomFormule() {
        return nomFormule;
    }

    public void setNomFormule(String nomFormule) {
        this.nomFormule = nomFormule;
    }

    public String getDescriptionFormule() {
        return descriptionFormule;
    }

    public void setDescriptionFormule(String descriptionFormule) {
        this.descriptionFormule = descriptionFormule;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }
}