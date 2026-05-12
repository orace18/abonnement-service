package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.constants.FlocashConstant;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.ws.DepotWss;
import com.clubchretien.abonnement.ws.MoyenPaiementWs;
import com.google.gson.Gson;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.NatureCorporateEnums;
import com.owomi.commons.enums.RetraitEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.transferts.IdTransfertGenerator;
import com.owomi.commons.utils.CollectionUtils;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.billing.response.PageResponse;
import com.owomi.commons.ws.flocash.*;
import com.owomi.commons.ws.moov.CreateRechargeMoovWs;
import com.owomi.commons.ws.stripe.CreateRechargeStripeWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.externalService.FloCashServiceClient;
import com.owomi.rest.client.externalService.MoovBeninFloozClient;
import com.owomi.rest.client.externalService.StripeServiceClient;
import com.owomi.rest.client.externalService.TouchServiceClient;
import com.owomi.rest.client.notif.NotificationUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Component
public class TransactionFinanciereService implements MobileConstant {
    private static Logger logger = LogManager.getLogger(TransactionFinanciereService.class);

    private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
    @Autowired
    BanqueRepository banqueRepository;
    @Autowired
    BanquesRepository banquesRepository;
    @Autowired
    CorporateService corporateService;
    @Autowired
    GrandLivreRepository grandLivreRepository;
    @Autowired
    VirementRepository virementRepository;
    @Autowired
    DepotRepository depotRepository;
    @Autowired
    MobileMoneyRepository mobileMoneyRepository;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    MoyenReceptionRepository moyenReceptionRepository;
    @Autowired
    MoyenRecPartenaireRepository moyenRecPartenaireRepository;
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    FileService fileService;
    @Autowired
    Mapper mapper;
    @Autowired
    TransfertsRepository transfertsRepository;
    @Autowired
    PanierService panierService;
    @Autowired
    OperationService operationService;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    RetraitMobileRepository retraitMobileRepository;
    @Autowired
    AccountSoldeRepository accountSoldeRepository;
    @Autowired
    AccountServiceClient accountServiceClient;
    @Autowired
    MoyenDePaiementRepository moyenDePaiementRepository;
    @Autowired
    RechargeRepository rechargeRepository;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    TouchServiceClient touchServiceClient;
    @Autowired
    FloCashServiceClient floCashServiceClient;
    @Autowired
    MoovBeninFloozClient moovBeninFloozClient;
    @Autowired
    ChampRepository champRepository;
    @Autowired
    PaysRepository paysRepository;
    @Autowired
    PartenaireCorporateRepository partenaireCorporateRepository;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    ValidationService validationService;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;


