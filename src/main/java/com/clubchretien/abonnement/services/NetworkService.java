package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.configs.ProductsInfos;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.PannierProduitStatusEnums;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PanierWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.PanierProduit;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.ProduitPays;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.dataservice.DataServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Calendar;

@Component
public class NetworkService implements MobileConstant {
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    PanierService panierService;
    @Autowired
    OperationService operationService;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    ProductsInfos productsInfos;
    @Autowired
    CommandespRepository commandespRepository;
    @Autowired
    PanierRepository panierRepository;
    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired
    PaysRepository paysRepository;
    @Autowired
    I18nFactory i18n;


    //@Transactional
    public CommonResponseWs payMemberShipFees(RequestContextWs requestContext, Partenaire partenaire, Integer idCorporate) throws DataNotFoundException, OwomiException {
        return payProcessing(requestContext, partenaire, idCorporate, productsInfos.getFraisInscription(), "FRAIS_ADHESION",1);
    }

    @Transactional
    public CommonResponseWs payMaintenanceFees(RequestContextWs requestContext, Partenaire partenaire, Integer idCorporate, Integer quantite) throws OwomiException, DataNotFoundException {
        return payProcessing(requestContext, partenaire, idCorporate, productsInfos.getFraisMaintenance(), "FRAIS_MAINTENANCE",quantite);
    }

    @Transactional
    public CommonResponseWs payMaintenanceFeesUsingCorporateMatricule(RequestContextWs requestContext, Partenaire partenaire, String matricule, Integer quantite) throws OwomiException, DataNotFoundException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        if(corporate == null) throw new OwomiException("", ErrorCodeEnums.DATA_NOT_FOUND, "");
        return payProcessing(requestContext, partenaire, corporate.getId(), productsInfos.getFraisMaintenance(), "FRAIS_MAINTENANCE",quantite);
    }

    private CommonResponseWs payProcessing(RequestContextWs requestContext, Partenaire partenaire, Integer idCorporate, String produitName, String sdk,Integer quantite) throws DataNotFoundException, OwomiException {
        Calendar lastWeekDay = Calendar.getInstance();
        Calendar firstWeekDay = Calendar.getInstance();
        firstWeekDay.set(Calendar.HOUR_OF_DAY, 0);
        firstWeekDay.set(Calendar.MINUTE, 0);
        firstWeekDay.set(Calendar.SECOND, 0);
        firstWeekDay.set(Calendar.HOUR, -168);
        if (!produitName.equals(productsInfos.getFraisMaintenance())) {
            Double weekCv = commandespRepository.getCommissionsVolumeByPeriode(partenaire, firstWeekDay.getTime(), lastWeekDay.getTime());
            weekCv = (weekCv == null) ? 0.0 : weekCv;
            if (weekCv >= 0.0 && weekCv < 20.0) throw new OwomiException(i18n.internationalize("pay.process.grade.error.message1"), ErrorCodeEnums.CV_INITIAL_NOT_FOUND, i18n.internationalize("pay.process.grade.error.message2"));
        }
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        if(!corporate.isStatut()) throw new OwomiException(i18n.internationalize("pay.process.corporate.not.active.message1"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("pay.process.corporate.not.active.message2", corporate.getMatricule()));
        ProduitPays produitPays = produitPaysRepository.findOneByProduit_NomAndPays_Devise(produitName,partenaire.getPays().getDevise());
        EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, "Nom", produitName);
        PanierProduit panierProduits1 = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE.name()).stream().reduce((first, last)->last).orElse(null);
        if(panierProduits1 != null ) {dataServiceClient.changeStatutOfPanierProduit(requestContext,panierProduits1.getReference(), PannierProduitStatusEnums.DESIR_PAYER_ECHEC_CORPORATE.name()); }
        PanierProduit panierProduit = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.name()).stream().reduce((first, last)->last).orElse(null);
        if (panierProduit != null && produitName.equalsIgnoreCase(productsInfos.getFraisInscription())) throw new OwomiException(i18n.internationalize("pay.process.adhesion.already.paid.message1"), ErrorCodeEnums.FRAIS_ADHESION_ALREADY_BUY_ERROR, i18n.internationalize("pay.process.adhesion.already.paid.message2"));
        PanierWs panierWs = new PanierWs();
        panierWs.setReference(sdk);
        panierWs.setQuantite(quantite);
        panierWs.setCode(produitPays.getCode());
        panierProduit = panierService.panierProcess(requestContext,partenaire, panierWs);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        return operationService.stepOneToBuy(requestContext,partenaire, ids, idCorporate,partenaire.getPays().getDevise().getCode(),0);
    }

}
