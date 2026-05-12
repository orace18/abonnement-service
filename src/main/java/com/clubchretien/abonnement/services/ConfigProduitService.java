package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.utils.UtilsService;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.ConfigProduit;
import com.owomi.database.repositories.ConfigProduitRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigProduitService {

    @Autowired
    ConfigProduitRepository configProduitRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    UtilsService utils;


    //Cette fonction permet de créer une configProduit et de la sauvegarder dans la base de donnée elle prend en parametre
    // l'Object "ConfigProduitWs" pas besoin de spécifier l'id car il est auto-généré lors de la création elle retourne l'id de la categorie créé.
    @Transactional
    public ConfigProduitResponseWs createConfigProduit(RequestContextWs requestContext,ConfigProduitWs configProduitWs) throws ParseException {

        ConfigProduit configProduit = configProduitRepository.save(utils.createConfigProduit(requestContext,configProduitWs));
        ConfigProduitResponseWs configProduitResponseWs = new ConfigProduitResponseWs();
        configProduitResponseWs.setId(configProduit.getId());
        return configProduitResponseWs;
    }

    //Cette fonction permet de recupérer une configProduit dans la base de donnée grace a son id qu'elle prend en parametre;
    // au cas ou l'id de cette configProduit n'existe pas un message d'erreur est envoyé le signalant.
    @Transactional
    public ConfigProduitWs getConfigProduit(RequestContextWs requestContext,int id) throws OwomiException {

        ConfigProduit configProduit = configProduitRepository.findOneById(id);
        if(configProduit == null) throw new OwomiException("",ErrorCodeEnums.GET_CONFIG_PRODUIT_FROM_DATABASE_ERROR,"");
        ConfigProduitWs configProduitWs = mapper.map(configProduit, ConfigProduitWs.class);
        ProduitPaysWs produitPaysWs = utils.createProduitPaysWs(requestContext,configProduit.getProduitpays());
        configProduitWs.setProduitpays(produitPaysWs);
        TypeOperationWs typeOperationWs = mapper.map(configProduit.getTypeoperation(), TypeOperationWs.class);
        configProduitWs.setTypeoperation(typeOperationWs);
        return configProduitWs;
    }


    //Cette fonction permet de recupérer la liste de toutes les configProduit présente dans la base de donnée.
    @Transactional
    public List<ConfigProduitWs> getConfigProduitList(RequestContextWs requestContext){

        List<ConfigProduit> configProduitList = configProduitRepository.findAll();
        List<ConfigProduitWs> configProduitWsList =  new ArrayList<>();
        for(ConfigProduit configProduit : configProduitList){
            ConfigProduitWs configProduitWs = mapper.map(configProduit, ConfigProduitWs.class);
            ProduitPaysWs produitPaysWs = utils.createProduitPaysWs(requestContext,configProduit.getProduitpays());
            configProduitWs.setProduitpays(produitPaysWs);
            TypeOperationWs typeOperationWs = mapper.map(configProduit.getTypeoperation(), TypeOperationWs.class);
            configProduitWs.setTypeoperation(typeOperationWs);
            configProduitWsList.add(configProduitWs);
        }
        return configProduitWsList;
    }

    //Cette methode permet de modifié une configProduit et de la sauvegarder dans la base de donnée elle prend en parametre
    // l'Object "ConfigProduitWs" ici spécifiez l'id de la configProduit à modifié avec les nouvelles valeurs.
    @Transactional
    public void updateConfigProduit(RequestContextWs requestContext, ConfigProduitWs configProduitWs) throws ParseException {

        configProduitRepository.save(utils.updateConfigProduit(requestContext,configProduitWs));

    }

    //Cette methode permet de supprimer une configProduit dans la base de donnée elle prend en parametre l'id de la configProduit
    @Transactional
    public void deleteConfigProduit(RequestContextWs requestContext, int id){
        configProduitRepository.deleteById(id);
    }

}
