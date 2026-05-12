package com.clubchretien.abonnement.abonnement.dto;

import javax.validation.constraints.NotNull;

public class AchatAbonnementRequest {
    
    @NotNull(message = "L'ID de la formule est obligatoire")
    private Long formuleId;

    @NotNull(message = "Le numero de telephone est requis")
    private String numeroPhone;

    @NotNull(message = "Moyen de paiement est requis")
    private String moyenPay;

    @NotNull(message = "Le numeroCCParent est requis")
    private String numeroCCParent;

    public String getNumeroPhone() {
        return numeroPhone;
    }

    public void setNumeroPhone(String numeroPhone) {
        this.numeroPhone = numeroPhone;
    }

    public String getMoyenPay() {
        return moyenPay;
    }

    public void setMoyenPay(String moyenPay) {
        this.moyenPay = moyenPay;
    }


    public AchatAbonnementRequest() {}

    public AchatAbonnementRequest(Long formuleId, String numeroPhone, String moyenPay, String numeroCCParent) {
        this.formuleId = formuleId;
        this.numeroPhone = numeroPhone;
        this.moyenPay = moyenPay;
        this.numeroCCParent = numeroCCParent;
    }

    public String getNumeroCCParent() {
        return numeroCCParent;
    }

    public void setNumeroCCParent(String numeroCCParent) {
        this.numeroCCParent = numeroCCParent;
    }

    // Getters et Setters
    public Long getFormuleId() {
        return formuleId;
    }

    public void setFormuleId(Long formuleId) {
        this.formuleId = formuleId;
    }
}