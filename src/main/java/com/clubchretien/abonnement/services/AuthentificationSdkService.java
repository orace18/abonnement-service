package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.SdkTokenResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.SdkToken;
import com.owomi.database.repositories.SdkTokenRepository;
import com.owomi.rest.client.auth.AuthentificationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AuthentificationSdkService {
    private static Logger logger = LogManager.getLogger(AuthentificationSdkService.class);
    @Autowired
    private SdkTokenRepository sdkTokenRepository;
    @Autowired
    AuthentificationUtils authentificationUtils;

    public SdkTokenResponseWs getToken(RequestContextWs requestContext, Partenaire partenaire, String idSdk) throws AuthentificationException {
        String code = authentificationUtils.getCode();
        logger.debug("code ==> "+code);
        String token = authentificationUtils.getToken(code);
        logger.debug("token ==> "+token);
        SdkToken sdkToken = createSdkToken(partenaire, idSdk, token);
        return  new SdkTokenResponseWs(WsOperationResponseStatus.success, sdkToken.getTokengenere());
    }

    private SdkToken createSdkToken(Partenaire partenaire, String idSdk, String token){
        SdkToken sdkToken = new SdkToken();
        sdkToken.setDateemission(new Date());
        sdkToken.setDejautilise(Boolean.FALSE);
        sdkToken.setIdsdk(idSdk);
        sdkToken.setPartenaire(partenaire);
        sdkToken.setTokengenere(token);
        return sdkTokenRepository.save(sdkToken);
    }
}
