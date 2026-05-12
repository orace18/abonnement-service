package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.configs.ProductsInfos;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.ws.CorporateDocumentWs;
import com.google.gson.Gson;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.enums.ecofam.AbonneGroupeTypeEnums;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.FileUploadException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.CollectionUtils;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.utils.MobileUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.acm.AcmAccountSoldeWs;
import com.owomi.commons.ws.acm.AcmSoldeWs;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import com.owomi.rest.client.accountService.AccountMouvementSoldeServiceClient;
import com.owomi.rest.client.dataservice.CorporateMatriculeServiceClient;
import com.owomi.rest.client.network.NetworkToolClient;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.clubchretien.abonnement.constants.MobileConstant.XOF;

@Component
public class CorporateService implements MobileConstant, ResponseConstant {
    private static Logger logger = LogManager.getLogger(CorporateService.class);

    private static String[] cdpIds ={"5607","5608","5829","5840","11508"};
    @Autowired
    TypeCorporateRepository typeCorporateRepository;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    PartenaireCorporateRepository  partenaireCorporateRepository;
    @Autowired
    PlanComptableBaseRepository planComptableBaseRepository;
    @Autowired
    PlanComptableRepository planComptableRepository;
    @Autowired
    FormeJuridiqueRepository formeJuridiqueRepository;
    @Autowired(required = false)
    Mapper mapper;
    @Autowired
	TypeDocumentRepository typeDocumentRepository;
	@Autowired(required = false)
	FileService fileService;
	@Autowired
	DocumentRepository documentRepository;
	@Autowired
	HabileteRepository habileteRepository;
	@Autowired
	TransactionFinanciereService transactionFinanciereService;
	@Autowired
	PartenaireBonusAccountService partenaireBonusAccountService;
	@Autowired
	ValidationRepository validationRepository;
	@Autowired
	PanierRepository panierRepository;
	@Autowired
	PanierProduitRepository panierProduitRepository;
	@Autowired
	ProduitPaysRepository produitPaysRepository;
	@Autowired
	NatureCorporateRepository natureCorporateRepository;
	@Autowired
    TransfertsRepository transfertsRepository;
    @Autowired
    RetraitRepository retraitRepository;
    @Autowired
    GrandLivreRepository grandLivreRepository;
    @Autowired
    ProductsInfos productsInfos;
    @Autowired
    PanierService panierService;
    @Autowired
    OperationService operationService;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    FormeJuridiqueTypeDodumentRepository formeJuridiqueTypeDodumentRepository;
    @Autowired
    PaysRepository paysRepository;
    @Autowired
    AdressePartenaireRepository adressePartenaireRepository;
    @Autowired
    CommandeRepository commandeRepository;
    @Autowired
    ShopizerServiceClient shopizerServiceClient;
    @Autowired
    AccountSoldeRepository accountSoldeRepository;
    @Autowired(required = false)
    HttpServletRequest httpServletRequest;
    @Autowired(required = false)
    UserService userService;
    @Autowired
    ProduitRepository produitRepository;
    @Autowired
    ConfigProduitRepository configProduitRepository;
    @Autowired
    NetworkToolClient networkToolClient;

    @Autowired
    RechargeService rechargeService;

    @Autowired
    ValidationService validationService;
    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;

    @Autowired
    CorporateMatriculeServiceClient corporateMatriculeServiceClient;

    @Autowired
    AccountMouvementSoldeServiceClient accountMouvementSoldeServiceClient;

    @Autowired
    HumanKycValidationRepository humanKycValidationRepository;

    public void createDefaultCorporate(RequestContextWs requestContext, Partenaire partenaire) {
        TypeCorporate typeCorporate = typeCorporateRepository.findOneByLibelle(TypeCorporateEnums.ALTERNATIF.getLibelle());
        Habilete habilete = !habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).isEmpty() ? habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).get(0) : null;
        FormeJuridique formeJuridique = formeJuridiqueRepository.findByLibelle(FormeJuridiqueEnums.PersonnePhysique.getForm());
        Corporate corporate = new Corporate();
        corporate.setLibelle(partenaire.getNom() + " " + partenaire.getPrenoms());
        corporate.setTypeCorporate(typeCorporate);
        corporate.setNumeroCompte(createPlanComptable(requestContext, partenaire.getNom() + " " + partenaire.getPrenoms()));
        corporate.setStatut(Boolean.TRUE);
        corporate.setMatricule(partenaire.getTelephone());
        corporate.setFormeJuridique(formeJuridique);
        corporate.setAutorise(Boolean.FALSE);
        corporate.setActif(Boolean.TRUE);
        corporate.setCdp(Boolean.FALSE);
        corporate.setAgentAllowed(Boolean.FALSE);
        corporate.setMasterAllowed(Boolean.FALSE);
        corporate.setProvisoire(Boolean.FALSE);
        corporate.setAllowedCashOut(Boolean.TRUE);
		if (partenaire.getConnectedSansNumero()) {
			corporate.setProvisoire(Boolean.TRUE);
            corporate.setAllowedCashOut(Boolean.FALSE);
            corporate.setStatut(Boolean.FALSE);
		}
		corporate.setNatureCorporate(natureCorporateRepository.findOneByLibelle(NatureCorporateEnums.UTILISATEUR.getNature()));
		corporate = corporateRepository.save(corporate);
		PartenaireCorporate partenaireCorporate = new PartenaireCorporate();
		partenaireCorporate.setPartenaire(partenaire);
		partenaireCorporate.setCorporate(corporate);
		partenaireCorporate.setHabilete(habilete);
		partenaireCorporate.setCurrent(Boolean.TRUE);
		partenaireCorporate.setActif(Boolean.TRUE);
		partenaireCorporate.setOwner(Boolean.TRUE);
		partenaireCorporateRepository.save(partenaireCorporate);
    }

    private String uniqueCorporateMatricule(Pays pays) {
        String matricule = null;
        for (int i = 0; i < 3; i++) {
            matricule = pays.getIndicatif() + MobileUtils.generateCompteNumber(6);
            matricule = matricule.replace("-", "");
            if (corporateRepository.findByMatricule(matricule) == null) {
                return matricule;
            }
        }
        return matricule;
    }

    @Transactional
    public CreateCorporateResponseWs createCorporate(RequestContextWs requestContext, Partenaire partenaire, CreateCorporateWs createCorporateWs, String type) throws DataNotFoundException, OwomiException {
        createCorporateWs.setNatureCorporate(NatureCorporateEnums.UTILISATEUR.getNature());
        //if (!partenaire.isIdentifier()) throw new OwomiException(i18n.internationalize("create.corporate.unidentified.short.message"), ErrorCodeEnums.IDENTIFY_ERROR, i18n.internationalize("create.corporate.unidentified.long.message"));
        TypeCorporateEnums typeCorporateEnums = TypeCorporateEnums.getTypeCorporateEnumsByCode(createCorporateWs.getTypeCorporate());
        TypeCorporate typeCorporate = typeCorporateRepository.findOneByLibelle(TypeCorporateEnums.ALTERNATIF.getLibelle());
        EntityUtils.checkIfEntityIsNull(TypeCorporate.class, typeCorporate, "Libelle", typeCorporateEnums.getLibelle());
		FormeJuridique formeJuridique = formeJuridiqueRepository.findOneById(createCorporateWs.getFormeJuridique());
        EntityUtils.checkIfEntityIsNull(FormeJuridique.class, formeJuridique, createCorporateWs.getFormeJuridique());
        if (createCorporateWs.getRaisonSocial().isEmpty())
            throw new OwomiException(i18n.internationalize("create.corporate.empty.raisonSocial.short.message"), ErrorCodeEnums.EMPTY_RAISON_SOCIAL_VALUE_ERROR, i18n.internationalize("create.corporate.empty.raisonSocial.long.message"));
        if (checkCorporateLibelle(requestContext, partenaire, createCorporateWs.getRaisonSocial()))
            throw new OwomiException("", ErrorCodeEnums.CORPORATE_NAME_EXIST_ERROR, i18n.internationalize("create.corporate.empty.raisonSocial.used"));
        Habilete habilete = !habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).isEmpty() ? habileteRepository.findByLibelle(HabileteEnums.PROPRIETAIRE.getLibelle()).get(0) : null;
        NatureCorporate natureCorp = natureCorporateRepository.findOneByLibelle(createCorporateWs.getNatureCorporate());

        if(!type.equals("none")) {
            List<PartenaireCorporate> partenaireCorporates = partenaireCorporateRepository.findAllByPk_Partenaire(partenaire);
            Boolean value = partenaireCorporates.stream().anyMatch(partenaireCorporate -> partenaireCorporate.getCorporate().getLibelle().startsWith(type));
            if(value.equals(true)) throw new OwomiException(i18n.internationalize("create.corporate.empty.doubloon.of.nature"), ErrorCodeEnums.EMPTY_RAISON_DOUBLOON_OF_NATURE_ERROR, i18n.internationalize("create.corporate.empty.doubloon.of.nature"));
        }
        Corporate corporate = new Corporate();
        corporate.setLibelle(type.equals("none") ? createCorporateWs.getRaisonSocial() : type + "-" + createCorporateWs.getRaisonSocial());
        corporate.setTypeCorporate(typeCorporate);
        corporate.setStatut(Boolean.FALSE);
        corporate.setMatricule((partenaire.getPays().getIndicatif() + MobileUtils.generateCompteNumber(6)).replace("-", ""));
        corporate.setNumeroCompte(createPlanComptable(requestContext, createCorporateWs.getRaisonSocial()));
        corporate.setTelephone(createCorporateWs.getTelephone());
        corporate.setIfu(createCorporateWs.getIfu());
        corporate.setFormeJuridique(formeJuridique);
        corporate.setActif(Boolean.TRUE);
        corporate.setAutorise(Boolean.FALSE);
        corporate.setCdp(Boolean.FALSE);
        corporate.setMasterAllowed(Boolean.FALSE);
        corporate.setAgentAllowed(Boolean.FALSE);
        corporate.setNatureCorporate(natureCorp);
        corporate = corporateRepository.save(corporate);
        PartenaireCorporate partenaireCorporate = new PartenaireCorporate();
        partenaireCorporate.setPartenaire(partenaire);
        partenaireCorporate.setCorporate(corporate);
        partenaireCorporate.setHabilete(habilete);
        partenaireCorporate.setCurrent(Boolean.FALSE);
        partenaireCorporate.setActif(Boolean.TRUE);
        partenaireCorporate.setOwner(Boolean.TRUE);
        partenaireCorporateRepository.save(partenaireCorporate);
