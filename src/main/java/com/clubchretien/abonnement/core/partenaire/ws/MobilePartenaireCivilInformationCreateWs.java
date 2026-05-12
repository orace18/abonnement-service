package com.clubchretien.abonnement.core.partenaire.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MobilePartenaireCivilInformationCreateWs {
    private Integer partenaireId;
    private String nom;
    private String prenoms;
    private String sexe;
    private String datenaissance;
    private String photoProfil;

}
