package com.clubchretien.abonnement.core.partenaire.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MobilePartenaireCountryCreateWs {
    private Integer partenaireId;
    private String phoneCodeNumber;
}
