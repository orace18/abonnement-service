package com.clubchretien.abonnement.services;


import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.PartenaireWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class InfoService {

    private static Logger logger = LogManager.getLogger(InfoService.class);

    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    Mapper mapper;


    public PartenaireWs getPartenaireInfo(RequestContextWs requestContext, String numeroTelephone) throws DataNotFoundException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(numeroTelephone);
        EntityUtils.checkIfEntityIsNull(Partenaire.class, partenaire, "telephone", numeroTelephone);
        PartenaireWs partenaireWs = mapper.map(partenaire, PartenaireWs.class);

        return  partenaireWs;
    }

}
