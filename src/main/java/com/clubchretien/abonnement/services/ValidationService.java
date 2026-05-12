package com.clubchretien.abonnement.services;

import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PartenaireWs;
import com.owomi.commons.ws.StepValidationCreateWs;
import com.owomi.commons.ws.ValidationWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.BoutiqueShopizerRepository;
import com.owomi.database.repositories.ImageRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.ValidationRepository;
import com.owomi.rest.client.Validation.ValidationClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationService {
    private static Logger logger = LogManager.getLogger(ValidationService.class);
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


    @Autowired
    ValidationClient validationClient;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    ImageRepository imageRepository;

    @Autowired
    PartenaireIdentityService partenaireIdentityService;

    @Autowired
    Mapper mapper;


    public ValidationWs convertValidationToWs(Validation entity, Partenaire partenaire) {
        if (entity == null) return null;
        ValidationWs result = mapper.map(entity, ValidationWs.class);
        if (partenaire != null) result.setPartenaire(mapper.map(partenaire, PartenaireWs.class));
        return result;
    }


    public CommonResponseWs createStepValidation(StepValidationCreateWs ws) throws DataNotFoundException, AuthentificationException, OwomiException {
        return validationClient.createStepValidate(ws);
    }

    public StepValidationCreateWs stepValidate(Integer idValidation, List<String> stores){
        StepValidationCreateWs ws = new StepValidationCreateWs();
        Validation validation = validationRepository.findOneById(idValidation);
        if(validation == null) return null;
        ws.setMarketplaces(stores);
        ws.setIdValidation(idValidation);
        ws.setNature(validation.getNature());
        return ws;
    }
    public CommonResponseWs createValidation(ValidationWs ws) throws OwomiException {
        return validationClient.createValidate(ws);
    }
    public CommonResponseWs createMarketplaceValidation(List<String> stores, Integer idValidation) throws OwomiException {
        return validationClient.createMarketplaceValidate(stores, idValidation);
    }

    public CommonResponseWs createStepValidationUnique(Integer idValidation, List<String> stores) throws OwomiException {
        return validationClient.stepCreateValidateUnique(idValidation, stores, null);
    }
    private CommonResponseWs stepCreateValidate(Integer idValidation, List<String> stores) throws OwomiException {
        return validationClient.stepCreateValidate(idValidation, stores);
    }

    public void inSendValidation(Partenaire partenaire, Validation validation){
        try {
            BoutiqueShopizer boutiqueShopizer = boutiqueShopizerRepository.findOneByPaysAndDefautIsTrue(partenaire.getPays());
            List<String> stores = new ArrayList<>();
            if(boutiqueShopizer != null) stores.add(boutiqueShopizer.getCodeBoutique());
            else stores.add("DEFAULT");
            this.createMarketplaceValidation(stores, validation.getId());
            if(validation.getTableref().equalsIgnoreCase(Corporate.class.getSimpleName()) || validation.getTicketvalidation().equalsIgnoreCase("identification"))
                this.createStepValidationUnique(validation.getId(), stores);
            if(validation.getTableref().equalsIgnoreCase(Depot.class.getSimpleName()))
                this.stepCreateValidate(validation.getId(), stores);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
