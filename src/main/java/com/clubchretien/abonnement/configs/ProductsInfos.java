package com.clubchretien.abonnement.configs;

public class ProductsInfos {
    private String fraisInscription;
    private String fraisMaintenance;
    private String kitDemarrageAgent;
    private String kitDemarrageMarchand;

    public String getFraisInscription() {
        return fraisInscription;
    }

    public void setFraisInscription(String fraisInscription) {
        this.fraisInscription = fraisInscription;
    }

    public String getFraisMaintenance() {
        return fraisMaintenance;
    }

    public void setFraisMaintenance(String fraisMaintenance) {
        this.fraisMaintenance = fraisMaintenance;
    }

    public String getKitDemarrageAgent() { return kitDemarrageAgent; }

    public void setKitDemarrageAgent(String kitDemarrageAgent) { this.kitDemarrageAgent = kitDemarrageAgent; }

    public String getKitDemarrageMarchand() { return kitDemarrageMarchand; }

    public void setKitDemarrageMarchand(String kitDemarrageMarchand) { this.kitDemarrageMarchand = kitDemarrageMarchand; }
}
