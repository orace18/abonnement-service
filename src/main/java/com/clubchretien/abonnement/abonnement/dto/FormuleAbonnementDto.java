package com.clubchretien.abonnement.abonnement.dto;



import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.TypeAbonnement;

import java.math.BigDecimal;

public class FormuleAbonnementDto {
    private Long id;
    private String pays;
    private TypeAbonnement typeAbonnement;
    private Duree duree;
    private BigDecimal prix;
    private BigDecimal maxLeveeFond;
    private String typeLibelle;
    private String dureeLibelle;

    // Constructeur par défaut
    public FormuleAbonnementDto() {}

    // Constructeur à partir de l'entité
    public FormuleAbonnementDto(FormuleAbonnement formule) {
        this.id = formule.getId();
        this.pays = formule.getPays();
        this.typeAbonnement = formule.getTypeAbonnement();
        this.duree = formule.getDuree();
        this.prix = formule.getPrix();
        this.maxLeveeFond = formule.getMaxLeveeFond();
        this.typeLibelle = formule.getTypeAbonnement().getLibelle();
        this.dureeLibelle = formule.getDuree().getLibelle();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public TypeAbonnement getTypeAbonnement() {
        return typeAbonnement;
    }

    public void setTypeAbonnement(TypeAbonnement typeAbonnement) {
        this.typeAbonnement = typeAbonnement;
    }

    public Duree getDuree() {
        return duree;
    }

    public void setDuree(Duree duree) {
        this.duree = duree;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public BigDecimal getMaxLeveeFond() {
        return maxLeveeFond;
    }

    public void setMaxLeveeFond(BigDecimal maxLeveeFond) {
        this.maxLeveeFond = maxLeveeFond;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }

    public String getDureeLibelle() {
        return dureeLibelle;
    }

    public void setDureeLibelle(String dureeLibelle) {
        this.dureeLibelle = dureeLibelle;
    }
}