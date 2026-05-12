package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PermissionResponseWs;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.CorporateRepository;
import com.owomi.rest.client.permissionsService.PermissionsServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarchandService {

    @Autowired
    PermissionsServiceClient permissionsServiceClient;
    @Autowired
    CorporateRepository corporateRepository;

    public CommonResponseWs responsePermissionMarchand(PermissionResponseWs permissionResponseWs, String pin, Partenaire partenaire) throws OwomiException, DataNotFoundException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
        if(!statusPin) throw new OwomiException("", ErrorCodeEnums.CHECK_PIN_ERROR,"");
        Corporate corporate = corporateRepository.findOneByMatricule(permissionResponseWs.getCode());
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,"matricule", permissionResponseWs.getCode());
        if(permissionResponseWs.getReponse()){
            corporate.setAutorise(Boolean.TRUE);
            corporateRepository.save(corporate);
            commonResponseWs = permissionsServiceClient.reponsePin(permissionResponseWs);
        }else {
            commonResponseWs =permissionsServiceClient.reponsePin(permissionResponseWs);
        }

        return commonResponseWs ;
    }
}
