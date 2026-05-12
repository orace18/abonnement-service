package com.clubchretien.abonnement.core.partenaire.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MobilePartenaireStateRequestWs {
    private String firebaseToken;
    private String sessionToken;
    private String deviceToken;
}
