package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.UtilsService;
import com.google.gson.Gson;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.PannierProduitStatusEnums;
import com.owomi.commons.enums.RetraitStatusEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.clubchretien.abonnement.constants.MobileConstant.SESSION_DURATION;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;

@Component
public class PanierService {

    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    PanierRepository panierRepository;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    UtilsService utilsService;
    @Autowired
    ConfigProduitRepository configProduitRepository;
    @Autowired
    OperationService operationService;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    DeviseRepository deviseRepository;
    @Autowired
    I18nFactory i18n;


    @Autowired
    UserService userService;

    DataServiceClient dataServiceClient;

    private static Logger logger = LogManager.getLogger(PanierService.class);

    public PanierService() {
    }


    /**
     * get hashed panier data , decrypt and add to panier
     * @param request
     * @param hashedPanier
     * @return
     * @throws OwomiException
     * @throws DataNotFoundException
     */
    @Transactional
    public CommonResponseWs getCommandInfo(HttpServletRequest request, String hashedPanier) throws OwomiException, DataNotFoundException, AuthentificationException {
        logger.info("CRYPTED DATA : "+hashedPanier);
        logger.info("BEGING PANIER PROCEE ");
        logger.info("GETTNG PRIVATE KEY  ");
        CommonResponseWs responseWs= new CommonResponseWs();
        Partenaire partenaire = userService.getPartenaire(request);
		String key = utilsService.getPrivateKey(partenaire.getId(), request);
        logger.info("PRIVATE  KEY =======> "+key);
        if (key==PRIVATE_KEY_CODE_NOT_FOUND) {
            logger.info("GENERATING NEW PRIVATE  KEY =======> "+key);
            key = utilsService.createKey(partenaire.getId());
            responseWs.setData(key);
            responseWs.setStatus(WsOperationResponseStatus.inProcess);
            responseWs.setCode(ErrorCodeEnums.PRIVATE_KEY_CODE_NOT_FOUND_ERROR.getErrorCode());
            return responseWs;
        }
        PanierInfo panierInfo = decryptHashedPanier(key,hashedPanier);
        logger.info("DECRYPTED DATA : "+ new Gson().toJson(panierInfo));
        RequestContextWs requestContext = userService.getRequestContext(request);
        return addToPanier(
                requestContext,
                partenaire,
                panierInfo.getPanierWs(),
                panierInfo.getIdCorporate(),
                panierInfo.getDeviseTransaction());

    }

    public PanierInfo decryptHashedPanier(String key,String hashedPanier){
        String operationDecrypt = utilsService.decrypt(hashedPanier,key);
        PanierInfo panierInfo = new Gson().fromJson(operationDecrypt, PanierInfo.class);
        return panierInfo;
    }
    @Transactional
    public CommonResponseWs addToPanier(RequestContextWs requestContext, Partenaire partenaire, List<PanierWs> panierWsList,String deviseTransaction) throws DataNotFoundException, OwomiException {
        Integer[] ids = new Integer[]{};
        ids = panierWsList.stream().map(panierWs -> {
            try {
                return panierProcess(requestContext, partenaire, panierWs).getId();
            }catch (DataNotFoundException exception){
                return 0;
            }
        }).collect(Collectors.toList()).toArray(ids);
        Corporate corporate = partenaire.getPartenaireCorporates().stream().filter(partenaireCorporate -> partenaireCorporate.getCorporate().getMatricule().equals(partenaire.getTelephone())).findFirst().get().getCorporate();
        return operationService.stepOneToBuy(requestContext,partenaire, ids, corporate.getId(),deviseTransaction,0);
    }

    public Session createSession () {
        logger.debug("In createSession ");
        Session session = new Session();
        session.setDateInitiation(new Date());
        session.setTime(new Timestamp(System.currentTimeMillis()));
        session.setStatus(RetraitStatusEnums.INITIAL.getStatus());
        session.setDateFinSession(new Date(session.getDateInitiation().getTime()+SESSION_DURATION));
        logger.debug("Out createSession ");
        return sessionRepository.save(session);
    }

