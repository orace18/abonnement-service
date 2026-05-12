package com.clubchretien.abonnement.ws;

public class OperationStepOneWs {
    private Integer[] panierProductIdList;
    private Integer idCorporate;
    private String deviseTransaction;
    private Integer idCommande;

    public OperationStepOneWs() {
    }

    public OperationStepOneWs(Integer[] panierProductIdList, Integer idCorporate, String deviseTransaction, Integer idCommande) {
        this.panierProductIdList = panierProductIdList;
        this.idCorporate = idCorporate;
        this.deviseTransaction = deviseTransaction;
        this.idCommande = idCommande;
    }

    public Integer[] getPanierProductIdList() {
        return panierProductIdList;
    }

    public void setPanierProductIdList(Integer[] panierProductIdList) {
        this.panierProductIdList = panierProductIdList;
    }

    public Integer getIdCorporate() {
        return idCorporate;
    }

    public void setIdCorporate(Integer idCorporate) {
        this.idCorporate = idCorporate;
    }

    public String getDeviseTransaction() {
        return deviseTransaction;
    }

    public void setDeviseTransaction(String deviseTransaction) {
        this.deviseTransaction = deviseTransaction;
    }

    public Integer getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(Integer idCommande) {
        this.idCommande = idCommande;
    }
}