//        sendInValidation(requestContext,corporate);
        createCorporateWs.setId(corporate.getId());
        return new CreateCorporateResponseWs(WsOperationResponseStatus.success, i18n.internationalize("create.corporate.success"), createCorporateWs, new SoldeWs(0.0, null, null));
    }

    private PlanComptable createPlanComptable(RequestContextWs requestContext, String libelle){
        PlanComptableBase planComptableBase = planComptableBaseRepository.findByLibelle("Clients").get(0);
        String number = MobileUtils.generateCompteNumberEightDigits(4111);
        PlanComptable  planComptable = new PlanComptable();
        planComptable.setLibelle(libelle);
        planComptable.setNumero(planComptableBase.getNumero()+number);
        planComptable.setParentbase(planComptableBase);
        planComptable.setDatecreation(new Date());
        planComptable = planComptableRepository.save(planComptable);
        return planComptable;
    }

    @Transactional
    public AddPartenaireToCorporateResponseWs addPartenaireToCorporate(RequestContextWs requestContext,int id, PartenaireCorporateWs pcWs) throws OwomiException, DataNotFoundException {
		List<PartenaireCorporateWs> failedPartenaireCorporateWs = new ArrayList<>();
		Corporate corporate = corporateRepository.findOneById(id);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, id);
        if (!corporate.isStatut()) throw new OwomiException(i18n.internationalize("add.partenaire.inactive.short.message"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("add.partenaire.inactive.long.message", corporate.getMatricule()));
        Partenaire partenaire = pcWs.getEmail() == null ? partenaireRepository.findOneByTelephone(pcWs.getTelephone()) : partenaireRepository.findOneByEmail(pcWs.getEmail());
        EntityUtils.checkIfEntityIsNull(Partenaire.class, partenaire, "email ou telephone", pcWs.getEmail()==null?pcWs.getTelephone():pcWs.getEmail());
        HabileteEnums habileteEnums = checkHabilete(requestContext,pcWs);
        if(habileteEnums == null) failedPartenaireCorporateWs.add(pcWs);
        List<Habilete> habileteList = habileteRepository.findByLibelle(habileteEnums.getLibelle());
        Habilete habilete=habileteList.get(0);
        PartenaireCorporate partenaireCorporate = new PartenaireCorporate();
        partenaireCorporate.setPartenaire(partenaire);
        partenaireCorporate.setCorporate(corporate);
        partenaireCorporate.setHabilete(habilete);
        partenaireCorporate.setCurrent(Boolean.FALSE);
        partenaireCorporate.setActif(Boolean.FALSE);
        partenaireCorporateRepository.save(partenaireCorporate);
        return addPartenaireToCorporateResponse(requestContext,failedPartenaireCorporateWs, partenaire);
    }

    private AddPartenaireToCorporateResponseWs addPartenaireToCorporateResponse(RequestContextWs requestContext,List<PartenaireCorporateWs> failedPartenaireCorporateWs, Partenaire partenaire){
        AddPartenaireToCorporateResponseWs response = new AddPartenaireToCorporateResponseWs();
        if(failedPartenaireCorporateWs.isEmpty()){
            PartenaireWs partenaireWs = mapper.map(partenaire, PartenaireWs.class);
            response.setStatus(WsOperationResponseStatus.success);
            response.setMessage(i18n.internationalize("add.partenaire.to.corporate.success"));
            response.setPartenaire(partenaireWs);
        }
        else{
            response.setStatus(WsOperationResponseStatus.error);
            response.setMessage(ErrorCodeEnums.INVALID_HABILETE.name());
            response.setCode(ErrorCodeEnums.INVALID_HABILETE.getErrorCode());
            response.setData(failedPartenaireCorporateWs);
        }
        return response;
    }

    @Transactional
    public void activedInvitations(RequestContextWs requestContext,Partenaire partenaire, int idCorporate) throws DataNotFoundException {
        List<PartenaireCorporate> partenaireCorporateList = partenaire.getPartenaireCorporates();
        PartenaireCorporate partenaireCorporate = partenaireCorporateList.stream().filter(item-> item.getCorporate().getId() == idCorporate).findFirst().orElse(null);
        EntityUtils.checkIfEntityIsNull(PartenaireCorporate.class, partenaireCorporate, idCorporate);
        partenaireCorporate.setActif(Boolean.TRUE);
        partenaireCorporateRepository.save(partenaireCorporate);
    }

    @Transactional
    public List<FormeJuridiqueWs> listFormeJuridique(RequestContextWs requestContext, Partenaire partenaire){
        Pays inter = paysRepository.findOneByCode("INTER");
        List<FormeJuridique> formeJuridiqueList = formeJuridiqueRepository.findByPays(partenaire.getPays());
        if (formeJuridiqueList.isEmpty()) formeJuridiqueList = formeJuridiqueRepository.findByPays(inter);
        List<FormeJuridiqueWs> formeJuridiqueWsList = new ArrayList<>();
        if(CollectionUtils.checkIfCollectionIsNullOrEmpty(formeJuridiqueList))
            return formeJuridiqueWsList;
        for (FormeJuridique formeJuridique:formeJuridiqueList){
            FormeJuridiqueWs formeJuridiqueWs = mapper.map(formeJuridique, FormeJuridiqueWs.class);
            formeJuridiqueWsList.add(formeJuridiqueWs);
        }
        return formeJuridiqueWsList;
    }

    @Transactional
    public CommonResponseWs associateFormeJuridiqueToTypeDocument(RequestContextWs requestContextWs, Integer idTypeDocument, Integer idFormeJuridique){
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        FormeJuridiqueTypeDocument formeJuridiqueTypeDocument = new FormeJuridiqueTypeDocument();
        formeJuridiqueTypeDocument.setTypeDocument(idTypeDocument);
        formeJuridiqueTypeDocument.setFormeJuridique(idFormeJuridique);
        formeJuridiqueTypeDodumentRepository.save(formeJuridiqueTypeDocument);
        commonResponseWs.setMessage("Association réussit");
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setCode(200);
        return commonResponseWs;
    }

    @Transactional
    public CommonResponseWs getTypeDocumentByFormeJuridique(RequestContextWs requestContextWs, Integer idFormeJuridique){
//        CommonResponseWs commonResponseWs = new CommonResponseWs();
//        List<TypeDocumentWs> typeDocumentWsList = new ArrayList<>();
//        List<FormeJuridiqueTypeDocument> formeJuridiqueTypeDocuments = formeJuridiqueTypeDodumentRepository.findByFormeJuridique(idFormeJuridique);
//        for (FormeJuridiqueTypeDocument formeJuridiqueTypeDocument : formeJuridiqueTypeDocuments){
//            Integer typeDocumentId = formeJuridiqueTypeDocument.getTypeDocument();
//            TypeDocument typeDocument = typeDocumentRepository.findOneById(typeDocumentId);
//            TypeDocumentWs typeDocumentWs = mapper.map(typeDocument, TypeDocumentWs.class);
//            typeDocumentWsList.add(typeDocumentWs);
//        }

        CommonResponseWs commonResponseWs = new CommonResponseWs();

        List<TypeDocumentWs> typeDocumentWsList = new ArrayList<>();
        List<FormeJuridiqueTypeDocument> listFormeJuridiqueTypeDocuments = formeJuridiqueTypeDodumentRepository.findByFormeJuridique(idFormeJuridique);

        if (!CollectionUtils.checkIfCollectionIsNullOrEmpty(listFormeJuridiqueTypeDocuments)) {
            List<Integer> listTypeDocuments = new ArrayList<>();
            for (FormeJuridiqueTypeDocument f : listFormeJuridiqueTypeDocuments) {
                listTypeDocuments.add(f.getTypeDocument());
            }

            List<TypeDocument> typeDocumentList = (List<TypeDocument>) typeDocumentRepository.findAllById(listTypeDocuments);
            for (TypeDocument typeDocument: typeDocumentList) {
                typeDocumentWsList.add(mapper.map(typeDocument, TypeDocumentWs.class));
            }
        }

        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage("typedoc.by.formejuridique");
        commonResponseWs.setCode(200);
        commonResponseWs.setData(typeDocumentWsList);

        return commonResponseWs;

    }

    @Transactional
    public List<TypeDocumentWs> listTypeDocument(RequestContextWs requestContext){
        List<TypeDocument> typeDocumentList = typeDocumentRepository.findAll();
        List<TypeDocumentWs> typeDocumentWsList = new ArrayList<>();
        if(CollectionUtils.checkIfCollectionIsNullOrEmpty(typeDocumentList)) return typeDocumentWsList;
        for (TypeDocument typeDocument:typeDocumentList){
            TypeDocumentWs typeDocumentWs = mapper.map(typeDocument, TypeDocumentWs.class);
            typeDocumentWsList.add(typeDocumentWs);
        }
        return typeDocumentWsList;
    }

    @Transactional
    public CommonResponseWs uploadDocument(RequestContextWs requestContext, CorporateDocumentWs corporateDocumentWs) throws FileUploadException, DataNotFoundException, OwomiException, IOException {
		Corporate corporate = corporateRepository.findOneById(corporateDocumentWs.getCorporate());
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, corporateDocumentWs.getCorporate());
        if (corporate.isStatut()) throw new OwomiException("Corporate déja validé",ErrorCodeEnums.CORPORATE_IS_ALREADY_ACTIVE,"Désolé, mais votre corporate est déja activé");
        TypeDocumentEnums typeDocumentEnums = TypeDocumentEnums.getTypeDocumentEnums(corporateDocumentWs.getTypeDocument());
        TypeDocument typeDocument = typeDocumentRepository.findByLibelle(typeDocumentEnums.getLibelle());
        EntityUtils.checkIfEntityIsNull(TypeDocument.class, typeDocument, "libelle",typeDocumentEnums.getLibelle());