    @Transactional
    public CommonResponseWs addToPanier(RequestContextWs requestContext, Partenaire partenaire, PanierWs panierWs, Integer idCorporate, String deviseTransaction) throws DataNotFoundException, OwomiException {
        logger.info("START ADDING PRODUCT");
		Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        if (!corporate.isStatut()) throw new OwomiException(i18n.internationalize("panier.ex.inactive.corporate.message1"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("panier.ex.inactive.corporate.message2", corporate.getMatricule()));
        PanierProduit panierProduit = panierProcess(requestContext,partenaire, panierWs);
        panierProduit.setStatus(PannierProduitStatusEnums.DESIR_PAYER.name());
        panierProduit.setSession(createSession());
        panierProduit.setCorporate(corporate);
        panierProduitRepository.save(panierProduit);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        logger.info("PRODUCT HAS BEEN SAVE");
        return operationService.stepOneToBuy(requestContext,partenaire, ids, idCorporate,deviseTransaction,0);
    }

    @Transactional
    public CommonResponseWs directPay(RequestContextWs requestContext, Partenaire partenaire, PanierWs panierWs, Integer idCorporate,String deviseTransaction) throws DataNotFoundException, OwomiException {
		Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        if (!corporate.isStatut()) throw new OwomiException(i18n.internationalize("panier.ex.inactive.corporate.message1"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("panier.ex.inactive.corporate.message2", corporate.getMatricule()));
        PanierProduit panierProduit = panierProcess(requestContext, partenaire, panierWs);
        panierProduit.setStatus(PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE.name());
        panierProduit.setCorporate(corporate);
        String commandReference = StringUtils.generateCommandReference();
        panierProduit.setReferenceCommande(commandReference);
        panierProduitRepository.save(panierProduit);
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setReference(commandReference);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setInfo(operationService.getResumeAchat(requestContext, panierProduit,deviseTransaction,partenaire.getPays().getDevise().getCode(),partenaire.getPays().getDevise().getLibelle(),0));
        commonResponseWs.setMessage(i18n.internationalize("direct.pay.message")+requestContext);
        return commonResponseWs;
    }


    public PanierProduit panierProcess(RequestContextWs requestContext, Partenaire partenaire, PanierWs panierWs)throws DataNotFoundException{
        logger.debug("value of panier ws is " + panierWs);
        Panier panier = panierRepository.findOneByPartenaire(partenaire);
        if(panier == null){
            panier = new Panier();
            panier.setPartenaire(partenaire);
            panier.setStatut(0);
            panier.setDirect(Boolean.FALSE);
            panier = panierRepository.save(panier);
        }
        ProduitPays produitPays = produitPaysRepository.findOneByCode(panierWs.getCode());
        EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, "code", panierWs.getCode());
        PanierProduit panierProduit = new PanierProduit();
        panierProduit.setPanier(panier);
        panierProduit.setProduitPays(produitPays);
        panierProduit.setQuantite(panierWs.getQuantite());
        panierProduit.setFormstring(panierWs.getFormString());
        panierProduit.setReference(panierWs.getReference());
        panierProduit.setSession(createSession());
        panierProduit.setStatus(PannierProduitStatusEnums.AJOUTER_AU_PANIER.name());
        panierProduit = panierProduitRepository.save(panierProduit);
        return panierProduit;
    }

    //Ce service reçois en paramètre un partenaire et panier prestashop et retour une list d'une list de panierProduit et l'objet PrestashopWsinfo
    @Transactional
    public  CommonResponseWs addToPanier(RequestContextWs requestContext, Partenaire partenaire, PanierPrestashopWs panierPrestashopWs, String deviseTransaction) throws DataNotFoundException, OwomiException {
        if(panierPrestashopWs.getPanierWsList().isEmpty()) throw new OwomiException(i18n.internationalize("panier.ex.empty.panier.message1"), ErrorCodeEnums.ADD_TO_PANIER_ERROR, i18n.internationalize("panier.ex.empty.panier.message2"));
        if(panierPrestashopWs.getPrestashopInfoWs() == null) throw new OwomiException(i18n.internationalize("panier.ex.prestashop.absent.message1"), ErrorCodeEnums.ADD_TO_PANIER_ERROR, i18n.internationalize("panier.ex.prestashop.absent.message2"));
        String prestashopInfo = new Gson().toJson(panierPrestashopWs.getPrestashopInfoWs());
        //String deviseLivraison=panierPrestashopWs.getCodeDeviseBoutique();
        Devise devise = deviseRepository.findOneByCode(deviseTransaction);
        logger.debug("devise livraison is " + deviseTransaction);
        ProduitPays produitPays = produitPaysRepository.findOneByProduit_NomAndPays_Devise(panierPrestashopWs.getPrestashopInfoWs().getVilleLivraison(),devise);
        EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, "Nom du produit", panierPrestashopWs.getPrestashopInfoWs().getVilleLivraison());
        PanierWs fraisLivraison = new PanierWs();
        fraisLivraison.setCode(produitPays.getCode());
        fraisLivraison.setQuantite(Integer.parseInt(panierPrestashopWs.getPrestashopInfoWs().getAmount()));
        fraisLivraison.setReference("1-PRESTASHOP");
        panierPrestashopWs.getPanierWsList().add(fraisLivraison);
        Integer[] ids = new Integer[]{};
        ids = panierPrestashopWs.getPanierWsList().stream().map(panierWs -> {
            try {
                PanierProduit panierProduit = panierProcess(requestContext, partenaire, panierWs);
                panierProduit.setFormstring(prestashopInfo);
                panierProduit.setSession(createSession());
                panierProduitRepository.save(panierProduit);
                return panierProduit.getId();
            }catch (DataNotFoundException exception){
                return 0;
            }
        }).collect(Collectors.toList()).toArray(ids);
        Corporate corporate = partenaire.getPartenaireCorporates().stream().filter(partenaireCorporate -> partenaireCorporate.getCorporate().getMatricule().equals(partenaire.getTelephone())).findFirst().get().getCorporate();
        return operationService.stepOneToBuy(requestContext, partenaire, ids, corporate.getId(),deviseTransaction, Integer.valueOf(panierPrestashopWs.getPrestashopInfoWs().getId_order()));
    }

