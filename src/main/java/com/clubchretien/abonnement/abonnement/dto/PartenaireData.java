package com.clubchretien.abonnement.abonnement.dto;

public class PartenaireData {
    private Boolean subscriptionState;
    private Boolean pinState;
    private Boolean civilState;
    private Boolean subscribed;
    private Boolean countryState;
    private Boolean emailVerified;
    private Boolean phoneNumberVerified;
    private Partenaire partenaire;
    private Boolean kycValidator;

    // Getters et Setters
    public Boolean getSubscriptionState() { return subscriptionState; }
    public void setSubscriptionState(Boolean subscriptionState) { this.subscriptionState = subscriptionState; }

    public Boolean getPinState() { return pinState; }
    public void setPinState(Boolean pinState) { this.pinState = pinState; }

    public Boolean getCivilState() { return civilState; }
    public void setCivilState(Boolean civilState) { this.civilState = civilState; }

    public Boolean getSubscribed() { return subscribed; }
    public void setSubscribed(Boolean subscribed) { this.subscribed = subscribed; }

    public Boolean getCountryState() { return countryState; }
    public void setCountryState(Boolean countryState) { this.countryState = countryState; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Boolean getPhoneNumberVerified() { return phoneNumberVerified; }
    public void setPhoneNumberVerified(Boolean phoneNumberVerified) { this.phoneNumberVerified = phoneNumberVerified; }

    public Partenaire getPartenaire() { return partenaire; }
    public void setPartenaire(Partenaire partenaire) { this.partenaire = partenaire; }

    public Boolean getKycValidator() { return kycValidator; }
    public void setKycValidator(Boolean kycValidator) { this.kycValidator = kycValidator; }
}