package com.clubchretien.abonnement.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeSuppressionPartenaireWs {

    private Integer partenaireId;
    private String raison;
    private String avis;
}