    @Transactional
    public CommonResponseWs deleteToPanier(RequestContextWs requestContext, int idPanierProduit) throws DataNotFoundException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
		PanierProduit panierProduit = panierProduitRepository.findOneById(idPanierProduit);
        EntityUtils.checkIfEntityIsNull(PanierProduit.class, panierProduit, idPanierProduit);
        panierProduitRepository.delete(panierProduit);
        return commonResponseWs;
    }

    @Transactional
    public PanierWs updateToPanier(RequestContextWs requestContext, PanierWs panierWs) throws DataNotFoundException {
		PanierProduit panierProduit = panierProduitRepository.findOneById(panierWs.getId());
        EntityUtils.checkIfEntityIsNull(PanierProduit.class, panierProduit, panierWs.getId());
        panierProduit.setQuantite(panierWs.getQuantite());
        panierProduit.setFormstring(panierWs.getFormString());
        panierProduit.setReference(panierWs.getReference());
        panierProduit = panierProduitRepository.save(panierProduit);
        Map<String, Double> result = utilsService.getFraisAndCv(requestContext,panierProduit.getProduitPays(), panierProduit.getQuantite());
        return new PanierWs(panierProduit.getId(), panierProduit.getProduitPays().getCode(), "", panierProduit.getQuantite(), 0.0, "", result.get("Frais")+" FCFA", result.get("Cv")+" CV", result.get("TotalHorsTaxes")+" FCFA");
    }

    @Transactional
    public List<PanierWs> panier(RequestContextWs requestContext, Partenaire partenaire){
        List<PanierProduit> panierProduitList = panierProduitRepository.findByPanier_Partenaire(partenaire);
        return map(requestContext,panierProduitList);
    }

    private List<PanierWs> map(RequestContextWs requestContext,List<PanierProduit> panierProduitList){
       return panierProduitList.stream().map(panierProduit -> {
           Map<String, Double> result = utilsService.getFraisAndCv(requestContext,panierProduit.getProduitPays(), panierProduit.getQuantite());
           PanierWs panierWs = new PanierWs();
           panierWs.setId(panierProduit.getId());
           panierWs.setCode(panierProduit.getProduitPays().getCode());
           panierWs.setFrais(result.get("Frais")+" FCFA");
           panierWs.setCv(result.get("Cv")+" CV");
           panierWs.setTotalHorsTaxes(result.get("TotalHorsTaxes")+" FCFA");
           panierWs.setFormString(panierProduit.getFormstring());
           panierWs.setQuantite(panierProduit.getQuantite());
           return panierWs;
       }).collect(Collectors.toList());
    }
}