    public CommonResponseWs listReservation(String matricule, Boolean leve, Integer page, Integer size) throws OwomiException, DataNotFoundException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, "matricule", matricule);
        return accountServiceClient.listReservation(
                corporate.getNumeroCompte().getId(), leve, page, size
        );
    }


    public double soldeDisponible(Corporate corporate) {
        CorporateSoldeWs solde = this.accountServiceClient.getAccountSolde(corporate.getNumeroCompte().getId());
        if (solde != null) return solde.getSoldeDisponible();
        return  0.0;
    }

    @Transactional
    public CommonResponseWs saveVirement(RequestContextWs requestContext, VirementWs virementWs, Partenaire partenaire) throws DataNotFoundException, ParseException, OwomiException {
        Virement virement = map(requestContext, virementWs);
        virement.setBanqueEmetteur(virementWs.getBanqueEmetteur());
        virement = virementRepository.save(virement);
        activityLogService.makeActivityLog(requestContext, virement, partenaire, VELOCITY_VIREMENT);
        return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18nCommons.internationalize("MobileConstant.MSG_VIREMENT"), virement.getId(), null, null);
    }

    @Transactional
    public CommonResponseWs saveDepot(RequestContextWs requestContext, DepotWss depotWss, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException {
        boolean result = partenaireCorporateRepository.findAllByPk_Partenaire(partenaire).stream().anyMatch(partenaireCorporate -> partenaireCorporate.getCorporate().getNatureCorporate().getLibelle().equals(NatureCorporateEnums.AGENT.getNature()));
        if (!result)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "NO ALLOWED, NO AGENT CORPORATE", 401, null);
        Depot depot = map(requestContext, depotWss, partenaire);
        depot = depotRepository.save(depot);
        activityLogService.makeActivityLog(requestContext, depot, partenaire, VELOCITY_DEPOT);
        sendInValidation(requestContext, depot);
        return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18nCommons.internationalize("MobileConstant.MSG_DEPOT"), depot.getId(), null, null);
    }

    @Transactional
    public CommonResponseWs saveDestruction(RequestContextWs requestContext, DepotWss depotWss, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException {
        boolean result = partenaireCorporateRepository.findAllByPk_Partenaire(partenaire).stream().anyMatch(partenaireCorporate -> partenaireCorporate.getCorporate().getNatureCorporate().getLibelle().equals(NatureCorporateEnums.AGENT.getNature()));
        if (!result)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "NO ALLOWED, NO AGENT CORPORATE", 401, null);
        Depot depot = map(requestContext, depotWss, partenaire);
        depot = depotRepository.save(depot);
        activityLogService.makeActivityLog(requestContext, depot, partenaire, VELOCITY_DEPOT);
        sendInValidation(requestContext, depot);
        return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18nCommons.internationalize("MobileConstant.MSG_DEPOT"), depot.getId(), null, null);
    }

    private void sendInValidation(RequestContextWs requestContext, Depot depot) {
        if(depot.getStatut()) return;
        Validation validation = new Validation();
        validation.setReference(Depot.class.getSimpleName());
        validation.setReferenceId(depot.getId());
        validation.setTableref(Depot.class.getSimpleName());
        validation.setIdref(depot.getId());
        validation.setNature(i18n.internationalize("send.in.validation.nature", Corporate.class.getSimpleName()));
        validation.setTicketvalidation("genere");
        validation.setDateEnvoi(new Timestamp(System.currentTimeMillis()));
        validation.setMotifreceveur(depot.getEmetteur());
        validation.setBoRole("Manager");
        validation = validationRepository.save(validation);
        this.validationService.inSendValidation(depot.getPartenaire(), validation);
    }


    @Transactional
    public MobileMoneyResponseWs mobileMoney(RequestContextWs requestContext, MobileMoneyWs mobileMoneyWs, Partenaire partenaire) throws DataNotFoundException, OwomiException {
        MobileMoney mobileMoney = !mobileMoneyRepository.findByReference(mobileMoneyWs.getReference()).isEmpty() ? mobileMoneyRepository.findByReference(mobileMoneyWs.getReference()).get(0) : null;
        if (mobileMoney != null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.MOBILE_MONEY_REFERENCE_ERROR_MSG"), ErrorCodeEnums.MOBILE_MONEY_REFERENCE_ERROR, i18nCommons.internationalize("MobileConstant.MOBILE_MONEY_REFERENCE_ERROR_DETAIL_MSG_START") + mobileMoneyWs.getReference() + i18nCommons.internationalize("MobileConstant.MOBILE_MONEY_REFERENCE_ERROR_DETAIL_MSG_END"));
        Corporate corporate = corporateRepository.findOneById(mobileMoneyWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, mobileMoneyWs.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        mobileMoney = new MobileMoney(mobileMoneyWs.getReference(), mobileMoneyWs.getMontant(), corporate, mobileMoneyWs.getType().name());
        mobileMoney = mobileMoneyRepository.save(mobileMoney);
        switch (mobileMoneyWs.getType()) {
            case MTN:
                activityLogService.makeActivityLog(requestContext, mobileMoney, partenaire, VELOCITY_MTN_MOBILE_MONEY);
                return new MobileMoneyResponseWs(mobileMoney.getId(), mobileMoney.getReference(), mobileMoney.getMontant(), i18nCommons.internationalize("MSG_MTN_MOBILE_MONEY"), WsOperationResponseStatus.success);
            case MOOV:
                activityLogService.makeActivityLog(requestContext, mobileMoney, partenaire, VELOCITY_MOOV_MOBILE_MONEY);
                return new MobileMoneyResponseWs(mobileMoney.getId(), mobileMoney.getReference(), mobileMoney.getMontant(), i18nCommons.internationalize("MobileConstant.MSG_MOOV_MOBILE_MONEY"), WsOperationResponseStatus.success);
            default:
                throw new OwomiException(i18n.internationalize("mobile.money.ex.message"), ErrorCodeEnums.CV_INITIAL_NOT_FOUND, i18n.internationalize("mobile.money.ex.message"));
        }
    }

    @Transactional
    public List<BanqueWs> list(RequestContextWs requestContext) {
        List<Banque> banqueList = banqueRepository.findAll();
        List<BanqueWs> banqueWsList = new ArrayList<>();
        if (!CollectionUtils.checkIfCollectionIsNullOrEmpty(banqueList))
            banqueWsList = banqueList.stream().map(banque -> new BanqueWs(banque.getId(), banque.getNom(), banque.getSigle(), banque.getNumerodecompte().getNumero(), null, null, null, null, null, null,null,null)).collect(Collectors.toList());
        return banqueWsList;
    }

    @Transactional
    public List<MoyenReception> listMoyenReception(RequestContextWs requestContext) {
        return moyenReceptionRepository.findAll();
    }

    @Transactional
    public List<MoyenRecPartenaireResponseWs> listMoyenRecPartenaire(RequestContextWs requestContext, Partenaire partenaire, Integer idCorporate) throws DataNotFoundException {
        List<MoyenRecPartenaire> moyenRecPartenaireList;
        List<MoyenRecPartenaireResponseWs> moyenRecPartenaireResponseWsList;
        if (idCorporate != null) {
            Corporate corporate = corporateRepository.findOneById(idCorporate);
            EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
            moyenRecPartenaireList = moyenRecPartenaireRepository.findByPartenaireAndCorporateAndStatutTrue(partenaire, corporate);
        } else
            moyenRecPartenaireList = moyenRecPartenaireRepository.findByPartenaireAndStatutTrue(partenaire);
        moyenRecPartenaireResponseWsList = map(requestContext, moyenRecPartenaireList);
        return moyenRecPartenaireResponseWsList;
    }

    @Transactional
    public CommonResponseWs retirer(RequestContextWs requestContext, RetraitWs retraitWs, Partenaire partenaire) throws DataNotFoundException, OwomiException {
        MoyenRecPartenaire moyenRecPartenaire = moyenRecPartenaireRepository.findOneById(retraitWs.getMoyen());
        EntityUtils.checkIfEntityIsNull(MoyenRecPartenaire.class, moyenRecPartenaire, retraitWs.getMoyen());
        Corporate corporate = corporateRepository.findOneById(retraitWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, retraitWs.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        RetraitMobile retraitMobile = createRetrait(requestContext, retraitWs, moyenRecPartenaire, corporate);
        PanierWs panierWs = createPanierWs(requestContext, retraitMobile, retraitWs, moyenRecPartenaire);
        PanierProduit panierProduit = panierService.panierProcess(requestContext, partenaire, panierWs);
        panierProduit.setMvntTemporaire(Boolean.TRUE);
        panierProduitRepository.save(panierProduit);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        CommonResponseWs response = operationService.stepOneToBuy(requestContext, partenaire, ids, corporateRepository.findOneByMatricule(partenaire.getTelephone()).getId(), partenaire.getPays().getDevise().getCode(), 0);
        if (response.getStatus() == WsOperationResponseStatus.error) {
            panierProduitRepository.delete(panierProduit);
            retraitMobileRepository.delete(retraitMobile);
        }
        return response;
    }

    @Transactional
    public ConfigMoyenResponseWs configMoyenBancaire(RequestContextWs requestContext, ConfigMoyenBancaireWs configMoyenBancaireWs, Partenaire partenaire) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneById(configMoyenBancaireWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, configMoyenBancaireWs.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        MoyenReception moyenReception = moyenReceptionRepository.findOneByLibelle(MOYEN_BANCAIRE_LIBELLE);
        MoyenRecPartenaire moyenRecPartenaire = moyenRecPartenaireRepository.findOneByNumCompte(configMoyenBancaireWs.getNumero_compte());
        if (moyenRecPartenaire != null)
            return new ConfigMoyenResponseWs(i18nCommons.internationalize("MobileConstant.CONFIG_MOYEN_RECEPTION_BANCAIRE_ERROR_MSG"), WsOperationResponseStatus.error);
        moyenRecPartenaire = map(requestContext, configMoyenBancaireWs, partenaire, moyenReception, corporate);
        moyenRecPartenaire = moyenRecPartenaireRepository.save(moyenRecPartenaire);
        sendInValidation(requestContext, moyenRecPartenaire);
        activityLogService.createActivityLogMobileConfigMoyen(requestContext, partenaire);
        return new ConfigMoyenResponseWs(i18nCommons.internationalize("MobileConstant.CONFIG_MOYEN_RECEPTION_BANCAIRE_SUCCESS_MSG"), WsOperationResponseStatus.success);
    }

    @Transactional
    public ConfigMoyenResponseWs configMoyenMobile(RequestContextWs requestContext, ConfigMoyenMobileWs configMoyenMobileWs, Partenaire partenaire) throws DataNotFoundException, OwomiException {
        MoyenReception moyenReception = moyenReceptionRepository.findOneById(configMoyenMobileWs.getType());
        EntityUtils.checkIfEntityIsNull(MoyenReception.class, moyenReception, configMoyenMobileWs.getType());
        Corporate corporate = corporateRepository.findOneById(configMoyenMobileWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, configMoyenMobileWs.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        MoyenRecPartenaire moyenRecPartenaire = moyenRecPartenaireRepository.findOneByTelephone(configMoyenMobileWs.getTelephone());
        if (moyenRecPartenaire != null)
            return new ConfigMoyenResponseWs(i18nCommons.internationalize("MobileConstant.CONFIG_MOYEN_RECEPTION_MOBILE_ERROR_MSG"), WsOperationResponseStatus.error);
        moyenRecPartenaire = map(requestContext, configMoyenMobileWs, partenaire, moyenReception, corporate);
        moyenRecPartenaire = moyenRecPartenaireRepository.save(moyenRecPartenaire);
        sendInValidation(requestContext, moyenRecPartenaire);
        activityLogService.createActivityLogMobileConfigMoyenMobile(requestContext, moyenReception.getLibelle(), partenaire);
        return new ConfigMoyenResponseWs(i18nCommons.internationalize("MobileConstant.CONFIG_MOYEN_RECEPTION_MOBILE_SUCCESS_MSG"), WsOperationResponseStatus.success);
    }

    @Transactional
    public List<GrandLivreWs> getHistoriqueCompteByCorporate(RequestContextWs requestContext, int idCorporate, int page, int size) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, corporate.getId());
        List<GrandLivreWs> grandLivreListWs = accountServiceClient.getHistoriqueCompte(corporate.getNumeroCompte().getId(), page, size);
        if (CollectionUtils.checkIfCollectionIsNullOrEmpty(grandLivreListWs))
            return new ArrayList<>();
        return grandLivreListWs;
    }

    @Transactional
    public CommonResponseWs getHistoriqueCompteByCorporate(Partenaire partenaire, Integer idCorporate, Integer page, Integer size) {
        Corporate corporate = this.corporateRepository.findOneById(idCorporate);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        logger.info("VALUES EXPECTED ================================= ");
        PageResponse<GrandLivreWs> pages = accountServiceClient.getHistoriqueCompteV2(corporate.getNumeroCompte().getId(), page, size);
        logger.info("VALUES EXPECTED ================================= " + pages.toString());
        String devise = partenaire.getPays().getDevise().getCode();
        List<GrandLivreWs> content = pages.getContent().stream().peek(p -> p.setCodeDevise(devise)).collect(Collectors.toList());
        Pageable pageable = PageRequest.of(page, size);
        Page<GrandLivreWs> result = new PageImpl<>(content, pageable, pages.getTotalElements());
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    @Transactional
    public List<GrandLivreWs> getHistoriqueCompteByPartenaire(RequestContextWs requestContext, Partenaire partenaire, int page, int size) throws DataNotFoundException, OwomiException {
        List<CorporateWs> corporateWsList = corporateService.getCorporate(requestContext, partenaire, false);
        List<GrandLivreWs> grandLivreListWs = new ArrayList<>();
        for (CorporateWs corporateWs : corporateWsList) {
            List<GrandLivreWs> grandLivreWsList = getHistoriqueCompteByCorporate(requestContext, corporateWs.getId(), page, size);
            grandLivreListWs.addAll(grandLivreWsList);
        }
        return grandLivreListWs;
    }

    @Transactional
    public double extraitCompte(RequestContextWs requestContext, String matricule) throws DataNotFoundException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, CORPORATE_MATRICULE, matricule);
        CorporateSoldeWs solde = this.accountServiceClient.getAccountSolde(corporate.getNumeroCompte().getId());
        if (solde != null) return solde.getSolde();
        AccountSolde accountSolde = accountSoldeRepository.findOneByCompte(corporate.getNumeroCompte());
        return accountSolde == null ? 0.0 : accountSolde.getSolde();
    }

    @Transactional
    public CommonResponseWs doTransfert(RequestContextWs requestContext, Partenaire partenaire, TransfertWs transfertWs, String deviseTransaction) throws DataNotFoundException, OwomiException {
        if (transfertWs.getQuantite() < 0)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.NEGATIVE_AMOUNT_ERROR_MSG"), ErrorCodeEnums.NEGATIVE_AMOUNT_ERROR, i18nCommons.internationalize("MobileConstant.NEGATIVE_AMOUNT_ERROR_DETAIL_MSG_START") + transfertWs.getQuantite() + i18nCommons.internationalize("MobileConstant.NEGATIVE_AMOUNT_ERROR_DETAIL_MSG_END"));
        Corporate expediteur = corporateRepository.findOneById(transfertWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, expediteur, transfertWs.getCorporate());
        if (!expediteur.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + expediteur.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        Corporate destinataire = corporateRepository.findOneByMatricule(transfertWs.getDestinataire());
        if (destinataire == null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.NUMBER_NO_SUBSCRIBE_ERROR_MSG"), ErrorCodeEnums.NUMBER_NO_SUBSCRIBE, i18nCommons.internationalize("MobileConstant.NUMBER_NO_SUBSCRIBE_ERROR_DETAIL_MSG_START") + transfertWs.getDestinataire() + i18nCommons.internationalize("MobileConstant.NUMBER_NO_SUBSCRIBE_ERROR_DETAIL_MSG_END"));
        Transferts transferts = createTransferts(requestContext, expediteur, destinataire, transfertWs);
        PanierWs panierWs = createPanierWs(requestContext, transfertWs, transferts);
        PanierProduit panierProduit = panierService.panierProcess(requestContext, partenaire, panierWs);
        panierProduit.setCorporate(expediteur);
        panierProduitRepository.save(panierProduit);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        return operationService.stepOneToBuy(requestContext, partenaire, ids, corporateRepository.findOneByMatricule(partenaire.getTelephone()).getId(), deviseTransaction, 0);
    }

    @Transactional
    public List<HistoriqueCompteWs> releveCompte(RequestContextWs requestContext, Partenaire partenaire, Integer page, Integer size) {
        List<Operation> operationList = operationRepository.findByCommande_PartenaireOrderByIdDesc(partenaire, PageRequest.of(page, size));
        return operationList.stream().filter(operation -> !(operation.getMontant() != 0.0)).map(operation -> {

            HistoriqueCompteWs historiqueCompteWs = new HistoriqueCompteWs();
            historiqueCompteWs.setId(operation.getId());
            historiqueCompteWs.setLabel(operation.getLibelle());
            historiqueCompteWs.setDate(format.format(operation.getDateop()));
            historiqueCompteWs.setReference(operation.getNumero());
            historiqueCompteWs.setMontant(operation.getMontant());
            List<GrandLivre> grandLivres = grandLivreRepository.findByEcriture(Integer.decode(operation.getNumero()));
            List<GrandLivreWs> grandLivreWs = grandLivres.stream()
                    .map(grandLivre -> new GrandLivreWs(
                            grandLivre.getId(), grandLivre.getSens(),
                            format.format(grandLivre.getDateEcriture()),
                            grandLivre.getMontant(), grandLivre.getLibelleecriture(),
                            grandLivre.getEcriture(), "", "",
                            null, null, null, null, null
                    ))
                    .collect(Collectors.toList());
            historiqueCompteWs.setDetails(grandLivreWs);
            return historiqueCompteWs;
        }).collect(Collectors.toList());
    }

    private Transferts createTransferts(RequestContextWs requestContext, Corporate expediteur, Corporate destinataire, TransfertWs transfertWs) {
        Transferts transferts = new Transferts();
        transferts.setId(IdTransfertGenerator.generateIdTransfert());
        transferts.setMontant(transfertWs.getQuantite().doubleValue());
        transferts.setDateExpedition(new Date());
        transferts.setValider(Boolean.FALSE);
        transferts.setExpediteur(expediteur);
        transferts.setDestinataire(destinataire);
        transferts = transfertsRepository.save(transferts);
        return transferts;
    }

    private PanierWs createPanierWs(RequestContextWs requestContext, TransfertWs transfertWs, Transferts transferts) {
        PanierWs panierWs = new PanierWs();
        panierWs.setCode(transfertWs.getCodeProduit());
        panierWs.setQuantite(transfertWs.getQuantite());
        panierWs.setReference(transferts.getId());
        return panierWs;
    }

    private RetraitMobile createRetrait(RequestContextWs requestContext, RetraitWs retraitWs, MoyenRecPartenaire moyenRecPartenaire, Corporate corporate) {
        RetraitMobile retraitMobile = new RetraitMobile();
        retraitMobile.setMontant(retraitWs.getMontant());
        retraitMobile.setMoyen(moyenRecPartenaire);
        retraitMobile.setPartenaire(moyenRecPartenaire.getPartenaire());
        retraitMobile.setCorporate(corporate);
        retraitMobile.setStatut(RetraitEnums.initial.name());
        retraitMobile.setDate(new Timestamp(new Date().getTime()));
        return retraitMobileRepository.save(retraitMobile);
    }

    private PanierWs createPanierWs(RequestContextWs requestContext, RetraitMobile retraitMobile, RetraitWs retraitWs, MoyenRecPartenaire moyenRecPartenaire) {
        PanierWs panierWs = new PanierWs();
        panierWs.setReference(retraitMobile.getId().toString());
        panierWs.setQuantite(retraitWs.getMontant().intValue());
        panierWs.setCode(getCodeProduit(requestContext, moyenRecPartenaire.getMoyenreception().getLibelle()));
        return panierWs;
    }

    private double getSoldeCompte(RequestContextWs requestContext, List<GrandLivre> grandLivreList) {
        Double credit = 0.0;
        Double debit = 0.0;
        for (GrandLivre grandLivre : grandLivreList) {
            String sens = grandLivre.getSens();
            switch (sens) {
                case "0":
                    debit += grandLivre.getMontant();
                    break;
                case "1":
                    credit += grandLivre.getMontant();
                    break;
                default:
            }
        }
        return (credit - debit);
    }

    private MoyenRecPartenaire map(RequestContextWs requestContext, ConfigMoyenBancaireWs configMoyenBancaireWs, Partenaire partenaire, MoyenReception moyenReception, Corporate corporate) {
        MoyenRecPartenaire moyenRecPartenaire = new MoyenRecPartenaire();
        moyenRecPartenaire.setBic(configMoyenBancaireWs.getBic());
        moyenRecPartenaire.setCleRib(configMoyenBancaireWs.getCle_rib());
        moyenRecPartenaire.setCodeBanque(configMoyenBancaireWs.getCode_banque());
        moyenRecPartenaire.setCodeGuichet(configMoyenBancaireWs.getCode_guichet());
        moyenRecPartenaire.setDomiciliation(configMoyenBancaireWs.getDomiciliation());
        moyenRecPartenaire.setIban(configMoyenBancaireWs.getIban());
        moyenRecPartenaire.setStatut(Boolean.FALSE);
        moyenRecPartenaire.setNumCompte(configMoyenBancaireWs.getNumero_compte());
        moyenRecPartenaire.setPartenaire(partenaire);
        moyenRecPartenaire.setMoyenreception(moyenReception);
        moyenRecPartenaire.setCorporate(corporate);
        return moyenRecPartenaire;
    }

    private MoyenRecPartenaire map(RequestContextWs requestContext, ConfigMoyenMobileWs configMoyenMobileWs, Partenaire partenaire, MoyenReception moyenReception, Corporate corporate) {
        MoyenRecPartenaire moyenRecPartenaire = new MoyenRecPartenaire();
        moyenRecPartenaire.setPartenaire(partenaire);
        moyenRecPartenaire.setMoyenreception(moyenReception);
        moyenRecPartenaire.setTelephone(configMoyenMobileWs.getTelephone());
        moyenRecPartenaire.setStatut(Boolean.FALSE);
        moyenRecPartenaire.setCorporate(corporate);
        return moyenRecPartenaire;
    }

    private Virement map(RequestContextWs requestContext, VirementWs virementWs) throws DataNotFoundException, ParseException, OwomiException {
        Virement virement = !virementRepository.findByReference(virementWs.getReference()).isEmpty() ? virementRepository.findByReference(virementWs.getReference()).get(0) : null;
        if (virement != null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.VIREMENT_REFERENCE_ERROR_MSG"), ErrorCodeEnums.TRANSFER_REFERENCE_ERROR, i18nCommons.internationalize("MobileConstant.VIREMENT_REFERENCE_ERROR_DETAIL_MSG_START") + virementWs.getReference() + i18nCommons.internationalize("MobileConstant.VIREMENT_REFERENCE_ERROR_DETAIL_MSG_END"));
        Banque banque = banqueRepository.findOneById(virementWs.getBanque());
        EntityUtils.checkIfEntityIsNull(Banque.class, banque, virementWs.getBanque());
        Corporate corporate = corporateRepository.findOneById(virementWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, virementWs.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        virement = new Virement(virementWs.getEmetteur(), format.parse(virementWs.getDate()), virementWs.getReference(), virementWs.getObjet(), virementWs.getNumeroCompte(), Boolean.FALSE, StringUtils.generateTicketValidation(), virementWs.getMontant(), banque, corporate);
        return virement;
    }

    private Depot map(RequestContextWs requestContext, DepotWss depotWss, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException {
        Banques banque = banquesRepository.findOneById(depotWss.getBanque());
        EntityUtils.checkIfEntityIsNull(Banque.class, banque, depotWss.getBanque());
        Corporate corporate = corporateRepository.findOneById(depotWss.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, depotWss.getCorporate());
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_START") + corporate.getMatricule() + i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_DETAIL_MSG_END"));
        String url = fileService.saveFile(depotWss.getFichier());
        return new Depot(depotWss.getEmetteur(), format.parse(depotWss.getDate()), depotWss.getMontant(), url, Boolean.FALSE, StringUtils.generateTicketValidation(), banque, partenaire, corporate);
    }

    private List<MoyenRecPartenaireResponseWs> map(RequestContextWs requestContext, List<MoyenRecPartenaire> moyenRecPartenaireList) {
        List<MoyenRecPartenaireResponseWs> moyenRecPartenaireResponseWsList = new ArrayList<>();
        if (!CollectionUtils.checkIfCollectionIsNullOrEmpty(moyenRecPartenaireList))
            moyenRecPartenaireResponseWsList = moyenRecPartenaireList.stream().map(moyenRecPartenaire -> new MoyenRecPartenaireResponseWs(moyenRecPartenaire.getId(), moyenRecPartenaire.getMoyenreception().getLibelle(), moyenRecPartenaire.getNumCompte() == null ? moyenRecPartenaire.getTelephone() : moyenRecPartenaire.getNumCompte(), moyenRecPartenaire.getStatut())).collect(Collectors.toList());
        return moyenRecPartenaireResponseWsList;
    }

    private String getCodeProduit(RequestContextWs requestContext, String libelle) {
        MoyenReceptionEnums moyenReceptionEnums = MoyenReceptionEnums.getMoyenReceptionEnumsByLibelle(libelle);
        switch (moyenReceptionEnums) {
            case MTN_MOBILE_MONEY:
                return produitPaysRepository.findOneByProduit_Nom(PRODUCT_NAME_RETRAIT_MTN).getCode();
            case MOOV_MOBILE_MONEY:
                return produitPaysRepository.findOneByProduit_Nom(PRODUCT_NAME_RETRAIT_MOOV).getCode();
            case VIA_BANQUE:
                return produitPaysRepository.findOneByProduit_Nom(PRODUCT_NAME_RETRAIT_DIAMOND_BANK).getCode();
            default:
                return null;
        }
    }

    private void sendInValidation(RequestContextWs requestContext, MoyenRecPartenaire moyenRecPartenaire) {
        Validation validation = new Validation();
        validation.setReference(VALIDATION_REFERENCE);
        validation.setReferenceId(moyenRecPartenaire.getId());
        validation.setTableref(VALIDATION_TABLE_REFERENCE);
        validation.setIdref(moyenRecPartenaire.getId());
        validation.setNature(VALIDATION_NATURE);
        validation.setTicketvalidation(StringUtils.generateTicketValidation());
        validation.setDateEnvoi(new Timestamp(System.currentTimeMillis()));
        validation.setMotifreceveur(moyenRecPartenaire.getPartenaire().getNom()+" "+moyenRecPartenaire.getPartenaire().getPrenoms());
        validationRepository.save(validation);

    }

    //     F  B  R    //
    public CommonResponseWs createMoyen(MoyenDePaiementWs moyenDePaiementWs) {
        MoyenDePaiement moyenDePaiement = moyenDePaiementRepository.save(mapper.map(moyenDePaiementWs, MoyenDePaiement.class));
        return new CommonResponseWs(WsOperationResponseStatus.success, null, i18n.internationalize("create.moyen.info"), i18n.internationalize("create.moyen.message"), moyenDePaiement.getId(), null, null);
    }


    @Transactional
    public List<ListMoyenDePaiementWs> getListMoyensOfPaysOfPartenaire(Partenaire partenaire) {
        List<MoyenDePaiement> moyensLocaux = moyenDePaiementRepository.findByPays(partenaire.getPays());
        List<MoyenDePaiement> moyensInternationnaux = moyenDePaiementRepository.findByPays(paysRepository.findOneByIndicatif("0"));
        List<MoyenDePaiement> moyensALL = fusionnerWithRemoveDoublon(moyensInternationnaux, moyensLocaux);
        List<ListMoyenDePaiementWs> toreturn = new ArrayList<>();
        for (MoyenDePaiement moyen : moyensALL) {
            ListMoyenDePaiementWs moyenDePaiementWs = convertMoyenDePaiementToListMoyenDePaiementWs(moyen);
            toreturn.add(moyenDePaiementWs);
        }
        return toreturn;
    }

    public ListMoyenDePaiementWs convertMoyenDePaiementToListMoyenDePaiementWs(MoyenDePaiement moyen){
        ListMoyenDePaiementWs moyenDePaiementWs = new ListMoyenDePaiementWs();
        moyenDePaiementWs.setId(moyen.getId());
        moyenDePaiementWs.setAllowInfoAcess(moyen.isAllowInfoAcess());
        moyenDePaiementWs.setAutomatic(moyen.isAutomatic());
        moyenDePaiementWs.setDescription(moyen.getDescription());
        moyenDePaiementWs.setFichier(moyen.getFichier());
       // moyenDePaiementWs.setIdProduitPays(moyen.getIdProduitPays());
        //moyenDePaiementWs.setIndicatifPays(moyen.getIndicatifPays);
        moyenDePaiementWs.setInfo(moyen.getInfo());
        moyenDePaiementWs.setIntitule(moyen.getIntitule());
        //moyenDePaiementWs.setSeulValidationAutomatic(moyen.getSeulValidationAutomatic());
        String listChamp = moyen.getListChamps();
        String[] parts = listChamp.split(",");
        List<ChampWs> champWsList = new ArrayList<>();
        for(String value : parts){
            int a = Integer.parseInt(value);
            Champ champ = champRepository.findOneById(a);
            ChampWs champWs = mapper.map(champ, ChampWs.class);
            champWsList.add(champWs);
        }
        moyenDePaiementWs.setListChamps(champWsList);

        List<ChampWs> champObligatoireWsList = new ArrayList<>();
        String listChampObligatoire = moyen.getListChampsObligatoire();
        String[] parts1 = listChampObligatoire.split(",");
        for(String value : parts1){
            int a = Integer.parseInt(value);
            Champ champ = champRepository.findOneById(a);
            ChampWs champWs = mapper.map(champ, ChampWs.class);
            champObligatoireWsList.add(champWs);
        }
        moyenDePaiementWs.setListChampsObligatoire(champObligatoireWsList);

        return moyenDePaiementWs;
    }


    //INITIALISER UNE OPERATION DE RECHARGE AUTOMATIQUE
    @Transactional
    public CommonResponseWs initialisationRechargeAutomatiqueNotAllowAccessInfo(RequestContextWs requestContext, com.clubchretien.abonnement.ws.RechargeWs createRechargeWs, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException {
        createRechargeWs.setValide(false);
        createRechargeWs.setEtat("INITIALISATION DE LA RECHARCHE");
        String ref = createRechargeWs.getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
        createRechargeWs.setReference(ref);
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
        String url = fileService.saveFile(createRechargeWs.getFile());
        recharge.setFichier(url);
        rechargeRepository.save(recharge);
        activityLogService.makeActivityLog(requestContext, recharge, partenaire, VELOCITY_RECHARGE_INITIALISATION);
        return new CommonResponseWs(WsOperationResponseStatus.success, null, "Reference de la recharge", MSG_INITIATION_RECHARGE, recharge.getId(), recharge.getReference(),null);
    }

    //INITIALISER UNE OPERATION DE RECHARGE CHEZ FLOCASH
    @Transactional
    public CommonResponseWs initialisationRechargeAutomatiqueNotAllowAccessInfoCaseFlocash(RequestContextWs requestContext, Partenaire partenaire, CreateFlocashRechargeWs createFlocashRechargeWs) throws FileUploadException, FloCashException, OwomiException {
        /*initialisation de la recharge chez owomi*/
        com.clubchretien.abonnement.ws.RechargeWs createRechargeWs = new com.clubchretien.abonnement.ws.RechargeWs();
        createRechargeWs.setValide(false);
        createRechargeWs.setEtat("INITIALISATION DE LA RECHARCHE");
        String ref = ("VIREMENT" + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime());
        createRechargeWs.setReference(ref);
        createRechargeWs.setType("VIREMENT");
        createRechargeWs.setDate(new Date());
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        createRechargeWs.setIdCorporate(corporate.getId());
        createRechargeWs.setMontant(createFlocashRechargeWs.getClientOrderInit().getOrder().getAmount());
        createRechargeWs.setEmetteur(partenaire.getTelephone());

        createRechargeWs.setIdMoyenDePaiement(moyenDePaiementRepository.findByIntitule("FLOCASH").getId());
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
        //String url = fileService.saveFile(file);
        //recharge.setFichier(url);

        FloCashClientOrderInitWs clientOrderInit = new FloCashClientOrderInitWs();
        FloCashOrderInputWs orderInput = new FloCashOrderInputWs();
        //Récupération du order info
        orderInput.setAmount(createFlocashRechargeWs.getClientOrderInit().getOrder().getAmount());
        orderInput.setCurrency(createFlocashRechargeWs.getClientOrderInit().getOrder().getCurrency());
        orderInput.setItem_name("recharge owomi");
        orderInput.setItem_price(createFlocashRechargeWs.getClientOrderInit().getOrder().getAmount().toString());
        orderInput.setOrderId("Flo"+Math.random() * 100000);
        orderInput.setQuantity("1");
        clientOrderInit.setOrder(orderInput);
        //clientOrderInit.setOrder(orderInput);
        //Ajout du payer info
        FloCashPayerInputWs payerInput = new FloCashPayerInputWs();
        payerInput.setMobile(FlocashConstant.MOBILE);
        payerInput.setEmail(FlocashConstant.EMAIL);
        payerInput.setCountry(FlocashConstant.COUNTRY);
        payerInput.setFirstName(FlocashConstant.FIRSTNAME);
        payerInput.setLastName(FlocashConstant.LASTNAME);
        clientOrderInit.setPayer(payerInput);
        logger.debug("payer info " + new Gson().toJson(payerInput));
        /*initialisation de la recharge chez flocash*/
        FloCashOrderInitResponse response = floCashServiceClient.initOrder(clientOrderInit);
        logger.debug("response =======" + new Gson().toJson(response.getPaymentOptions()));
        logger.debug("response =======" + new Gson().toJson(response));
        //Exception gérer au niveau du client
        recharge.setMarchandTransactionRef(response.getOrder().getTraceNumber());
        rechargeRepository.save(recharge);
        //activityLogService.makeActivityLog(requestContext, recharge, partenaire, VELOCITY_RECHARGE_INITIALISATION);
        return new CommonResponseWs(WsOperationResponseStatus.success, response.getPaymentOptions(), "Reference de la recharge", MSG_INITIATION_RECHARGE, null, response.getOrder().getTraceNumber(),null);
    }

    //récupération de la liste des champs à remplir pour le payement chez flocash
    @Transactional
    public CommonResponseWs getPayementOptionInfos(RequestContextWs requestContextWs, FloCashSelectPaymentInputWs floCashSelectPaymentInputWs) throws OwomiException, FloCashException {
        FloCashSelectPaymentInputWs selectPaymentInputWs = new FloCashSelectPaymentInputWs();

        FloCashSelectPayementOptionOrderWs payementOptionOrderWs = new FloCashSelectPayementOptionOrderWs();
        payementOptionOrderWs.setTraceNumber(floCashSelectPaymentInputWs.getOrder().getTraceNumber());

        FloCashSelectPayementOptionWs payementOptionWs = new FloCashSelectPayementOptionWs();
        payementOptionWs.setId(floCashSelectPaymentInputWs.getPayOption().getId());

        selectPaymentInputWs.setOrder(payementOptionOrderWs);
        selectPaymentInputWs.setPayOption(payementOptionWs);
        FloCashSelectPayementResponse response = floCashServiceClient.selectPayment(selectPaymentInputWs);
        logger.debug("liste des champs ===========" + response);
        FloCashSelectPayementResponse theresponse = new FloCashSelectPayementResponse();
        FloCashOrderOutput orderOutput = new FloCashOrderOutput();
        orderOutput.setRedirect(response.getOrder().getRedirect());
        orderOutput.setAdditionFields(response.getOrder().getAdditionFields());
        theresponse.setOrder(orderOutput);
        return new CommonResponseWs(WsOperationResponseStatus.success, theresponse, "recharge payement infos", FlocashConstant.GET_PAYEMENT_INFO, payementOptionWs.getId(), payementOptionOrderWs.getTraceNumber(),200);
    }


    public CommonResponseWs validateTransactionValidate(String traceNumber,HttpServletRequest request) throws OwomiException {
        CommonResponseWs responseWs =  new CommonResponseWs();
                //floCashServiceClient.providePaymentInfo(traceNumber,request);
        return responseWs;
    }


    @Transactional
    public CommonResponseWs checkRechargeFlocashStatus(RequestContextWs requestContext, Partenaire partenaire, String refTransaction) throws DataNotFoundException, OwomiException, NotificationException {
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        Recharge recharge = rechargeRepository.findByMarchandTransactionRef(refTransaction);
        EntityUtils.checkIfEntityIsNull(Recharge.class, recharge, recharge.getId());
        CommonResponseWs commonResponseWs = new CommonResponseWs();

        try {
            FloCashSelectPayementResponse response = floCashServiceClient.getOrder(refTransaction);
            logger.debug("============= "+response);
            String status = response.getOrder().getPartnerMessage();
            if (status.equalsIgnoreCase("success")) {
                recharge.setValide(true);
                recharge.setEtat("PAYEE");
                recharge = rechargeRepository.save(recharge);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                // "Recharge Validée", null);
                commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(), recharge.getMontant());
                commonResponseWs.setReference(recharge.getReference());
                commonResponseWs.setMessage("" + response.getOrder().getStatusDesc());
            } else if (status.equalsIgnoreCase("decline")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_FAILED);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Vérification échouée pour la recharge de référence:  " + recharge.getReference(),
                //  "Echec de la  vérification", null);
                commonResponseWs.setMessage("" + response.getOrder().getStatusDesc());
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setReference(recharge.getReference());
            } else if (status.equalsIgnoreCase("pending")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                //   "Vérification en attente", null);
                commonResponseWs.setMessage("" + response.getOrder().getStatusDesc());
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            }
        } catch (OwomiException ex) {
            ex.setDetailMessage("flocash service exception");
            ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
            ex.setRequestContextWs(requestContext);
            activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
            //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
            //   "Vérification en attente", null);
            commonResponseWs.setMessage("pending");
            commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
            commonResponseWs.setReference(recharge.getReference());
        }

        return commonResponseWs;
    }

    // RECHARGE : CAS AUTOMATIQUE OU ON PAS ACCES A TOUTES LES INFOS : INFOS SENSIBLES
    @Transactional
    public CommonResponseWs checkRechargeStatus(RequestContextWs requestContext, Partenaire partenaire, CheckStatusWs checkStatusWs, String reference) throws DataNotFoundException, OwomiException, NotificationException {
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        Recharge recharge = rechargeRepository.findByReference(reference);
        EntityUtils.checkIfEntityIsNull(Recharge.class, recharge, recharge.getId());
        CommonResponseWs commonResponseWs = new CommonResponseWs();

            try {
                CheckStatusResponseWs checkStatusResponseWs = touchServiceClient.checkStatus(checkStatusWs);
                String status = checkStatusResponseWs.getStatus();
                if (status.equalsIgnoreCase("sucess")) {
                    recharge.setValide(true);
                    recharge.setEtat(i18n.internationalize("recharge.verification.status.etat"));
                    recharge = rechargeRepository.save(recharge);
                    //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                           // "Recharge Validée", null);
                    commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(), recharge.getMontant());
                    commonResponseWs.setReference(recharge.getReference());
                    commonResponseWs.setMessage(i18n.internationalize("recharge.verification.status.succes"));
                } else if (status.equalsIgnoreCase("failed")) {
                    activityLogService.makeActivityLog(requestContext, recharge, partenaire, i18nCommons.internationalize("MobileConstant.MSG_VERIFICATION_FAILED"));
                    //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Vérification échouée pour la recharge de référence:  " + recharge.getReference(),
                          //  "Echec de la  vérification", null);
                    commonResponseWs.setMessage(i18n.internationalize("recharge.verification.status.echoue"));
                    commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                    commonResponseWs.setReference(recharge.getReference());
                } else if (status.equalsIgnoreCase("pending")) {
                    activityLogService.makeActivityLog(requestContext, recharge, partenaire, i18nCommons.internationalize("MobileConstant.MSG_VERIFICATION_EN_ATTENTE"));
                    //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                         //   "Vérification en attente", null);
                    commonResponseWs.setMessage(i18n.internationalize("recharge.verification.status.attente"));
                    commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                    commonResponseWs.setReference(recharge.getReference());
                }
            } catch (OwomiException ex) {
                ex.setDetailMessage(i18n.internationalize("recharge.verification.status.ex.not.responding"));
                ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
                ex.setRequestContextWs(requestContext);
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, i18nCommons.internationalize("MobileConstant.MSG_VERIFICATION_EN_ATTENTE"));
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                     //   "Vérification en attente", null);
                commonResponseWs.setMessage(i18n.internationalize("recharge.verification.status.ex.not.responding"));
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            }

        return commonResponseWs;
    }

    // RECHARGE MANUELLE
    @Transactional
    public CommonResponseWs initialisationRechargeManuelle(RequestContextWs requestContext, com.clubchretien.abonnement.ws.RechargeWs createRechargeWs, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException {
        createRechargeWs.setValide(false);
        createRechargeWs.setEtat("EN ATTENTE DE VALIDATION");
        String ref = createRechargeWs.getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
        createRechargeWs.setReference(ref);
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
        String url = fileService.saveFile(createRechargeWs.getFile());
        recharge.setFichier(url);
        recharge = rechargeRepository.save(recharge);
        Corporate corporate = corporateRepository.findOneById(createRechargeWs.getIdCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,createRechargeWs.getIdCorporate());
        CommonResponseWs commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(),recharge.getMontant());
        commonResponseWs.setReference(recharge.getReference());
        commonResponseWs.setMessage("Recharge initalisée avec succes. En attente de validation");
        activityLogService.makeActivityLog(requestContext, recharge, partenaire, VELOCITY_RECHARGE_DEPOT);
        return commonResponseWs;
    }


    //INITIALISATION OPERATION DE RECHARGE PAR STRIPE
    @Transactional
    public CommonResponseWs initialisationRechargeAutomatiqueNotAllowAccessInfoCaseStripe(RequestContextWs requestContextWs, Partenaire partenaire, CreateRechargeStripeWs createRechargeStripeWs) throws StripeException {
        com.clubchretien.abonnement.ws.RechargeWs createRechargeWs = new com.clubchretien.abonnement.ws.RechargeWs();
        createRechargeWs.setValide(false);
        createRechargeWs.setEtat("INITIALISATION DE LA RECHARCHE");
        String ref = ("VIREMENT" + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime());
        createRechargeWs.setReference(ref);
        createRechargeWs.setType("VIREMENT");
        createRechargeWs.setDate(new Date());
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        createRechargeWs.setIdCorporate(corporate.getId());
        createRechargeWs.setMontant(createRechargeStripeWs.getAmount().doubleValue());
        createRechargeWs.setEmetteur(partenaire.getTelephone());

        createRechargeWs.setIdMoyenDePaiement(moyenDePaiementRepository.findByIntitule("STRIPE").getId());
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
        //String url = fileService.saveFile(file);
        //recharge.setFichier(url);
        Map<String, Object> param = new HashMap<>();
        param.put("amount", createRechargeStripeWs.getAmount());
        param.put("currency", createRechargeStripeWs.getCurrency());
        StripeServiceClient stripeServiceClient = new StripeServiceClient();
        PaymentIntent paymentIntent = stripeServiceClient.createPaymentIntent(param);
        recharge.setMarchandTransactionRef(paymentIntent.getId());
        rechargeRepository.save(recharge);
        //activityLogService.makeActivityLog(requestContext, recharge, partenaire, VELOCITY_RECHARGE_INITIALISATION);
        return new CommonResponseWs(WsOperationResponseStatus.success, paymentIntent.getClientSecret(), "Reference de la recharge", MSG_INITIATION_RECHARGE, recharge.getId(), recharge.getReference(),null);
    }

    @Transactional
    public CommonResponseWs checkRechargeStripeStatus(RequestContextWs requestContext, Partenaire partenaire, String owomiReference, String id) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        Recharge recharge = rechargeRepository.findByReference(owomiReference);
        logger.debug("recharge =========" + recharge.getId());
        EntityUtils.checkIfEntityIsNull(Recharge.class, recharge, recharge.getId());
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        try {
            StripeServiceClient stripeServiceClient = new StripeServiceClient();
            PaymentIntent paymentIntent = stripeServiceClient.getPaymentIntent(id);
            logger.debug("============= "+paymentIntent);
            String status = paymentIntent.getStatus();
            if (status.equalsIgnoreCase("success")) {
                recharge.setValide(true);
                recharge.setEtat("PAYEE");
                recharge = rechargeRepository.save(recharge);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                // "Recharge Validée", null);
                String message = "Payement effectué avec succès";
                commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(), recharge.getMontant());
                commonResponseWs.setReference(recharge.getReference());
                commonResponseWs.setMessage("" + message);
            } else if (status.equalsIgnoreCase("decline")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_FAILED);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Vérification échouée pour la recharge de référence:  " + recharge.getReference(),
                //  "Echec de la  vérification", null);
                String message = "Echec du payement";
                commonResponseWs.setMessage("" + message);
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setReference(recharge.getReference());
            } else if (status.equalsIgnoreCase("pending")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                //   "Vérification en attente", null);
                String message = "payement en cours de traitement";
                commonResponseWs.setMessage("" + message);
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            }
        } catch (StripeException | OwomiException ex) {

            activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
            //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
            //   "Vérification en attente", null);
            String message = "payement en cours de traitement";
            commonResponseWs.setMessage("pending");
            commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
            commonResponseWs.setReference(recharge.getReference());
        }
        return commonResponseWs;
    }

    @Transactional
    public CommonResponseWs doRechargeMoov(RequestContextWs requestContextWs, Partenaire partenaire, CreateRechargeMoovWs createRechargeMoovWs) throws OwomiException, DataNotFoundException {
        com.clubchretien.abonnement.ws.RechargeWs createRechargeWs = new com.clubchretien.abonnement.ws.RechargeWs();
        createRechargeWs.setValide(false);
        createRechargeWs.setEtat("INITIALISATION DE LA RECHARCHE");
        String ref = createRechargeMoovWs.getCreateRecharge().getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
        createRechargeWs.setReference(ref);
        createRechargeWs.setType(createRechargeMoovWs.getCreateRecharge().getType());
        createRechargeWs.setDate(createRechargeMoovWs.getCreateRecharge().getDate());
        createRechargeWs.setIdCorporate(createRechargeMoovWs.getCreateRecharge().getCorporate().getId());
        createRechargeWs.setMontant(createRechargeMoovWs.getCreateRecharge().getMontant());
        createRechargeWs.setEmetteur(createRechargeMoovWs.getCreateRecharge().getEmetteur());

        createRechargeWs.setIdMoyenDePaiement(createRechargeMoovWs.getCreateRecharge().getMoyenDePaiement().getId());
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);

        PushWs thePushWs = new PushWs();
        thePushWs.setAmount(createRechargeMoovWs.getPushWs().getAmount());
        thePushWs.setMessage(MobileConstant.TRANSACTION_FLOOZ_MESSAGE);
        thePushWs.setMsisdn(createRechargeMoovWs.getPushWs().getMsisdn());
        thePushWs.setExternaldata1(createRechargeMoovWs.getPushWs().getExternaldata1());
        thePushWs.setExternaldata2(createRechargeMoovWs.getPushWs().getExternaldata2());
        thePushWs.setFee(createRechargeMoovWs.getPushWs().getFee());
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        try {
            PushResponseWs response = moovBeninFloozClient.pushTransaction(thePushWs);
            if (response.getResult().getStatus().equalsIgnoreCase("0")) {
                recharge.setValide(true);
                recharge.setEtat("PAYEE");
                recharge = rechargeRepository.save(recharge);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                // "Recharge Validée", null);
                commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(), recharge.getMontant());
                commonResponseWs.setMessage("Recharge réaliser avec succès.");
                commonResponseWs.setReference(recharge.getReference());
            }else{
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setMessage("Recharge non effectué");
                commonResponseWs.setCode(500);
            }
        } catch (OwomiException ex) {
            ex.setDetailMessage("Flooz service not responding");
            ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
            ex.setRequestContextWs(requestContextWs);
            activityLogService.makeActivityLog(requestContextWs, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
            //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
            //   "Vérification en attente", null);
            commonResponseWs.setMessage("Verification en attente");
            commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
            commonResponseWs.setReference(recharge.getReference());
            commonResponseWs.setCode(200);
        }

        return commonResponseWs;
    }



    // RECHARGE : CAS AUTOMATIQUE OU ON ACCES A TOUTES LES INFOS : PAS INFOS SENSIBLES
    @Transactional
    public CommonResponseWs rechargeAutomatiqueAllowAccessInfo(RequestContextWs requestContext, com.clubchretien.abonnement.ws.RechargeWs createRechargeWs, Partenaire partenaire) throws DataNotFoundException, FileUploadException, ParseException, OwomiException, NotificationException {
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        createRechargeWs.setIdCorporate(corporate.getId());
        createRechargeWs.setType("automatic");
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,corporate.getId());
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        String ref = createRechargeWs.getType() + Math.random() * 100000 + new Timestamp(System.currentTimeMillis()).getTime();
        createRechargeWs.setReference(ref);
        createRechargeWs.setEmetteur(partenaire.getTelephone());
        createRechargeWs.setEtat("En cours de validation");
        createRechargeWs.setIdBanque(createRechargeWs.getIdMoyenDePaiement());
        Recharge recharge = mapRechargeWsToRecharge(createRechargeWs, partenaire);
        recharge.setReference(ref);
        String url = fileService.saveFile(createRechargeWs.getFile());
        recharge.setFichier(url);
        recharge = rechargeRepository.save(recharge);
        MoyenDePaiement moyenDePaiement = moyenDePaiementRepository.findOneById(createRechargeWs.getIdMoyenDePaiement());
        EntityUtils.checkIfEntityIsNull(MoyenDePaiement.class, moyenDePaiement, createRechargeWs.getIdMoyenDePaiement());
        if (moyenDePaiement.getIntitule().contains("MTN") || moyenDePaiement.getIntitule().contains("mtn")
                || moyenDePaiement.getIntitule().contains("Mtn")) {
            CashOutResponseWS cashOutResponseWS = touchServiceClient.cashOut(cashout(createRechargeWs.getNumMobileMoney(),recharge.getReference(),createRechargeWs.getMontant()));
            logger.debug("client response **************** " + cashOutResponseWS);
            try {
                String status = cashOutResponseWS.getStatus();
               commonResponseWs = touchReturnControl(status,corporate.getNumeroCompte().getId(),moyenDePaiement.getSeulValidationAutomatic(),partenaire,requestContext,commonResponseWs,recharge);
            } catch (OwomiException ex) {
                ex.setDetailMessage("Touch service not responding");
                ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
                ex.setRequestContextWs(requestContext);
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_RECHARGE_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                    //    "Recharge en attente", null);
                commonResponseWs.setMessage("Recharge en attente");
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            }
        } else if (moyenDePaiement.getIntitule().contains("orange") || moyenDePaiement.getIntitule().contains("Orange")
                || moyenDePaiement.getIntitule().contains("ORANGE")) {
            CashOutResponseWS cashOutResponseWS = touchServiceClient.cashOut(cashout(partenaire.getTelephone(),recharge.getReference(),createRechargeWs.getMontant()));
            try {
                String status = cashOutResponseWS.getStatus();
                commonResponseWs = touchReturnControl(status,corporate.getNumeroCompte().getId(),moyenDePaiement.getSeulValidationAutomatic(),partenaire,requestContext,commonResponseWs,recharge);
            } catch (OwomiException ex) {
                ex.setDetailMessage("Touch service not responding");
                ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
                ex.setRequestContextWs(requestContext);
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_RECHARGE_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                       // "Recharge en attente", null);
                commonResponseWs.setMessage("Recharge en attente");
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());

            }
        }else if(moyenDePaiement.getIntitule().contains("moov") || moyenDePaiement.getIntitule().contains("Moov") || moyenDePaiement.getIntitule().contains("MOOV")){

            PushWs thePushWs = new PushWs();
            thePushWs.setAmount(createRechargeWs.getMontant().toString());
            thePushWs.setMessage(MobileConstant.TRANSACTION_FLOOZ_MESSAGE);
            thePushWs.setMsisdn(createRechargeWs.getNumMobileMoney());
            thePushWs.setExternaldata1("");
            thePushWs.setExternaldata2("");
            thePushWs.setFee(createRechargeWs.getMontant().toString());
           // Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
            try {
                PushResponseWs response = moovBeninFloozClient.pushTransaction(thePushWs);
                if (response.getResult().getStatus().equalsIgnoreCase("0")) {
                    recharge.setValide(true);
                    recharge.setEtat("PAYEE");
                    recharge = rechargeRepository.save(recharge);
                    //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                    // "Recharge Validée", null);
                    commonResponseWs = saveGranLivre(corporate.getNumeroCompte().getId(), recharge.getMontant());
                    commonResponseWs.setMessage("Recharge réaliser avec succès.");
                    commonResponseWs.setReference(recharge.getReference());
                }else{
                    commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                    commonResponseWs.setMessage("Recharge non effectué");
                    commonResponseWs.setCode(500);
                }
            } catch (OwomiException ex) {
                ex.setDetailMessage("Flooz service not responding");
                ex.setErrorCodeEnums(ErrorCodeEnums.RESTFUL_CLIENT_ERROR);
                ex.setRequestContextWs(requestContext);
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_VERIFICATION_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                //   "Vérification en attente", null);
                commonResponseWs.setMessage("Verification en attente");
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
                commonResponseWs.setCode(200);
            }
        }
            return commonResponseWs;
        }

        private CashOutWS cashout(String tel,String ref,double montant){
            CashOutWS cashOutWS = new CashOutWS();
            cashOutWS.setService_id(MobileConstant.MTN_SERVICE_ID);
            cashOutWS.setRecipient_phone_number(tel);
            cashOutWS.setPassword_api(MobileConstant.PASSWORD_API);
            cashOutWS.setPartner_transaction_id(ref);
            cashOutWS.setPartner_id(MobileConstant.PARTNER_ID);
            cashOutWS.setLogin_api(MobileConstant.LOGIN_API);
            cashOutWS.setCall_back_url(MobileConstant.CALL_BACK_URL);
            cashOutWS.setAmount(Double.toString(montant));
            return cashOutWS;
        }
        private CommonResponseWs touchReturnControl(String status,int numCompte,double seuilMontant,Partenaire partenaire, RequestContextWs requestContext, CommonResponseWs commonResponseWs, Recharge recharge) throws OwomiException, DataNotFoundException {
            if (status.equalsIgnoreCase("sucess")) {
                recharge.setValide(true);
                recharge.setEtat("PAYEE");
                recharge=rechargeRepository.save(recharge);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge de référence :" + recharge.getReference() + " effectuée avec succès.  \n Etat: " + recharge.getEtat(),
                //"Recharge Réussie", null);
                commonResponseWs = saveGranLivre(numCompte,recharge.getMontant());
                commonResponseWs.setReference(recharge.getReference());
                commonResponseWs.setMessage("Recharge initalisée avec succes.");
            } else if (status.equalsIgnoreCase("failed")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_RECHARGE_FAILED);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge échouée pour la recharge de référence:  " + recharge.getReference(),
                //  "Echec de la  recharge", null);
                commonResponseWs.setMessage("Recharge échouée");
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setReference(recharge.getReference());
            } else if (status.equalsIgnoreCase("pending")) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_RECHARGE_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                //  "Recharge en attente", null);
                commonResponseWs.setMessage("Recharge en attente");
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            }
            else if (recharge.getMontant() > seuilMontant) {
                activityLogService.makeActivityLog(requestContext, recharge, partenaire, MobileConstant.MSG_RECHARGE_EN_ATTENTE);
                //notificationUtils.notification(10, partenaire.getFirebasetoken(), "Recharge en attente de vérification. Référence: " + recharge.getReference(),
                //  "Recharge en attente", null);
                commonResponseWs.setMessage("Recharge en attente");
                commonResponseWs.setInfo("Le montant de la recharge est supérieur au seuil autorisé");
                commonResponseWs.setStatus(WsOperationResponseStatus.inProcess);
                commonResponseWs.setReference(recharge.getReference());
            } else {
                commonResponseWs.setReference(recharge.getReference());
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setMessage("Vérifier le moyen de paiement");
            }
            return commonResponseWs;
        }


        private List<MoyenDePaiement> fusionnerWithRemoveDoublon
        (List < MoyenDePaiement > list1, List < MoyenDePaiement > list2){
            List<MoyenDePaiement> moyensALL = new ArrayList<>();
            moyensALL.addAll(list1);
            moyensALL.addAll(list2);
            Set set = new HashSet();
            set.addAll(moyensALL);
            ArrayList distinctList = new ArrayList(set);
            return distinctList;
        }

        private Recharge mapRechargeWsToRecharge (CreateRechargeWs createRechargeWs, Partenaire partenaire){
            Recharge recharge = new Recharge();
            Corporate corporate = corporateRepository.findOneById(createRechargeWs.getIdCorporate());
           // EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,createRechargeWs.getIdCorporate());
            recharge.setCorporate(corporate);
            recharge.setDate(createRechargeWs.getDate());
            recharge.setTelephone(createRechargeWs.getNumMobileMoney());
            recharge.setFichier(createRechargeWs.getFichier());
            recharge.setIdBanque(createRechargeWs.getIdBanque());
            recharge.setMontant(createRechargeWs.getMontant());
            recharge.setPartenaire(partenaire);
            recharge.setReference(createRechargeWs.getReference());
            recharge.setType(createRechargeWs.getType());
            recharge.setValide(createRechargeWs.getValide());
            recharge.setEtat(createRechargeWs.getEtat());
            recharge.setEmetteur(partenaire.getNom() + "" + partenaire.getPrenoms());
            recharge.setBanqueEmettrice(createRechargeWs.getBanqueEmettrice());
            recharge.setReferenceExterne(createRechargeWs.getReferenceExterne());
            recharge.setDateExterne(createRechargeWs.getDateExterne());
            //recharge.setMoyenDePaiement(moyenDePaiementRepository.findOneById(createRechargeWs.getIdMoyenDePaiement()));
            //EntityUtils.checkIfEntityIsNull(MoyenDePaiement.class, moyenDePaiementRepository.findOneById(createRechargeWs.getIdMoyenDePaiement()), createRechargeWs.getIdMoyenDePaiement());
            return recharge;
        }
        private RechargeWs mapRechargeToRechargeWs (Recharge recharge) throws DataNotFoundException {
            RechargeWs rechargeWs = new RechargeWs();
            rechargeWs.setBanque(mapper.map(banqueRepository.findOneById(recharge.getIdBanque()), BanqueWs.class));
            EntityUtils.checkIfEntityIsNull(Banque.class, banqueRepository.findOneById(recharge.getIdBanque()), recharge.getIdBanque());
            rechargeWs.setCorporate(mapper.map(recharge.getCorporate(), CorporateWs.class));
            rechargeWs.setDate(recharge.getDate());
            rechargeWs.setNumMobileMoney(recharge.getTelephone());
            rechargeWs.setEtat(recharge.getEtat());
            rechargeWs.setFichier(recharge.getFichier());
            rechargeWs.setId(recharge.getId());
            rechargeWs.setMontant(recharge.getMontant());
            rechargeWs.setReference(recharge.getReference());
            rechargeWs.setType(recharge.getType());
            rechargeWs.setValide(recharge.isValide());
            //rechargeWs.setMoyenDePaiement(mapper.map(recharge.getMoyenDePaiement(), MoyenDePaiementWs.class));
            return rechargeWs;
        }
        private CommonResponseWs saveGranLivre(int numeroCompte, Double montant) throws OwomiException {
                    ResumeGrandLivreWs resumeGrandLivreWs = new ResumeGrandLivreWs();
                    resumeGrandLivreWs.setEcriture(Integer.parseInt(StringUtils.generateCodeVerification()));
                    resumeGrandLivreWs.setLibelle(i18n.internationalize("save.grand.livre.libelle"));
                    resumeGrandLivreWs.setMontant(montant);
                    resumeGrandLivreWs.setNumeroCompte(numeroCompte);
                    resumeGrandLivreWs.setSens("1");
                   // return accountServiceClient.saveGrandLivre(resumeGrandLivreWs);
            return new CommonResponseWs();
        }

        public MoyenDePaiementRepository getMoyenDePaiementRepository () {
            return moyenDePaiementRepository;
        }

        public void setMoyenDePaiementRepository (MoyenDePaiementRepository moyenDePaiementRepository){
            this.moyenDePaiementRepository = moyenDePaiementRepository;
        }

        public CommonResponseWs listMoyenDePayement(Partenaire partenaire){
            List<MoyenDePaiement> moyenDePaiements = moyenDePaiementRepository.findByPays(partenaire.getPays());
            List<MoyenPaiementWs> out = new ArrayList<>();
            for (MoyenDePaiement moyenDePaiement : moyenDePaiements) {
                out.add(moyenDePayementToMoyenPayementWs(moyenDePaiement));
            }
            return new CommonResponseWs(WsOperationResponseStatus.success, out, "Requete exécutée avec succès", "Reussi", null, null, 200);
        }

        private MoyenPaiementWs moyenDePayementToMoyenPayementWs(MoyenDePaiement moyenDePaiement){
            MoyenPaiementWs moyenPaiementWs = new MoyenPaiementWs();
            moyenPaiementWs.setId(moyenDePaiement.getId());
            moyenPaiementWs.setDescription(moyenDePaiement.getDescription());
            moyenPaiementWs.setFichier(moyenDePaiement.getFichier());
            moyenPaiementWs.setInfo(moyenDePaiement.getInfo());
            moyenPaiementWs.setAllowInfoAcess(moyenDePaiement.isAllowInfoAcess());
            moyenPaiementWs.setAutomatic(moyenDePaiement.isAutomatic());
            moyenPaiementWs.setIdProduitPays(moyenDePaiement.getIdProduitPays());
            moyenPaiementWs.setIndicatifPays(moyenDePaiement.getPays().getIndicatif());
            moyenPaiementWs.setIntitule(moyenDePaiement.getIntitule());
            //moyenPaiementWs.setListChamps(moyenDePaiement.getListChamps());
            //moyenPaiementWs.setListChampsObligatoire(moyenDePaiement.getListChampsObligatoire());
            moyenPaiementWs.setSeulValidationAutomatic(moyenDePaiement.getSeulValidationAutomatic());
            return moyenPaiementWs;
        }
    }
