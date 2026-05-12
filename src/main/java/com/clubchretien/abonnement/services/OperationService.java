package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.exception.SessionExpireException;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.UtilsService;
import com.google.gson.Gson;
import com.owomi.commons.constants.OperationConstant;
import com.owomi.commons.enums.DevisesEnums;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.PannierProduitStatusEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.*;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.message.publisher.publishers.CommandeActiveMqPublisher;
import com.owomi.message.publisher.publishers.CorporateProcessorActiveMqPublisher;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.receiver.CommandeComponentServiceClient;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;

@Component
public class OperationService implements OperationConstant {
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    ConfigProduitRepository configProduitRepository;
    @Autowired
    TransactionFinanciereService transactionFinanciereService;
    @Autowired
    PanierRepository panierRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    CommandespRepository commandespRepository;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    OperationStringRepository operationStringRepository;
    @Autowired
    AutorisationsRepository autorisationsRepository;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    PlanComptableRepository planComptableRepository;
    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired
    GradesRepository gradesRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    UtilsService utilsService;
    @Autowired
    RetraitRepository retraitRepository;
    @Autowired
    CorporateProcessorActiveMqPublisher corporateProcessorActiveMqPublisher;
    @Autowired
    AccountSoldeRepository accountSoldeRepository;
    @Autowired
    AccountServiceClient accountServiceClient;
    @Autowired
    ShopizerServiceClient shopizerServiceClient;
	private static Logger logger = LogManager.getLogger(OperationService.class);
    @Autowired
	I18nFactory i18n;
	@Autowired
	I18nCommonsFactory i18nCommons;
	@Autowired
	Mapper mapper;
	@Autowired
	CommandeActiveMqPublisher commandeActiveMqPublisher;
	@Autowired
	PartenaireCorporateRepository partenaireCorporateRepository;
	@Autowired
	CommandeComponentServiceClient commandeComponentServiceClient;
	@Autowired
	UserService userService;

	@Transactional
	public CommonResponseWs stepTwoToBuyProcess(HttpServletRequest request, String hashedOperation) throws OwomiException, SessionExpireException, AuthentificationException, DataNotFoundException {
		CommonResponseWs responseWs = new CommonResponseWs();
		Partenaire partenaire = userService.getPartenaire(request);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
		logger.debug("stepTwoToBuyProcess " + partenaire.getAutoriserResetPin());
		boolean authorized = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
		logger.debug("stepTwoToBuyProcess " + authorized);
		if (!authorized) {
			responseWs.setReference(ErrorCodeEnums.PIN_RESET_ERROR.getErrorDescription());
			responseWs.setMessage(i18nCommons.internationalize("CHANGE_PIN"));
			responseWs.setStatus(WsOperationResponseStatus.rejected);
			responseWs.setInfo("PIN MUST BE CHANGE BEFORE ANY OPERATION");
			responseWs.setData(false);
			return responseWs;
		}

		Corporate corporate = partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true, partenaire).getCorporate();

        if(partenaire.getConnectedSansNumero() && corporate.getProvisoire()) throw new OwomiException("",ErrorCodeEnums.NEED_TO_ADD_NUMBER_ERROR,"");
        if(partenaire.getConnectedSansNumero() && !corporate.getProvisoire()) throw new OwomiException("",ErrorCodeEnums.NEED_TO_CHECK_NUMBER_ERROR,"");