//        if (documentRepository.findByCorporateAndTypeDocument(corporate,typeDocument) != null) throw new OwomiException("Document existant", ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, "Désolé vous avez deja fournis ce type de document ");
        Document document = documentRepository.findByCorporateAndTypeDocument(corporate,typeDocument);
        if (document != null) {
            if (!document.getStatus().equals(DocumentStatusEnums.VALIDATED.getStatus())) {
                updateDocument(requestContext, corporateDocumentWs.getFile(), corporate, typeDocument, document);
            } else {
                throw new OwomiException("Document déjà validé", ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, "Désolé ! Ce document ne peut être modifié puisqu'il a déjà été validé");
            }
        } else {
            upload(requestContext, corporateDocumentWs.getFile(), corporate, typeDocument);
        }

        //Vérifier si tous les docs ont été fournis
        if (this.checkIfIsSendableInValidation(corporate)) {
            sendInValidation(requestContext, corporate);
            documentRepository.findByCorporate(corporate).forEach(doc -> {
                doc.setStatus(DocumentStatusEnums.IN_PROGRESS.getStatus());
                documentRepository.save(doc);
            });
        }

        CommonResponseWs response = new CommonResponseWs();
        response.setMessage("upload.doc.mes.sucess");
        response.setStatus(WsOperationResponseStatus.success);
        return response;
    }

    private boolean checkIfIsSendableInValidation(Corporate corporate) {
        // check if corporate is already in validation
        Validation validation = validationRepository.findByIdrefAndReference(corporate.getId(), Corporate.class.getSimpleName());
        if (validation != null) { // si déjà en validatiton
            return false;
        }
        // check if all corporate's docs are provided
        List<FormeJuridiqueTypeDocument> typeDocumentList = formeJuridiqueTypeDodumentRepository.findByFormeJuridique(corporate.getFormeJuridique().getId());
        if (typeDocumentList == null) {
            return false;
        } else if (typeDocumentList.isEmpty()) {
            return true;
        } else {
            List<Document> documentList = documentRepository.findByCorporate(corporate);
            if (CollectionUtils.checkIfCollectionIsNullOrEmpty(documentList)) {
                return false;
            } else if (typeDocumentList.size() == documentList.size()) {
                return true;
            } else return false;
        }
    }


    private void upload(RequestContextWs requestContext, MultipartFile file, Corporate corporate, TypeDocument typeDocument) throws FileUploadException, IOException {
        String url = fileService.storeFile(file);
        Document document = new Document();
        document.setUrl(url);
        document.setCorporate(corporate);
        document.setFormeJuridique(corporate.getFormeJuridique());
        document.setTypeDocument(typeDocument);
        document.setStatus(DocumentStatusEnums.NOT_IN_VALIDATION.getStatus());
        Document test = documentRepository.save(document);
    }

    private void updateDocument(RequestContextWs requestContext, MultipartFile file, Corporate corporate, TypeDocument typeDocument, Document document) throws FileUploadException, IOException {
        String url = fileService.storeFile(file);
        document.setUrl(url);
        document.setCorporate(corporate);
        document.setFormeJuridique(corporate.getFormeJuridique());
        document.setTypeDocument(typeDocument);
        document.setStatus(DocumentStatusEnums.NOT_IN_VALIDATION.getStatus());
        Document test = documentRepository.save(document);
    }

    @Transactional
    public List<CorporateWs> getCorporate(
            RequestContextWs requestContext, Partenaire partenaire, Boolean certified
    ) throws DataNotFoundException {
        List<PartenaireCorporate> partenaireCorporateList = partenaire.getPartenaireCorporates();
//        partenaireCorporateList.stream().filter(t-> t.getCorporate().isVerifier()).collect(Collectors.toList());
        List<String> types = Arrays.asList(
                AbonneGroupeTypeEnums.PROFESSIONAL_GROUP.getValue(), AbonneGroupeTypeEnums.PROFESSIONAL_GROUP.getValue()
        );
        partenaireCorporateList = partenaireCorporateList.stream()
                .filter(v -> !types.contains(v.getCorporate().getTypeCorporate().getLibelle()))
                .filter(v -> !v.getCorporate().getLibelle().startsWith("GROUPE SOCIALE SOLIDAIRE"))
                .filter(v -> !v.getCorporate().getLibelle().startsWith("GROUPE PROFESSIONNEL"))
                .collect(Collectors.toList());
        List<CorporateWs> corporateWsList = new ArrayList<>();
        for (PartenaireCorporate partenaireCorporate : partenaireCorporateList) {
            Corporate corporate = partenaireCorporate.getCorporate();
            Boolean provisoire = corporate.getProvisoire() != null && corporate.getProvisoire();
            Boolean noNumber = partenaire.getConnectedSansNumero() != null && partenaire.getConnectedSansNumero();
            if (certified && (provisoire || noNumber)) continue;
            CorporateWs corporateWs = new CorporateWs();
            corporateWs.setProvisoire(provisoire);
            corporateWs.setConnectedSansNumero(noNumber);
            corporateWs.setStatut(partenaireCorporate.isActif());
            corporateWs.setMatricule(corporate.getMatricule());
            corporateWs.setLibelle(corporate.getLibelle());
            corporateWs.setId(corporate.getId());
            corporateWs.setName(partenaire.getNom());
            corporateWs.setFirstName(partenaire.getPrenoms());
            corporateWs.setSolde(transactionFinanciereService.extraitCompte(requestContext, corporate.getMatricule()));
            corporateWs.setSoldeDisponible(transactionFinanciereService.soldeDisponible(corporate));
            corporateWs.setStatutCorporate(corporate.isStatut());
            if (provisoire) corporateWs.setStatutCorporate(false);
            corporateWs.setOwner(partenaireCorporate.isOwner() == null ? Boolean.FALSE : partenaireCorporate.isOwner());
            corporateWs.setNature(corporate.getNatureCorporate().getLibelle());
            corporateWs.setTelephone(corporate.getTelephone() == null ? partenaire.getTelephone() : corporate.getTelephone());
            corporateWs.setIfu(corporate.getIfu());
            corporateWs.setFormeJuridique(corporate.getFormeJuridique() != null ? corporate.getFormeJuridique().getLibelle() : "");
            corporateWs.setDefault(corporate.getMatricule().equals(partenaire.getTelephone()) ? Boolean.TRUE : Boolean.FALSE);
            corporateWs.setCodePays(partenaire.getPays().getIndicatif());
            corporateWs.setDevise(mapper.map(partenaire.getPays().getDevise(), DeviseWs.class));
            corporateWs.setEtapeInscription(partenaire.getEtapeInscriptionReseau());
            corporateWs.setPhotoProfil(partenaire.getPhotoProfil());
            corporateWs.setBonusAccount(partenaireBonusAccountService.solde(partenaire));
            corporateWs.setKycValidator(partenaire.getKycValidator() != null && partenaire.getKycValidator());
            corporateWsList.add(corporateWs);
        }
        corporateWsList = corporateWsList.stream()
                .sorted(Comparator.comparing(v -> v.isDefault() ? 1 : 2))
                .collect(Collectors.toList());
        logger.debug("Check Partenaire community => " + partenaire.getId());
        logger.debug("Check Partenaire community => " + Optional.ofNullable(partenaire.getParent()).map(Partenaire::getId).orElse(null));
        this.futureCorporates(partenaire.getId(), certified);
		logger.debug("auto integration community");
		corporateWsList = corporateWsList.stream().sorted(Comparator.comparing(CorporateWs::getId)).collect(Collectors.toList());
		return corporateWsList;
	}

    private void futureCorporates(Integer idPartenaire, Boolean certified) {
        if (!certified) return;
        CompletableFuture.runAsync(() -> {
            Partenaire partenaire = this.partenaireRepository.findOneById(idPartenaire);
            if (partenaire == null) return;
            if (partenaire.getParent() == null) {
                try {
                    CommonResponseWs commonResponseWs = networkToolClient.getChefCommunity(partenaire.getEmail(), httpServletRequest.getHeader("access-token"));
                    if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) {
                        PartenaireWs partenaireWs = new Gson().fromJson(new Gson().toJson(commonResponseWs.getData()), PartenaireWs.class);
                        logger.debug("Partenaire ===> " + partenaireWs);
                        networkToolClient.joinCommunity(partenaireWs.getId(), httpServletRequest.getHeader("access-token"));
                    }
                } catch (Exception e) {
                    //mise en forme derniere page d'URL redirect
                    e.printStackTrace();
                }
            }
            try {
                this.rechargeService.updateRecharges(partenaire);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Transactional
    public CommonResponseWs getCorporateValider(RequestContextWs requestContext,Partenaire partenaire) throws DataNotFoundException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        List<PartenaireCorporate> partenaireCorporateList = partenaire.getPartenaireCorporates();
        List<CorporateWs> corporateWsList = new ArrayList<>();
        List<PartenaireCorporate> partenaireCorporateValideList = partenaireCorporateList.stream().filter(t-> t.getCorporate().isStatut()).collect(Collectors.toList());
        for (PartenaireCorporate partenaireCorporate : partenaireCorporateValideList){
                Corporate corporate = partenaireCorporate.getCorporate();
                CorporateWs corporateWs = new CorporateWs();
                corporateWs.setStatut(partenaireCorporate.isActif());
                corporateWs.setMatricule(corporate.getMatricule());
                corporateWs.setLibelle(corporate.getLibelle());
                corporateWs.setId(corporate.getId());
                corporateWs.setName(partenaire.getNom());
                corporateWs.setFirstName(partenaire.getPrenoms());
                corporateWs.setSolde(transactionFinanciereService.extraitCompte(requestContext, corporate.getMatricule()));
                corporateWs.setStatutCorporate(corporate.isStatut());
                corporateWs.setOwner(partenaireCorporate.isOwner() == null ? Boolean.FALSE : partenaireCorporate.isOwner());
                corporateWs.setNature(corporate.getNatureCorporate().getLibelle());
                corporateWs.setTelephone(corporate.getTelephone() == null ? partenaire.getTelephone() : corporate.getTelephone());
                corporateWs.setIfu(corporate.getIfu());
                corporateWs.setFormeJuridique(corporate.getFormeJuridique() != null ? corporate.getFormeJuridique().getLibelle() : "");
                corporateWs.setDefault(corporate.getMatricule().equals(partenaire.getTelephone()) ? Boolean.TRUE : Boolean.FALSE);
                corporateWs.setCodePays(partenaire.getPays().getIndicatif());
                corporateWs.setDevise(mapper.map(partenaire.getPays().getDevise(), DeviseWs.class));
                corporateWsList.add(corporateWs);

        }
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setCode(200);
        commonResponseWs.setMessage("Success");
        commonResponseWs.setData(corporateWsList);
        return commonResponseWs;
    }

    private HabileteEnums checkHabilete(RequestContextWs requestContext, PartenaireCorporateWs partenaireCorporateWs){
        return Arrays.asList(HabileteEnums.values()).stream().filter(item->item.getPriority() == partenaireCorporateWs.getHabilete()).findFirst().orElse(null);
    }

    @Transactional
    public List<HabileteWs> getHabileteList(RequestContextWs requestContext) {
        List<Habilete> habileteList = habileteRepository.findAll();
        List<HabileteWs> habileteWsList = new ArrayList<>();
        for(Habilete habilete : habileteList){
            HabileteWs habileteWs = new HabileteWs();
            habileteWs.setId(habilete.getId());
            habileteWs.setLibelle(habilete.getLibelle());
            habileteWs.setPriorite(habilete.getPriorite());
            habileteWsList.add(habileteWs);
        }
        return habileteWsList;
    }

    @Transactional
    public List<PartenaireWs> getCorporateMembers(RequestContextWs requestContext,Integer idCorporate) throws DataNotFoundException {
		Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        List<PartenaireCorporate> partenaireCorporateList = corporate.getPartenaireCorporates();
        List<PartenaireWs> partenaireWsList = new ArrayList<>();
        for (PartenaireCorporate partenaireCorporate:partenaireCorporateList) {
            PartenaireWs partenaireWs = mapper.map(partenaireCorporate.getPartenaire(), PartenaireWs.class);
            partenaireWsList.add(partenaireWs);
        }
        return partenaireWsList;
    }

    private void sendInValidation(RequestContextWs requestContext, Corporate corporate){
        if(corporate.isStatut()) return;
        List<Validation> list = validationRepository.findByTablerefAndIdrefAndTicketvalidationAndStatutBetween(Corporate.class.getSimpleName(), corporate.getId(), "genere", 0, 2);
        if(!list.isEmpty()) return;
        Validation validation = new Validation();
        validation.setReference(Corporate.class.getSimpleName());
        validation.setReferenceId(corporate.getId());
        validation.setTableref(Corporate.class.getSimpleName());
        validation.setIdref(corporate.getId());
        validation.setStatut(0);
        validation.setNature(i18n.internationalize("send.in.validation.nature", Corporate.class.getSimpleName()));
        validation.setTicketvalidation("genere");
        validation.setDateEnvoi(new Timestamp(System.currentTimeMillis()));
        validation.setMotifreceveur(corporate.getLibelle());
        validation = validationRepository.save(validation);
        PartenaireCorporate partenaireCorporate = partenaireCorporateRepository.findOneByPk_Corporate(corporate);
        Partenaire partenaire = partenaireCorporate.getPartenaire();
        this.validationService.inSendValidation(partenaire, validation);
    }

    @Transactional
    public CommonResponseWs setCorporatePrestashopId(RequestContextWs requestContext, Partenaire partenaire, Integer id){
       List<PartenaireCorporate> partenaireCorporateList = partenaire.getPartenaireCorporates().stream().filter(partenaireCorporate -> partenaireCorporate.isOwner() == Boolean.TRUE).collect(Collectors.toList());
       for (PartenaireCorporate target : partenaireCorporateList) {
           Corporate corporate = target.getCorporate();
           corporate.setIdPrestashop(id);
           corporateRepository.save(corporate); }
        return new CommonResponseWs(WsOperationResponseStatus.success, null, null, i18n.internationalize("set.corporate.prestashop.id.success"), null, null,null);
    }

    @Transactional
    public CorporateResponseWs getCorporateIdPrestashop(RequestContextWs requestContext,Partenaire partenaire) {
        return partenaire.getPartenaireCorporates().stream()
                .filter(partenaireCorporate -> partenaireCorporate.isOwner() == Boolean.TRUE)
                .findFirst()
                .map(partenaireCorporate -> new CorporateResponseWs(partenaireCorporate.getCorporate().getIdPrestashop()))
                .get();
    }

    private void setCoporateIdPrestashop(RequestContextWs requestContext,Integer coporateIdPrestashop ,Corporate corporate){
        corporate.setIdPrestashop(coporateIdPrestashop);
        corporateRepository.save(corporate);
    }

    @Transactional
    public List<CheckDocumentWs> checkDocuments(RequestContextWs requestContext,Integer idCorporate){
		Corporate corporate = corporateRepository.findOneById(idCorporate);
		List<CheckDocumentWs> checkDocumentWsList = new ArrayList<>();
        List<TypeDocument> typeDocumentList = new ArrayList<>();
        List<FormeJuridiqueTypeDocument> formeJuridiqueTypeDocuments = formeJuridiqueTypeDodumentRepository.findByFormeJuridique(corporate.getFormeJuridique().getId());

        formeJuridiqueTypeDocuments.forEach(formeJuridiqueTypeDocument -> {
			TypeDocument typeDocument = typeDocumentRepository.findOneById(formeJuridiqueTypeDocument.getTypeDocument());
            typeDocumentList.add(typeDocument);
        });
        List<Document> documentList = documentRepository.findByCorporate(corporate);
        for (TypeDocument typeDocument : typeDocumentList) {
            CheckDocumentWs checkDocumentWs = new CheckDocumentWs();
            checkDocumentWs.setDocument(typeDocument.getLibelle());
            checkDocumentWs.setAbrev(typeDocument.getAbrev());
            checkDocumentWs.setStatus(Boolean.FALSE);
            for (Document target : documentList) {
                if (checkDocumentWs.getDocument().equals(target.getTypeDocument().getLibelle())) {
                    checkDocumentWs.setStatus(Boolean.TRUE);
                    checkDocumentWs.setStatusDocument(target.getStatus());
                    checkDocumentWs.setCommentaires(target.getCommentaire());
                    checkDocumentWs.setUrl(target.getUrl());
                }

            }
            checkDocumentWsList.add(checkDocumentWs);
        }
        return checkDocumentWsList;
    }

    @Transactional
    public CommonResponseWs payKitDemarrageAgent(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate) throws OwomiException, DataNotFoundException {
        return payProcessing(requestContext,partenaire, idCorporate, productsInfos.getKitDemarrageAgent());
    }

    @Transactional
    public CommonResponseWs payKitDemarrageMarchand(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate, Integer typeKit) throws OwomiException, DataNotFoundException {
        //String produitMarchandName = productsInfos.getKitDemarrageMarchand()+" "+typeKit.toString();
        //return payProcessing(requestContext,partenaire, idCorporate, produitMarchandName);
        return new CommonResponseWs(WsOperationResponseStatus.failed,"SVP VEUILLEZ EFFECTUER L'ACHAT SUR L'APPLICATION MARCHAND !!!",null,null);
    }

    @Transactional
    public CommonResponseWs payKitDemarrageMarchand(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate, String nomProduit, Integer corporate) throws OwomiException, DataNotFoundException {
        return payProcessingv2(requestContext,partenaire, idCorporate, nomProduit,corporate);
    }

    public CommonResponseWs getProductMerchantList(RequestContextWs requestContextWs){
        List<Produit> produitList = produitRepository.findByNomContains(productsInfos.getKitDemarrageMarchand());
        List<ProductInfosWs> productInfosWsList = produitList.stream().map(produit -> {
            ProductInfosWs productInfosWs = new ProductInfosWs();
            productInfosWs.setId(produit.getId());
            productInfosWs.setNom(produit.getNom());
            ProduitPays produitPays = produitPaysRepository.findOneByProduit(produit);
            ConfigProduit configProduit = configProduitRepository.findOneByProduitpays(produitPays);
            productInfosWs.setMontant(configProduit.getPrix());
            return productInfosWs;
        }).collect(Collectors.toList());
        return new CommonResponseWs(WsOperationResponseStatus.success,"",200,productInfosWsList);
    }


    @Transactional
    public List<NatureCorporateWs> getNatureCorporate(RequestContextWs requestContext) {
        List<NatureCorporateWs> natureCorporateWsList = new ArrayList<>();
        List<NatureCorporate> natureCorporateList = natureCorporateRepository.findAll();
        for (NatureCorporate target : natureCorporateList) {
           if (target.getLibelle().equals(AGENT) || target.getLibelle().equals(MARCHAND)){
               NatureCorporateWs natureCorporateWs = new NatureCorporateWs();
               natureCorporateWs.setId(target.getId());
               natureCorporateWs.setLibelle(target.getLibelle());
               natureCorporateWsList.add(natureCorporateWs);
           }

        }
        return natureCorporateWsList;
    }
    @Transactional
    public CommonResponseWs deleteDocument(RequestContextWs requestContext,Integer idCorporate, String abreviation) {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        TypeDocumentEnums typeDocumentEnums = TypeDocumentEnums.getTypeDocumentEnums(abreviation);
        TypeDocument typeDocument = typeDocumentRepository.findByLibelle(typeDocumentEnums.getLibelle());
        Document documentList = documentRepository.findByCorporateAndTypeDocument(corporate,typeDocument);
        documentRepository.delete(documentList);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage(i18n.internationalize("delete.document.message"));
        return commonResponseWs;
    }
    @Transactional
    public CommonResponseWs checkPiece(RequestContextWs requestContext,Partenaire partenaire) {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        if (partenaire.getPiece() != null && partenaire.getPiecePhoto() != null) {
            commonResponseWs.setStatus(WsOperationResponseStatus.success);
            commonResponseWs.setMessage(i18n.internationalize("check.piece.message"));
        }else {
            commonResponseWs.setStatus(WsOperationResponseStatus.failed);
        }
        return commonResponseWs;
    }
    //Methode interne setCorporateNature
    //Elle permet le passage de
    @Transactional
    public CommonResponseWs traitementForBecomeAgent(RequestContextWs requestContext,Partenaire partenaire, AgentWs agentWs) throws OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Corporate corporate = corporateRepository.findOneById(agentWs.getIdCorporate());
        TypeDocument iFu = typeDocumentRepository.findByLibelle(TypeDocumentEnums.IFU.getLibelle());
        checkAgentException(requestContext,corporate,partenaire,agentWs,iFu);
        NatureCorporate natureCorporate = natureCorporateRepository.findOneByLibelle("Agent");
        corporate.setNatureCorporate(natureCorporate);
        corporate.setCdp(Boolean.TRUE);
        corporateRepository.save(corporate);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage("Félicitations, vous etes désormais Agent à owomi. Enjoy !!!");
        return commonResponseWs;
    }


    @Transactional
    public CommonResponseWs traitementForBecomeMaster(RequestContextWs requestContext,Partenaire partenaire, AgentWs agentWs) throws OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Corporate corporate = corporateRepository.findOneById(agentWs.getIdCorporate());
        TypeDocument iFu = typeDocumentRepository.findByLibelle(TypeDocumentEnums.IFU.getLibelle());
        checkMasterException(requestContext,corporate,partenaire,agentWs,iFu);
        NatureCorporate natureCorporate = natureCorporateRepository.findOneByLibelle("Master");
        corporate.setNatureCorporate(natureCorporate);
        corporate.setCdp(Boolean.TRUE);
        corporateRepository.save(corporate);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage("Félicitations, vous etes désormais Master à owomi. Enjoy !!!");
        return commonResponseWs;
    }

    public AcmSoldeWs acmSolde(Corporate entity) {
        AcmAccountSoldeWs solde = this.acmAccountSolde(entity);
        return Optional.ofNullable(solde).map(AcmAccountSoldeWs::getSolde).orElse(null);
    }

    private AcmAccountSoldeWs acmAccountSolde(Corporate entity) {
        if(entity == null) return null;
        PlanComptable account = entity.getNumeroCompte();
        if(account == null) return null;
        return this.accountMouvementSoldeServiceClient.getAcmAccountSolde(account.getId());
    }

    public CommonResponseWs soldeCorporate(Partenaire partenaire, String matricule) {
        Corporate corporate = this.corporateRepository.findOneByMatricule(matricule);
        if (corporate == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "corporate", 404, null);
        if (!this.isOwner(corporate, partenaire)) return new CommonResponseWs(WsOperationResponseStatus.failed, "owner", 431, null);
        AcmSoldeWs result = this.acmSolde(corporate);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    private Boolean isOwner(Corporate corporate, Partenaire partenaire)  {
        if (partenaire == null || corporate == null) return false;
        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByPk_CorporateAndPk_Partenaire(corporate, partenaire);
        return Optional.ofNullable(pc).map(PartenaireCorporate::isOwner).orElse(false);
    }

    public CommonResponseWs checkMasterAgentAllowed (Partenaire partenaire) throws OwomiException, DataNotFoundException, AuthentificationException {
        List<PartenaireCorporate> list =partenaireCorporateRepository.findAllByPk_Partenaire(partenaire);
        AgentMasterCheckWs agentMasterWs = new AgentMasterCheckWs();
        agentMasterWs.setAgentAllowed(false);
        agentMasterWs.setMasterAllowed(false);
        for (PartenaireCorporate current : list)
        {
            Corporate corporate = current.getCorporate();
            if(corporate.getAgentAllowed())
            {
                agentMasterWs.setAgentAllowed(corporate.getAgentAllowed());
            }
            if(corporate.getMasterAllowed())
            {
                agentMasterWs.setMasterAllowed(corporate.getMasterAllowed());
            }
        }
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setData(agentMasterWs);
        return commonResponseWs;
    }

    private void checkAgentException(RequestContextWs requestContext,Corporate corporate, Partenaire partenaire,AgentWs agentWs,TypeDocument iFu) throws OwomiException {
        if (!partenaire.isIdentifier())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"), ErrorCodeEnums.IDENTIFY_ERROR, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY_MSG"));
        if (validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.AGENT.getNature(), 0) != null || validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.MARCHAND.getNature(), 0) != null)
            throw new OwomiException(i18n.internationalize("check.argent.ex.message1"), ErrorCodeEnums.ALREADY_IN_VALIDATION, i18n.internationalize("check.argent.ex.message2"));
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_NOT_ACTIF_MSG"));
        if(!corporate.getAgentAllowed()) throw new OwomiException("Vous n'etes pas autorisé a etre agent !!!",ErrorCodeEnums.AGENT_NOT_ALLOWED_ERROR,"Vous n'etes pas autorisé a etre agent !!!") ;
        if (partenaire.getTelephone().equals(corporate.getMatricule()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE"), ErrorCodeEnums.DEFAULT_CORPORATE_ERROR, i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE_MSG"));
        if (partenaire.getEtapeInscriptionReseau() != 3)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.NOT_NETWORKER"), ErrorCodeEnums.NOT_NETWORKER_ERROR, i18nCommons.internationalize("MobileConstant.NOT_NETWORKER_MSG"));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_MARCHAND"), ErrorCodeEnums.ALREADY_MARCHAND_ERROR, i18nCommons.internationalize("MobileConstant.ALREADY_NATURE_MSG_WITH_2_REPLACE", NatureCorporateEnums.MARCHAND.getNature(), NatureCorporateEnums.AGENT.getNature()));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.AGENT.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_AGENT"), ErrorCodeEnums.ALREADY_AGENT_ERROR, i18nCommons.internationalize("MobileConstant.ALREADY_NATURE_MSG_WITH_REPLACE", NatureCorporateEnums.AGENT.getNature()));
        /*if (documentRepository.findByCorporateAndTypeDocument(corporate, iFu) == null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.DOCUMENT_NOT_FOUND"), ErrorCodeEnums.DOCUMENT_NOT_FOUND_ERROR, i18nCommons.internationalize("MobileConstant.DOCUMENT_NOT_FOUND_MSG_WITH_REPLACE", TypeDocumentEnums.IFU.getLibelle()));*/
        if (!corporate.isVerifier()) {
            checkNumero(requestContext, agentWs, partenaire, corporate);
        }
        Panier panier = panierRepository.findOneByPartenaire(partenaire);
        ProduitPays kit_de_demarrage_agent = produitPaysRepository.findOneByProduit_Nom(productsInfos.getKitDemarrageAgent());
        String produitPaysSucces = PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.toString();


        if (panierProduitRepository.findByCorporateAndProduitPaysAndPanierAndStatus(corporate, kit_de_demarrage_agent, panier, produitPaysSucces).isEmpty()){
            if(hasPaidCdp(partenaire) && !hasAlreadyCorporateCdp(partenaire)) {
                if(getSolde(corporate) < 100000.0) throw new OwomiException("Vous devez avoir un minimum de 100.000 FCFA sur votre compte !!!",ErrorCodeEnums.MINIMUM_BALANCE_NOT_REACH_ERROR,"Vous devez avoir un minimum de 100.000 FCFA sur votre compte !!!") ;
                  return;
            }
        }throw new OwomiException(i18nCommons.internationalize("MobileConstant.KIT_NOT_PAY"), ErrorCodeEnums.KIT_NO_PAY_ERROR, i18nCommons.internationalize("MobileConstant.KIT_NOT_PAY_MSG_WITH_REPLACE", productsInfos.getKitDemarrageAgent()));
    }

    private void checkMasterException(RequestContextWs requestContext,Corporate corporate, Partenaire partenaire,AgentWs agentWs,TypeDocument iFu) throws OwomiException {
        if (!partenaire.isIdentifier())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"), ErrorCodeEnums.IDENTIFY_ERROR, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY_MSG"));
        if (validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.AGENT.getNature(), 0) != null || validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.MARCHAND.getNature(), 0) != null)
            throw new OwomiException(i18n.internationalize("check.argent.ex.message1"), ErrorCodeEnums.ALREADY_IN_VALIDATION, i18n.internationalize("check.argent.ex.message2"));
        if (!corporate.isStatut())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_INACTIF_ERROR_MSG"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_NOT_ACTIF_MSG"));
        if(!corporate.getMasterAllowed()) throw new OwomiException("Vous n'etes pas autorisé a etre master !!!",ErrorCodeEnums.MASTER_NOT_ALLOWED_ERROR,"Vous n'etes pas autorisé a etre master !!!") ;
        if (partenaire.getTelephone().equals(corporate.getMatricule()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE"), ErrorCodeEnums.DEFAULT_CORPORATE_ERROR, i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE_MSG"));
        if (partenaire.getEtapeInscriptionReseau() != 3)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.NOT_NETWORKER"), ErrorCodeEnums.NOT_NETWORKER_ERROR, i18nCommons.internationalize("MobileConstant.NOT_NETWORKER_MSG"));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_MARCHAND"), ErrorCodeEnums.ALREADY_MARCHAND_ERROR, i18nCommons.internationalize("MobileConstant.ALREADY_NATURE_MSG_WITH_2_REPLACE", NatureCorporateEnums.MARCHAND.getNature(), NatureCorporateEnums.AGENT.getNature()));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.AGENT.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_AGENT"), ErrorCodeEnums.ALREADY_AGENT_ERROR, i18nCommons.internationalize("MobileConstant.ALREADY_NATURE_MSG_WITH_REPLACE", NatureCorporateEnums.AGENT.getNature()));
        /*if (documentRepository.findByCorporateAndTypeDocument(corporate, iFu) == null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.DOCUMENT_NOT_FOUND"), ErrorCodeEnums.DOCUMENT_NOT_FOUND_ERROR, i18nCommons.internationalize("MobileConstant.DOCUMENT_NOT_FOUND_MSG_WITH_REPLACE", TypeDocumentEnums.IFU.getLibelle()));*/
        if (!corporate.isVerifier()) {
            checkNumero(requestContext, agentWs, partenaire, corporate);
        }

        if(!hasPaidCdp(partenaire)) throw new OwomiException("Vous devez avoir payé un CDP", ErrorCodeEnums.HAS_NOT_PAID_CDP_ERROR,"Vous devez avoir payé un CDP");

        if(getSolde(corporate) < 1000000.0) throw new OwomiException("Vous devez avoir un minimum de 1.000.000 FCFA sur votre compte !!!",ErrorCodeEnums.MINIMUM_BALANCE_NOT_REACH_ERROR,"Vous devez avoir un minimum de 1.000.000 FCFA sur votre compte !!!") ;
    }

    private void checkNumero(RequestContextWs requestContext, AgentWs agentWs, Partenaire partenaire, Corporate corporate) throws OwomiException {
        if (agentWs.getNumero1().equals("") || agentWs.getNumero2().equals(""))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.NUMERO_NOT_FOUND"), ErrorCodeEnums.NUMBER_NOT_FOUND, i18nCommons.internationalize("MobileConstant.NUMERO_NOT_FOUND_MSG"));
        if (agentWs.getNumero1().equals(partenaire.getTelephone()) || agentWs.getNumero2().equals(partenaire.getTelephone()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.BAD_NUMERO"), ErrorCodeEnums.PARTENAIRE_NUMBER_ERROR, i18nCommons.internationalize("MobileConstant.BAD_NUMERO_MSG"));
        if (partenaireRepository.findOneByTelephone(agentWs.getNumero1()) == null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_FOUND"), ErrorCodeEnums.PARTENAIRE_NUMBER_NOT_EXIST, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_FOUND_MSG_WITH_REPLACE", agentWs.getNumero1()));
        if (partenaireRepository.findOneByTelephone(agentWs.getNumero2()) == null)
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_FOUND"), ErrorCodeEnums.PARTENAIRE_NUMBER_NOT_EXIST, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_FOUND_MSG_WITH_REPLACE", agentWs.getNumero2()));
        if (!partenaireRepository.findOneByTelephone(agentWs.getNumero1()).isIdentifier())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"), ErrorCodeEnums.IDENTIFY_ERROR, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY_MSG_WITH_REPLACE", agentWs.getNumero1()));
        if (!partenaireRepository.findOneByTelephone(agentWs.getNumero2()).isIdentifier())
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"), ErrorCodeEnums.IDENTIFY_ERROR, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY_MSG_WITH_REPLACE", agentWs.getNumero2()));
        corporate.setVerifier(Boolean.TRUE);
        corporateRepository.save(corporate);
    }

    private Double getSolde(Corporate corporate){
        AccountSolde accountSolde = accountSoldeRepository.findOneByCompte(corporate.getNumeroCompte());
        return accountSolde !=null ? accountSolde.getSolde() : 0.0;
    }
    @Transactional
    public CommonResponseWs isIdentifier(RequestContextWs requestContext,Partenaire partenaire) throws OwomiException {
        CommonResponseWs identifier = new CommonResponseWs();
        CommonResponseWs commonResponseWs = checkPiece(requestContext,partenaire);
        identifier.setStatus(WsOperationResponseStatus.failed);
        identifier.setMessage(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"));

        if(partenaire.getSexe()==null && partenaire.getDatenaissance()==null) throw new OwomiException("vous devez mettre a jour votre sexe et votre date de naissance",ErrorCodeEnums.UPDATE_SEX_AND_BIRTHDAY_ERROR,"vous devez mettre à jour votre sexe et votre date de naissance");

        if (partenaire.isIdentifier()) {
            identifier.setStatus(WsOperationResponseStatus.success);
            identifier.setMessage(PARTENAIRE_NOT_IDENTIFY);
        } else if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) {
//            identifier.setStatus(WsOperationResponseStatus.inProcess);
            List<Validation> validationList = validationRepository.findByIdrefAndTablerefOrderByIdDesc(partenaire.getId(),Partenaire.class.getSimpleName());

            if (!validationList.isEmpty()) {
                if (validationList.get(0).getStatut() == 4)
                    identifier.setStatus(WsOperationResponseStatus.rejected);
                else if (validationList.get(0).getStatut() == 0)
                    identifier.setStatus(WsOperationResponseStatus.inProcess);
            }
        }

        identifier.setMessage(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"));
        return identifier;
    }

    @Transactional
    public CommonResponseWs isIdentify(RequestContextWs requestContext,Partenaire partenaire) throws OwomiException {
        CommonResponseWs identifier = new CommonResponseWs();
        if(partenaire.isIdentifier()){
            identifier.setStatus(WsOperationResponseStatus.success);
            identifier.setMessage(PARTENAIRE_IS_IDENTIFY);
            identifier.setCode("200");
            return identifier;

        }else {
            HumanKycValidation validation = humanKycValidationRepository.findOneByIdPartenaireAndStatus(partenaire.getId(), "PENDING");
            if(validation == null){
                identifier.setStatus(WsOperationResponseStatus.rejected);
                identifier.setMessage(PARTENAIRE_NOT_IDENTIFY);
                identifier.setCode("202");
            }else{
                identifier.setStatus(WsOperationResponseStatus.inProcess);
                identifier.setMessage(PARTENAIRE_NOT_IDENTIFY);
                identifier.setCode("201");
            }
            return identifier;
        }

    }

    @Transactional
    public CommonResponseWs isNotIdentifier(RequestContextWs requestContext,Partenaire partenaire) {
        CommonResponseWs identifier = new CommonResponseWs();
        partenaire.setIdentifier(Boolean.FALSE);
        partenaireRepository.save(partenaire);
        identifier.setStatus(WsOperationResponseStatus.success);
        identifier.setMessage(i18nCommons.internationalize("MobiliConstant.PARTENAIRE_NOT_IDENTIFY"));
        return identifier;
    }

    private void sendInValidationDemande(RequestContextWs requestContext,Corporate corporate, String natureCorporate){
        Validation validation = new Validation();
        validation.setReference(Corporate.class.getSimpleName());
        validation.setReferenceId(corporate.getId());
        validation.setTableref(Corporate.class.getSimpleName());
        validation.setIdref(corporate.getId());
        validation.setNature(i18nCommons.internationalize("MobileConstant.NATURE_CORPORATE_VALIDATION_LIBELLE_WITH_REPLACE", Corporate.class.getSimpleName()));
        validation.setTicketvalidation(natureCorporate);
        validationRepository.save(validation);
    }
    @Transactional
    public CommonResponseWs traitementForBecomeMarchand(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate) throws OwomiException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Corporate corporate =corporateRepository.findOneById(idCorporate);
        TypeDocument typeDocument = typeDocumentRepository.findByLibelle(TypeDocumentEnums.FICHE_DESCRIPTIF_ACTIVITE.getLibelle());
        checkMarchandException(requestContext,corporate,partenaire,typeDocument);
        //sendInValidationDemande(requestContext,corporate,NatureCorporateEnums.MARCHAND.getNature());
        NatureCorporate natureCorporate = natureCorporateRepository.findOneByLibelle("Marchand");
        corporate.setNatureCorporate(natureCorporate);
        corporate.setCdp(Boolean.TRUE);
        corporateRepository.save(corporate);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage(i18nCommons.internationalize("ResponseConstant.CHANGER_NATURE_CORPORATE_SUCCES"));
        return commonResponseWs;
    }

    private void checkMarchandException(RequestContextWs requestContext,Corporate corporate, Partenaire partenaire,TypeDocument typeDocument) throws OwomiException {
        //if (!partenaire.isIdentifier())
         //   throw new OwomiException(i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY"), ErrorCodeEnums.IDENTIFY_ERROR, i18nCommons.internationalize("MobileConstant.PARTENAIRE_NOT_IDENTIFY_MSG"));
        //if (validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.MARCHAND.getNature(), 0) != null || validationRepository.findByIdrefAndTicketvalidationAndStatut(corporate.getId(), NatureCorporateEnums.AGENT.getNature(), 0) != null)
          //  throw new OwomiException(i18n.internationalize("check.marchand.ex.already.validation.message1"), ErrorCodeEnums.ALREADY_IN_VALIDATION, i18n.internationalize("check.marchand.ex.already.validation.message2"));
        //if (!corporate.isStatut())
          //  throw new OwomiException(i18nCommons.internationalize("MobileConstant.CORPORATE_NOT_ACTIF"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18nCommons.internationalize("MobileConstant.CORPORATE_NOT_ACTIF_MSG"));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.MARCHAND.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_MARCHAND"), ErrorCodeEnums.ALREADY_MARCHAND_ERROR, i18n.internationalize("check.marchand.ex.already.marchand.message", NatureCorporateEnums.MARCHAND.getNature()));
        if (corporate.getNatureCorporate().getLibelle().equals(NatureCorporateEnums.AGENT.getNature()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.ALREADY_AGENT"), ErrorCodeEnums.ALREADY_AGENT_ERROR, i18n.internationalize("check.marchand.ex.already.agent.message", NatureCorporateEnums.AGENT.getNature(), NatureCorporateEnums.MARCHAND.getNature()));
        //if (partenaire.getParent()==null)
          //  throw new OwomiException(i18nCommons.internationalize("MobileConstant.NOT_NETWORKER"), ErrorCodeEnums.NOT_NETWORKER_ERROR, i18nCommons.internationalize("MobileConstant.NOT_NETWORKER_MSG"));
        if (partenaire.getTelephone().equals(corporate.getMatricule()))
            throw new OwomiException(i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE"), ErrorCodeEnums.DEFAULT_CORPORATE_ERROR, i18nCommons.internationalize("MobileConstant.DEFAULT_CORPORATE_MSG"));
       // if (documentRepository.findByCorporateAndTypeDocument(corporate, typeDocument) == null)
         //   throw new OwomiException(i18nCommons.internationalize("MobileConstant.DOCUMENT_NOT_FOUND"), ErrorCodeEnums.DOCUMENT_NOT_FOUND_ERROR,  i18n.internationalize("check.marchand.ex.doc.not.found", TypeDocumentEnums.FICHE_DESCRIPTIF_ACTIVITE.getLibelle()));
        Panier panier = panierRepository.findOneByPartenaire(partenaire);
        ProduitPays kit_de_demarrage_marchand = produitPaysRepository.findOneByProduit_Nom(productsInfos.getKitDemarrageMarchand()+" 1");
        String produitPaysSucces = PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.toString();

        if(hasPaidCdp(partenaire) && !hasAlreadyCorporateCdp(partenaire)) return;
        if (panierProduitRepository.findByCorporateAndProduitPaysAndPanierAndStatus(corporate, kit_de_demarrage_marchand, panier, produitPaysSucces).isEmpty()) throw new OwomiException(i18nCommons.internationalize("MobileConstant.KIT_NOT_PAY"), ErrorCodeEnums.KIT_NO_PAY_ERROR, i18n.internationalize("check.marchand.ex.kit.not.pay"));
    }


    public boolean hasPaidCdp(Partenaire partenaire){

        List<AdressePartenaire> adressePartenaireList = adressePartenaireRepository.findByPartenaire(partenaire);
        logger.debug("size of addresseList is " + adressePartenaireList.size());
        logger.debug("ids " + adressePartenaireList);

        if(adressePartenaireList.size()==0) return false;

        List<Adresse> adresseList = adressePartenaireList.stream().map(adressePartenaire -> adressePartenaire.getAdresse()).collect(Collectors.toList());
        boolean value =  commandeRepository.findByIsPaidIsTrueAndAdresseIn(adresseList).stream()
                .map(Commande::getCommandeId)
                .parallel()
                .map(id -> {
                    try {
                        return shopizerServiceClient.getOrder(id).getProducts();
                    } catch (Exception e) {
                        return new ArrayList<ProduitShopizerWs>(0);
                    }
                })
                .flatMap(Collection::stream)
                .anyMatch(product -> (product != null) && (product.getProduct() != null) && Arrays.asList(cdpIds).contains(product.getProduct().getId().toString()));

        logger.debug("value of hasPaid is =====> " + value);
        return value;
    }

    public boolean hasAlreadyCorporateCdp(Partenaire partenaire){

        List<PartenaireCorporate> partenaireCorporateList = partenaireCorporateRepository.findAllByPk_Partenaire(partenaire);
        logger.debug("Partenaire corporates list size is " + partenaireCorporateList.size());
        return partenaireCorporateList.stream().anyMatch(partenaireCorporate -> partenaireCorporate.getCorporate().getCdp());

    }


    private double getSoldeCompte(RequestContextWs requestContext,List<GrandLivre> grandLivreList){
        Double credit = 0.0;
        Double debit = 0.0;
        for (GrandLivre grandLivre : grandLivreList) {
            String sens = grandLivre.getSens();
            switch (sens){
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

    private CommonResponseWs payProcessing(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate, String produitName) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        //if(!corporate.isStatut()) throw new OwomiException(i18n.internationalize("pay.process.corporate.not.active.message1"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("pay.process.corporate.not.active.message2", corporate.getMatricule()));
        ProduitPays produitPays = produitPaysRepository.findOneByProduit_Nom(produitName);
        EntityUtils.checkIfEntityIsNull(Produit.class, produitPays, "Nom", produitName);
        //PanierProduit panierProduit = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.name()).stream().reduce((first, last)->last).orElse(null);
        //if (panierProduit != null && produitName.equalsIgnoreCase(productsInfos.getFraisInscription())) throw new OwomiException(i18n.internationalize("pay.process.adhesion.already.paid.message1"), ErrorCodeEnums.FRAIS_ADHESION_ALREADY_BUY_ERROR, i18n.internationalize("pay.process.adhesion.already.paid.message2"));
        PanierWs panierWs = new PanierWs();
        panierWs.setReference(produitName);
        panierWs.setQuantite(1);
        panierWs.setCode(produitPays.getCode());
        panierWs.setFormString(idCorporate.toString());
        PanierProduit panierProduit = panierService.panierProcess(requestContext,partenaire, panierWs);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        return operationService.stepOneToBuy(requestContext,partenaire, ids, idCorporate,XOF,0);
    }

    private CommonResponseWs payProcessingv2(RequestContextWs requestContext,Partenaire partenaire, Integer idCorporate, String produitName,Integer corporateDebited) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, idCorporate);
        Corporate corporateDebite = corporateRepository.findOneById(corporateDebited);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, corporateDebited);
        //if(!corporate.isStatut()) throw new OwomiException(i18n.internationalize("pay.process.corporate.not.active.message1"), ErrorCodeEnums.CORPORATE_NOT_ACTIVE_ERROR, i18n.internationalize("pay.process.corporate.not.active.message2", corporate.getMatricule()));
        ProduitPays produitPays = produitPaysRepository.findOneByProduit_Nom(produitName);
        EntityUtils.checkIfEntityIsNull(Produit.class, produitPays, "Nom", produitName);
        //PanierProduit panierProduit = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.name()).stream().reduce((first, last)->last).orElse(null);
        //if (panierProduit != null && produitName.equalsIgnoreCase(productsInfos.getFraisInscription())) throw new OwomiException(i18n.internationalize("pay.process.adhesion.already.paid.message1"), ErrorCodeEnums.FRAIS_ADHESION_ALREADY_BUY_ERROR, i18n.internationalize("pay.process.adhesion.already.paid.message2"));
        PanierWs panierWs = new PanierWs();
        panierWs.setReference(produitName);
        panierWs.setQuantite(1);
        panierWs.setCode(produitPays.getCode());
        panierWs.setFormString(idCorporate.toString());
        PanierProduit panierProduit = panierService.panierProcess(requestContext,partenaire, panierWs);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        return operationService.stepOneToBuy(requestContext,partenaire, ids, corporateDebite.getId(),XOF,0);
    }


    private boolean checkCorporateLibelle (RequestContextWs requestContext,Partenaire partenaire,String libelle) {
        boolean result = Boolean.FALSE;
        List<PartenaireCorporate> partenaireCorporateList = partenaire.getPartenaireCorporates();
        for (PartenaireCorporate target : partenaireCorporateList) {
            if (target.getCorporate().getLibelle().equals(libelle)) {
                result = Boolean.TRUE;
                break;
            }
        }
      return result;
    }


    public CorporateWs getCorporateByMatricule(RequestContextWs requestContext, String matricule) throws DataNotFoundException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        EntityUtils.checkIfEntityIsNull(Corporate.class, corporate, "matricule", matricule);
        return mapper.map(corporate, CorporateWs.class);
    }


    public CommonResponseWs deleteAccount(Partenaire partenaire) throws OwomiException {
        if (partenaire == null)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "parent not exist", 404, null);
        AnonymizeRequestWs ws = new AnonymizeRequestWs();
        ws.setFrom(partenaire.getEmail());
        return this.corporateMatriculeServiceClient.anonymize(ws);
    }
}
