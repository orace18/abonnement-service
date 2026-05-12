package com.clubchretien.abonnement.core.corporate.ws;

import com.owomi.commons.ws.DeviseWs;
import com.owomi.commons.ws.PlanComptableWs;
import com.owomi.commons.ws.acm.AcmSoldeWs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MobileCorporateWs {
    private Integer id;
    private String matricule;
    private Integer numeroCompteId;
    private String libelle;
    private DeviseWs devise;
    private AcmSoldeWs solde;
    private Boolean owner;
    private boolean isDefault;
    private String natureCorporateLibelle;
    private String typeCorporateLibelle;
    private PlanComptableWs numeroCompte;
//    private NatureCorporateWs natureCorporate;
//    private TypeCorporateWs typeCorporate;

}
