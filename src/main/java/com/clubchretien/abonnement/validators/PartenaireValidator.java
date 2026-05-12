package com.clubchretien.abonnement.validators;

import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.CreatePartenaireStepOneWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Pays;
import com.owomi.database.repositories.PaysRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.owomi.commons.utils.PartenairePhoneNumberUtils.checkIfNumberExist;

@Component
public class PartenaireValidator {
    @Autowired
    PaysRepository paysRepository;
    private static Logger logger = LogManager.getLogger(PartenaireValidator.class);
    public Pays validate(RequestContextWs requestContext,CreatePartenaireStepOneWs createPartenaireStepOneWs) throws OwomiException, DataNotFoundException {
        Pays pays = paysRepository.findOneByIndicatif(createPartenaireStepOneWs.getIndicatif());
        EntityUtils.checkIfEntityIsNull(Pays.class, pays,"indicatif" ,createPartenaireStepOneWs.getIndicatif());
        validatePaysIndicatif(requestContext,createPartenaireStepOneWs);
        validatePhoneNumber(requestContext, createPartenaireStepOneWs, pays);
        return pays;
    }

    private void validatePaysIndicatif(RequestContextWs requestContext,CreatePartenaireStepOneWs createPartenaireStepOneWs){

    }

    private void validatePhoneNumber(RequestContextWs requestContext,CreatePartenaireStepOneWs createPartenaireStepOneWs,  Pays pays) throws OwomiException {
        if (createPartenaireStepOneWs.getNumero() == null) return;
        if (createPartenaireStepOneWs.getNumero().trim().isEmpty()) return;
        if (createPartenaireStepOneWs.getNumero().trim().length() != 15)
            checkIfNumberExist(createPartenaireStepOneWs.getIndicatif(), createPartenaireStepOneWs.getNumero(), pays.getCode());
    }


}
