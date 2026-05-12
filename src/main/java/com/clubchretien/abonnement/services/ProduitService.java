package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.UtilsService;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.NatureCorporateEnums;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CategorieWs;
import com.owomi.commons.ws.ProduitResponseWs;
import com.owomi.commons.ws.ProduitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Categorie;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.Produit;
import com.owomi.database.repositories.*;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


@Component
public class ProduitService {
    @Autowired
    ProduitRepository produitRepository;
    @Autowired
    Mapper mapper;
    @Autowired
    UtilsService utils;
    @Autowired
    CategorieRepository categorieRepository;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    GrandLivreRepository grandLivreRepository;
    @Autowired
    I18nFactory i18n;



    //Cette fonction permet de créer un produit et de le sauvegarder dans la base de donnée elle prend en parametre
    // l'Object "ProduitWs" pas besoin de spécifier l'id car il est auto-généré lors de la création elle retourne l'id et le nom du produit créé.
    @Transactional
    public ProduitResponseWs createProduit(RequestContextWs requestContext,ProduitWs produitWs) throws ParseException, OwomiException {
        Corporate corporate = mapper.map(produitWs.getCorporateWs(), Corporate.class);
        if (!corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature())) throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
        Produit produit = utils.createProduit(requestContext,produitWs);
        produit = produitRepository.save(produit);
        ProduitResponseWs produitResponseWs = new ProduitResponseWs();
        produitResponseWs.setId(produit.getId());
        produitResponseWs.setNom(produit.getNom());
        return produitResponseWs;
    }


    //Cette fonction permet de recupérer un produit dans la base de donnée grace a son id qu'elle prend en parametre;
    // au cas ou l'id de cet produit n'existe pas un message d'erreur est envoyé le signalant.
    @Transactional
    public ProduitWs getProduit(RequestContextWs requestContext,Integer id, Integer idCorporate) throws OwomiException {
		if (!corporateRepository.findOneById(idCorporate).getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
			throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
		Produit produit = produitRepository.findOneById(id);
		if (produit == null) throw new OwomiException("", ErrorCodeEnums.GET_PRODUIT_FROM_DATABASE_ERROR, "");
		ProduitWs produitWs = utils.createProduitWs(requestContext, produit);
		return produitWs;

	}


    //Cette fonction permet de recupérer la liste de tout les produit présent dans la base de donnée.
    @Transactional
    public List<ProduitWs> getProduitList(RequestContextWs requestContext,Integer idCorporate) throws OwomiException {
		if (!corporateRepository.findOneById(idCorporate).getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
			throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
        List<Produit> produitList = produitRepository.findAll();
        List<ProduitWs> produitWsList = new ArrayList<>();

        for (Produit produit : produitList){
            ProduitWs produitWs = utils.createProduitWs(requestContext,produit);
            produitWsList.add(produitWs);
        }

        return produitWsList;

    }


    //Cette fonction permet de recupérer la liste de tout les categorie présente dans la base de donnée.
    @Transactional
    public List<CategorieWs> getCategorieList(RequestContextWs requestContext, Integer idCorporate) throws OwomiException {
		if (!corporateRepository.findOneById(idCorporate).getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
			throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
        List<Categorie> categorieList = categorieRepository.findAll();
        List<CategorieWs> categorieWsList = new ArrayList<>();
        for (Categorie categorie : categorieList){
            CategorieWs categorieWs = mapper.map(categorie, CategorieWs.class);
            categorieWsList.add(categorieWs);
        }
        return categorieWsList;
    }



    //Cette methode permet de modifié un produitPays et de le sauvegarder dans la base de donnée elle prend en parametre
    // l'Object "ProduitPaysWs" ici spécifiez l'id du produitPays à modifié avec les nouvelles valeurs.
    @Transactional
    public void updateProduit(RequestContextWs requestContext, ProduitWs produitWs, Integer idCorporate) throws ParseException, OwomiException {
		if (!corporateRepository.findOneById(idCorporate).getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
			throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
        produitRepository.save(utils.updateProduit(requestContext,produitWs));
    }


    //Cette methode permet de supprimer un produit dans la base de donnée elle prend en parametre l'id du produit
    @Transactional
    public void deleteProduit(RequestContextWs requestContext, Integer id, Integer idCorporate) throws OwomiException {
		if (!corporateRepository.findOneById(idCorporate).getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
			throw new OwomiException(i18n.internationalize("corporate.not.marchand.ex.message"));
        produitRepository.deleteById(id);
    }


}
