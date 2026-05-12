package com.clubchretien.abonnement.core.payment.Services;

import com.google.gson.Gson;
import com.owomi.commons.enums.PaymentTypeEnums;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DemandePaiementMetaDataWs;
import com.owomi.commons.ws.DemandePaiementWs;
import com.owomi.database.entities.Partenaire;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class PaymentJawupayService {
    private static Logger logger = LogManager.getLogger(PaymentJawupayService.class);

    /*public CommonResponseWs initialisation(Partenaire partenaire, InitPaymentWs ws){

        DemandePaiementWs demandePaiementWs = new DemandePaiementWs();
        demandePaiementWs.setType(PaymentTypeEnums.PAYMENT.getValue());
        demandePaiementWs.setMessage(ws.getMessage());
        demandePaiementWs.setIdPartenaire(partenaire.getId());
        demandePaiementWs.setRefClient(ref);
        demandePaiementWs.setMontant(createRechargeWs.getMontant());
        demandePaiementWs.setDevise(createRechargeWs.getDevise());
        demandePaiementWs.setClientId(clientRecharge.getId());
        demandePaiementWs.setClientSecret(clientRecharge.getSecret());
        demandePaiementWs.setCallbackUrl(this.getCallbackUrlRecharge());
        DemandePaiementMetaDataWs metaData = new DemandePaiementMetaDataWs();
        String metaDataStr = new Gson().toJson(metaData);
        demandePaiementWs.setMetadata(metaDataStr);

    }*/

}