		RequestContextWs requestContext = userService.getRequestContext(request);
		String key = utilsService.getPrivateKey(partenaire.getId(), request);
        if (key.equalsIgnoreCase(PRIVATE_KEY_CODE_NOT_FOUND)) {
			key = utilsService.createKey(partenaire.getId());
			responseWs.setData(key);
            responseWs.setStatus(WsOperationResponseStatus.inProcess);
            responseWs.setCode(ErrorCodeEnums.PRIVATE_KEY_CODE_NOT_FOUND_ERROR.getErrorCode());
            return responseWs;
        }
        String operationString =utilsService.decrypt(hashedOperation,key);
        OperationInfoWs operationInfo = new Gson().fromJson(operationString,OperationInfoWs.class);
        logger.debug("decryptage info ============================ " + operationInfo.getDevisTransaction());
        List<PanierProduit> panierProduits = panierProduitRepository.findByReferenceCommande(operationInfo.getCommandReference());
        logger.debug("step logging 1");
        if(checkSessionStatus(panierProduits.get(0).getSession())){
            logger.debug("step logging 2");
            return stepTwoToBuy(requestContext,partenaire,operationInfo.getPin(),operationInfo.getCommandReference(),operationInfo.getDevisTransaction());
        }else throw new SessionExpireException();
//        return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"Plateforme en maintenance !!!!",null,"",200);
    }

    public boolean checkSessionStatus(Session session){
        return new Timestamp(System.currentTimeMillis()).after(new Timestamp(session.getDateFinSession().getTime()));
    }

    @Transactional
    public CommonResponseWs stepOneToBuy(RequestContextWs requestContext, Partenaire partenaire, Integer[] ids, int idCorporate,String deviseTransaction,Integer idCommande) throws DataNotFoundException, OwomiException {
        logger.info("OPERATION STEP ON TO BUY");
		Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        List<PanierProduit> panierProduitList = getPanierProduits(requestContext, partenaire, ids);
        Map<String, Double> totaux = getTotalProduitAndFrais(requestContext, panierProduitList,deviseTransaction,partenaire.getPays().getDevise().getCode());
        updateStatePaymentByStatus(requestContext,panierProduitList, PannierProduitStatusEnums.DESIR_PAYER);
        double solde = transactionFinanciereService.extraitCompte(requestContext, corporate.getMatricule());
        return checkStatePayment(requestContext, solde, totaux.get(TOTAL_PRIX_PRODUIT) + totaux.get(TOTAL_FRAIS), panierProduitList, corporate,deviseTransaction,partenaire.getPays().getDevise().getCode(),partenaire.getPays().getDevise().getLibelle(),idCommande);
    }

    public CommonResponseWs stepOneToBuyDiffered(RequestContextWs requestContext, Partenaire partenaire, Integer[] ids,String deviseTransaction,Integer idCommande) throws DataNotFoundException, OwomiException {
        List<PanierProduit> panierProduitList = getPanierProduits(requestContext, partenaire, ids);
        updateStatePaymentByStatus(requestContext,panierProduitList, PannierProduitStatusEnums.DESIR_PAYER);
        PartenaireCorporate partenaireCorporate = partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true,partenaire);
        return checkStatePaymentDiffered(requestContext,partenaireCorporate.getCorporate(), panierProduitList,deviseTransaction,partenaire.getPays().getDevise().getCode(),partenaire.getPays().getDevise().getLibelle(),idCommande);
    }

    @Transactional
    public CommonResponseWs stepTwoToBuy(RequestContextWs requestContext, Partenaire partenaire, String pin, String reference,String deviseTransaction) throws OwomiException {
        CommonResponseWs responseWs = new CommonResponseWs();
        if(!partenaire.getAutoriserResetPin()){
            responseWs.setReference(ErrorCodeEnums.PIN_RESET_ERROR.getErrorDescription());
            responseWs.setMessage(i18nCommons.internationalize("CHANGE_PIN"));
            responseWs.setStatus(WsOperationResponseStatus.rejected);
            responseWs.setInfo("PIN MUST BE CHANGE BEFORE ANY OPERATION");
            responseWs.setData(false);
            return responseWs;
		}
		logger.debug("step logging 3");
		if (deviseTransaction.equals("TEST"))
			throw new OwomiException(i18n.internationalize("step.2.buy.update.ex.message"), ErrorCodeEnums.VERSION_ERROR, i18n.internationalize("step.2.buy.update.ex.message"));
		logger.debug("step logging 4");
		// if pinverification = 3  cancel operation and block corporate ( isActif = false) and RETURN errorCode (BLOCKED CORPORATE)
		if (deviseTransaction.equals("TEST"))
			throw new OwomiException("Veuillez mettre a jour l'application, cette version est obsolète !!!!", ErrorCodeEnums.VERSION_ERROR, "Veuillez mettre a jour l'application, cette version est obsolète !!!!");
		logger.debug("step logging 5");
		boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
		logger.debug("step logging 6");
		List<PanierProduit> panierProduits = panierProduitRepository.findByReferenceCommandeAndStatus(reference, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE.name());
		logger.debug("step logging 7 = " + panierProduits.size());
		if (statusPin) {
			updateStatePaymentByStatus(requestContext, panierProduits, PannierProduitStatusEnums.VERIFICATION_PIN_SUCCES);
			logger.debug("step logging 8");
			deviseTransaction = panierProduits.get(0).getProduitPays().getPays().getDevise().getCode();
			logger.debug("step logging 9");
			CreateCommandeForReceiverWs createCommandeForReceiverWs = map(requestContext, partenaire, reference, partenaire.getPays().getDevise().getCode(), deviseTransaction);
			logger.debug("step logging 10 = " + createCommandeForReceiverWs);
			this.createCommandeFuture(createCommandeForReceiverWs);
			//commandeActiveMqPublisher.send(createCommandeForReceiverWs);
			logger.debug("step logging 11");
			return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18nCommons.internationalize("OperationConstant.MSG_SUCCESS_VALID_PIN"), null, null, null);
		}else{
            panierProduits.forEach(t->t.setPinVerification(t.getPinVerification()+1));
			if (!panierProduits.isEmpty() && panierProduits.get(0).getPinVerification() == 3) {
				updateStatePaymentByStatus(requestContext, panierProduits, PannierProduitStatusEnums.VERIFICATION_PIN_ECHEC);
				blockCorporate(panierProduits.get(0).getCorporate().getId());
			}
			if (!panierProduits.isEmpty()) panierProduitRepository.saveAll(panierProduits);
//            updateStatePaymentByStatus(requestContext, panierProduits, PannierProduitStatusEnums.VERIFICATION_PIN_ECHEC);
			return new CommonResponseWs(WsOperationResponseStatus.error, null, null, MSG_FAILED_VALID_PIN, null, null, 4545);

		}
	}

	private void createCommandeFuture(CreateCommandeForReceiverWs ws) {
		CompletableFuture.runAsync(() -> {
			try {
				this.createCommande(ws);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void createCommande(CreateCommandeForReceiverWs ws) {
		try {
			this.commandeComponentServiceClient.createCommande(ws);
		} catch (Exception e) {
			e.printStackTrace();
			commandeActiveMqPublisher.send(ws);
		}
	}

	private Corporate blockCorporate(Integer idCorporate) {
		Corporate corporate = corporateRepository.findOneById(idCorporate);
		corporate.setActif(false);
		return corporateRepository.save(corporate);
	}

	@Transactional
	public CommonResponseWs corporateValidation(RequestContextWs requestContext, Partenaire partenaire, String pin, String referenceCommande) {
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
		if (statusPin) {
			List<PanierProduit> panierProduitList = panierProduitRepository.findByReferenceCommande(referenceCommande);
			updateStatePaymentByValidationCourant(requestContext, panierProduitList, 1);
			updateStatePaymentByPartenaires(requestContext, panierProduitList, partenaire.getId().toString());
			commonResponseWs.setStatus(WsOperationResponseStatus.success);
			commonResponseWs.setMessage(i18n.internationalize("corporate.validation.message"));
		} else {
			commonResponseWs.setStatus(WsOperationResponseStatus.error);
            commonResponseWs.setMessage(i18nCommons.internationalize("OperationConstant.MSG_FAILED_VALID_PIN"));
        }
        return commonResponseWs;
    }

    private List<PanierProduit> getPanierProduits(RequestContextWs requestContext, Partenaire partenaire, Integer[] ids){
        List<PanierProduit> panierProduitList;
        if(ids.length >0)
			panierProduitList = Arrays.stream(ids).map(id -> panierProduitRepository.findOneById(id)).collect(Collectors.toList());
        else
            panierProduitList = panierProduitRepository.findByPanier_Partenaire(partenaire);
        return panierProduitList;
    }

    private Map<String, Double> getTotalProduitAndFrais(RequestContextWs requestContext, List<PanierProduit> panierProduitList, String deviseTransaction, String deviseDefaut) throws OwomiException {
        Map<String, Double> resultat = new HashMap<>();
        double totalPrixProduit = 0.0;
        double totalFrais = 0.0;
        double totalCv = 0.0;
        if(!CollectionUtils.checkIfCollectionIsNullOrEmpty(panierProduitList)){
            for (PanierProduit panierProduit:panierProduitList){
                ConfigProduit configProduit = configProduitRepository.findOneByProduitpays(panierProduit.getProduitPays());
                double prixProduit = configProduit.getPrix() * panierProduit.getQuantite();
                DetailsFraisCvResponseWs response = dataServiceClient.getDetailsFraisAndCv(requestContext, new FraisCvWs(panierProduit.getProduitPays().getCode(), panierProduit.getQuantite()));
                totalFrais += response.getFrais().stream().map(ResumeFraisWs::getMontant).reduce(0.0, (f, l) -> f+l);
                double totalFraisInlus = response.getFrais().stream().filter(ResumeFraisWs::isInclus).map(ResumeFraisWs::getMontant).reduce(0.0, (f, l) -> f+l);
                totalPrixProduit += (prixProduit - totalFraisInlus);
                totalCv += response.getCv();
            }
        }
        MontantDetailsWs  totalPrixProduitWs = dataServiceClient.achatDevise(new AchatDeviseWs(totalPrixProduit ,deviseTransaction,deviseDefaut));
        MontantDetailsWs  totalFraisWs = dataServiceClient.achatDevise(new AchatDeviseWs(totalFrais ,deviseTransaction,deviseDefaut));
        resultat.put("totalPrixProduit", totalPrixProduitWs.getMontantDefaut());
        resultat.put("totalFrais", totalFraisWs.getMontantDefaut());
        resultat.put("totalCv", totalCv);
        return resultat;
    }

    public String getResumeAchat(RequestContextWs requestContext,List<PanierProduit> panierProduitList,String deviseTransaction, String deviseDefaut,String deviseLibelle,Integer idCommande) throws OwomiException {
        String resume = resumeProcess(requestContext,panierProduitList,deviseTransaction,deviseDefaut,deviseLibelle,idCommande);
        return resume;
    }

    public String getResumeAchat(RequestContextWs requestContext,PanierProduit panierProduit,String deviseTransaction,String deviseDefaut,String deviseLibelle,Integer idCommande) throws OwomiException {
        List<PanierProduit> panierProduitList = new ArrayList<>();
        panierProduitList.add(panierProduit);
        String resume = resumeProcess(requestContext,panierProduitList,deviseTransaction,deviseDefaut,deviseLibelle,idCommande);
        return resume;
    }

    @Transactional
    public PrestashopResponseWs getCvTiedToIdPrestashop(RequestContextWs requestContext,Integer idPrestashop, Double quantite) throws DataNotFoundException, OwomiException {
        String requestGson = new Gson().toJson(requestContext);
        ProduitPays produitPays = produitPaysRepository.findOneByIdPrestashop(idPrestashop);
        EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays,"idPrestashop", idPrestashop.toString());
        FraisCvResponseWs response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), quantite.intValue()));
        return new PrestashopResponseWs(produitPays.getId_prestashop(), produitPays.getCode(), response.getCv());
    }

    private CommonResponseWs checkStatePayment(RequestContextWs requestContext,double solde, double montant, List<PanierProduit> panierProduitList, Corporate corporate,String deviseTransaction,String deviseDefaut,String deviseLibelle,Integer idCommande) throws OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        if(solde < montant){
            updateStatePaymentByStatusAndCorporate(requestContext,panierProduitList, PannierProduitStatusEnums.DESIR_PAYER_ECHEC_CORPORATE, corporate);
            throw new OwomiException(i18n.internationalize("check.state.payment.ex.message"), ErrorCodeEnums.SOLDE_INSUFFISANT_ERROR, messageSoldeInsuffisant(requestContext, panierProduitList, montant, corporate, solde,deviseLibelle));
        }else {
            String commandReference = StringUtils.generateOTP(20);
            updateStatePaymentByCommandReference(requestContext,panierProduitList, commandReference);
            updateStatePaymentByStatusAndCorporate(requestContext,panierProduitList, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE, corporate);
            commonResponseWs.setReference(commandReference);
            commonResponseWs.setStatus(WsOperationResponseStatus.success);
            commonResponseWs.setInfo(getResumeAchat(requestContext,panierProduitList,deviseTransaction,deviseDefaut,deviseLibelle,idCommande));
            commonResponseWs.setMessage(i18n.internationalize("check.state.payment.message"));
        }
        return commonResponseWs;
    }

    private CommonResponseWs checkStatePaymentDiffered(RequestContextWs requestContext, Corporate corporate, List<PanierProduit> panierProduitList,String deviseTransaction,String deviseDefaut,String deviseLibelle,Integer idCommande) throws OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        String commandReference = StringUtils.generateOTP(20);
        updateStatePaymentByCommandReference(requestContext,panierProduitList, commandReference);
        updateStatePaymentByStatusAndCorporate(requestContext,panierProduitList, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE, corporate);
        commonResponseWs.setReference(commandReference);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setInfo(getResumeAchat(requestContext,panierProduitList,deviseTransaction,deviseDefaut,deviseLibelle,idCommande));
        commonResponseWs.setMessage(i18n.internationalize("check.state.payment.message"));
        return commonResponseWs;
    }

    private void updateStatePaymentByStatusAndCorporate(RequestContextWs requestContext, List<PanierProduit> panierProduitList, PannierProduitStatusEnums pannierProduitStatusEnums, Corporate corporate){
        for (PanierProduit panierProduit:panierProduitList){
            panierProduit.setStatus(pannierProduitStatusEnums.name());
            panierProduit.setCorporate(corporate);
            panierProduitRepository.save(panierProduit);
        }
    }

    private void updateStatePaymentByStatus(RequestContextWs requestContext, List<PanierProduit> panierProduitList, PannierProduitStatusEnums pannierProduitStatusEnums){
        for (PanierProduit panierProduit:panierProduitList){
            panierProduit.setStatus(pannierProduitStatusEnums.name());
            panierProduitRepository.save(panierProduit);
        }
    }

    private void updateStatePaymentByCommandReference(RequestContextWs requestContext, List<PanierProduit> panierProduitList, String commandReference){
        for (PanierProduit panierProduit:panierProduitList){
            panierProduit.setReferenceCommande(commandReference);
            panierProduitRepository.save(panierProduit);
        }
    }

    private void updateStatePaymentByValidationCourant(RequestContextWs requestContext, List<PanierProduit> panierProduitList, int total){
        for (PanierProduit panierProduit:panierProduitList){
            panierProduit.setValidationCourant(panierProduit.getValidationCourant()+total);
            panierProduitRepository.save(panierProduit);
        }
    }

    private void updateStatePaymentByPartenaires(RequestContextWs requestContext, List<PanierProduit> panierProduitList, String id){
        for (PanierProduit panierProduit:panierProduitList){
            if (panierProduit.getPartenaires() != null)
                panierProduit.setPartenaires(panierProduit.getPartenaires()+","+id);
            else
                panierProduit.setPartenaires(id);
            panierProduitRepository.save(panierProduit);
        }
    }

    private String resumeProcess(RequestContextWs requestContext, List<PanierProduit> panierProduitList,String deviseTransaction,String deviseDefaut,String deviseLibelle,Integer idCommande) throws OwomiException {
        Map<String, Double> totaux = getTotalProduitAndFrais(requestContext, panierProduitList,deviseTransaction,deviseDefaut);
        String resume = "";
        if(idCommande!=0){
            resume = resumeProcessForAchatBoutique(requestContext,idCommande,deviseLibelle,totaux);
        }else{
             resume = i18n.internationalize("resume.process.message11", i18n.internationalize(panierProduitList.size() > 1 ? "resume.process.produit.plural" : "resume.process.produit.singular"));
            for (PanierProduit panierProduit:panierProduitList){
                if (panierProduitList.indexOf(panierProduit) == (panierProduitList.size() - 1))
                    resume = resume+" "+panierProduit.getProduitPays().getProduit().getNom() +". ";
                else
                    resume = resume+" "+panierProduit.getProduitPays().getProduit().getNom()+", ";
            }
            if (panierProduitList.size() > 1)
                resume = resume+i18n.internationalize("resume.process.message12", formatter.format(totaux.get("totalPrixProduit")), deviseLibelle);
            else
                resume = resume+i18n.internationalize("resume.process.message13",formatter.format(totaux.get("totalPrixProduit")), deviseLibelle);
            resume = resume+ i18n.internationalize("resume.process.message14", formatter.format(totaux.get("totalFrais")), deviseLibelle, formatter.format(totaux.get("totalCv")), formatter.format(totaux.get("totalPrixProduit")+totaux.get("totalFrais")), deviseLibelle);

        }

        return resume;
    }

    private String resumeProcessForAchatBoutique(RequestContextWs requestContextWs, Integer idCommande, String deviseLibelle, Map<String, Double> totaux) throws OwomiException {
        CommandeShopizerWs commandeShopizerWs = shopizerServiceClient.getOrder(idCommande);
        String resume = i18n.internationalize("resume.process.message01");
        for (ProduitShopizerWs produitShopizerWs :commandeShopizerWs.getProducts()){
            resume = resume+","+produitShopizerWs.getProductName();
        }
            resume = resume+". "+ i18n.internationalize("resume.process.message02", formatter.format(totaux.get("totalPrixProduit")), deviseLibelle);
        resume = resume+ i18n.internationalize("resume.process.message03", formatter.format(totaux.get("totalFrais")), deviseLibelle, formatter.format(totaux.get("totalCv")), formatter.format(totaux.get("totalPrixProduit")+totaux.get("totalFrais")), deviseLibelle);

        return resume;
    }

    private String messageSoldeInsuffisant(RequestContextWs requestContext, List<PanierProduit> panierProduitList, double montant, Corporate corporate, double solde,String deviseLibelle){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(i18n.internationalize("solde.insuffisant.message1"));
        if (panierProduitList.size()>1) {
            stringBuilder.append(i18n.internationalize("solde.insuffisant.produit.plural")).append(panierProduitList.stream().map(panierProduit -> panierProduit.getProduitPays().getProduit().getNom()).collect(Collectors.joining(", ")));
        }
        else
            stringBuilder.append(i18n.internationalize("solde.insuffisant.produit.singular")).append(panierProduitList.get(0).getProduitPays().getProduit().getNom());
        stringBuilder.append(i18n.internationalize("solde.insuffisant.message2", formatter.format(montant), deviseLibelle, corporate.getMatricule(), formatter.format(solde), deviseLibelle));
        return stringBuilder.toString();
    }

    public void restartSolde (RequestContextWs requestContextWs) throws OwomiException {
        List<Partenaire> partenaireList = partenaireRepository.findAll();
        for (Partenaire partenaire : partenaireList){
            Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
            if(corporate!=null){
                AccountSolde accountSolde = accountSoldeRepository.findOneByCompte(corporate.getNumeroCompte());
                if(accountSolde!=null){
                    Double solde= accountSolde.getSolde();
                    Double newSolde = dataServiceClient.achatDevise(new AchatDeviseWs(solde,DevisesEnums.FRANC_CFA.getCodeDevise(),partenaire.getPays().getDevise().getCode())).getMontantDefaut();
					dataServiceClient.updateSolde(corporate.getNumeroCompte().getId(), newSolde, 0);
                    if(newSolde > solde){
                        ResumeGrandLivreWs resumeGrandLivreWs = getResume(corporate,"1",newSolde,solde);
                        //accountServiceClient.saveGrandLivre(resumeGrandLivreWs);
                    }
                    if(newSolde < solde){
                        ResumeGrandLivreWs resumeGrandLivreWs = getResume(corporate,"0",solde,newSolde);
                        //accountServiceClient.saveGrandLivre(resumeGrandLivreWs);
                    }
                }
            }
            logger.debug("END OF THIS PARTENAIRE " + partenaire.getNom());
        }
    }

    private ResumeGrandLivreWs getResume(Corporate corporate, String sens, Double newSolde, Double solde){
        ResumeGrandLivreWs resumeGrandLivreWs = new ResumeGrandLivreWs();
        resumeGrandLivreWs.setEcriture(IntegerUtils.generateEcriture());
        resumeGrandLivreWs.setIdOperation(9999);
        resumeGrandLivreWs.setIdOperationString(9999);
        resumeGrandLivreWs.setLibelle(i18n.internationalize("resume.libelle"));
        resumeGrandLivreWs.setNumeroCompte(corporate.getNumeroCompte().getId());
        resumeGrandLivreWs.setMontant(newSolde-solde);
        resumeGrandLivreWs.setSens(sens);
        return resumeGrandLivreWs;
	}

	public CommonResponseWs getKey(HttpServletRequest request) throws OwomiException {
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		PublicKeyWs publicKeyWs = dataServiceClient.createKey(request.getHeader("access-token"));
		commonResponseWs.setStatus(WsOperationResponseStatus.success);
		commonResponseWs.setData(publicKeyWs);
		return commonResponseWs;
	}

	private CreateCommandeForReceiverWs map(RequestContextWs requestContextWs, Partenaire partenaire, String reference, String deviseDefaut, String deviseTransaction) {
		List<PanierProduit> panierProduits = panierProduitRepository.findByReferenceCommandeAndStatus(reference, PannierProduitStatusEnums.VERIFICATION_PIN_SUCCES.name());
		logger.debug("step logging MAP 1 = " + panierProduits.size());
		List<PanierProduitWs> panierProduitWsList = map(panierProduits);
		logger.debug("step logging MAP 2");
		PartenaireWs partenaireWs = mapper.map(partenaire, PartenaireWs.class);
		logger.debug("step logging MAP 3");
		return new CreateCommandeForReceiverWs(requestContextWs, partenaireWs, panierProduitWsList, reference, deviseDefaut, deviseTransaction);
	}

	private List<PanierProduitWs> map(List<PanierProduit> panierProduitList) {
		List<PanierProduitWs> list = panierProduitList.stream()
				.map(v -> {
					PanierProduitWs panier = mapper.map(v, PanierProduitWs.class);
					panier.setIdpanier(v.getPanier().getId());
					panier.setIdproduit(v.getProduitPays().getId());
					if (v.getProduitPays() != null) {
						ProduitPaysWs produitPays = mapper.map(v.getProduitPays(), ProduitPaysWs.class);
						panier.setProduct(produitPays);
					}
					return panier;
				}).collect(Collectors.toList());
		logger.debug("step logging END MAP");
		return list;
	}

    private void updateStatePaymentByStatus(List<PanierProduit> panierProduitList, PannierProduitStatusEnums pannierProduitStatusEnums){
        panierProduitList.forEach(panierProduit -> {
            panierProduit.setStatus(pannierProduitStatusEnums.name());
            panierProduitRepository.save(panierProduit);
        });
    }


    @Transactional
    public CommonResponseWs stepTwoToBuyProcessOperation(OperationInfoWs operationInfo, Integer idPartenaire) throws OwomiException, SessionExpireException, AuthentificationException, DataNotFoundException {
        Partenaire partenaire = partenaireRepository.findOneById(idPartenaire);
        List<PanierProduit> panierProduits = panierProduitRepository.findByReferenceCommande(operationInfo.getCommandReference());
        if(checkSessionStatus(panierProduits.get(0).getSession())){
            return stepTwoToBuyOwoPay(new RequestContextWs(),partenaire,operationInfo.getPin(),operationInfo.getCommandReference(),operationInfo.getDevisTransaction());
        }else throw new SessionExpireException();
    }

    @Transactional
    public CommonResponseWs stepTwoToBuyOwoPay(RequestContextWs requestContext, Partenaire partenaire, String pin, String reference,String deviseTransaction) throws OwomiException {
		List<PanierProduit> panierProduits = panierProduitRepository.findByReferenceCommandeAndStatus(reference, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE.name());
		updateStatePaymentByStatus(requestContext, panierProduits, PannierProduitStatusEnums.VERIFICATION_PIN_SUCCES);
		deviseTransaction = panierProduits.get(0).getProduitPays().getPays().getDevise().getCode();
		CreateCommandeForReceiverWs createCommandeForReceiverWs = map(requestContext, partenaire, reference, partenaire.getPays().getDevise().getCode(), deviseTransaction);
		this.createCommandeFuture(createCommandeForReceiverWs);
		//commandeActiveMqPublisher.send(createCommandeForReceiverWs);
		return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18nCommons.internationalize("OperationConstant.MSG_SUCCESS_VALID_PIN"), null, null, null);
	}

}
