package com.clubchretien.abonnement.core.partenaire.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MobilePartenaireInscriptionStateWs {
    private Boolean subscriptionState;
    private Boolean pinState;
    private Boolean civilState;
    private Boolean subscribed;
    private Boolean countryState;
    private Boolean emailVerified;
    private Boolean phoneNumberVerified;
    private MobilePartenaireWs partenaire;

    private Boolean kycValidator;
}
