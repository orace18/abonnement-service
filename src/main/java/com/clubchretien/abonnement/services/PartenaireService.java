package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.core.partenaire.services.MobilePartenaireEncryptService;
import com.clubchretien.abonnement.firebase.utils.FireBaseUtils;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.validators.PartenaireValidator;
import com.clubchretien.abonnement.ws.CreatePartenaireStepTwoWss;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owomi.commons.constants.MobilePartenaireConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.utils.*;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.authentication.MobilePartenairePinWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.commons.ws.compensation.CompensationUnitaire;
import com.owomi.commons.ws.compensation.GlobalCompensation;
import com.owomi.commons.ws.compensation.StatutCompensation;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import com.owomi.message.publisher.publishers.AchatNeo4jActiveMqPublisher;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.neo4j.PartenaireServiceClient;
import com.owomi.rest.client.network.NetworkToolClient;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.clubchretien.abonnement.constants.MobileConstant.OWOMI_CODE_SUITE_2;
import static com.clubchretien.abonnement.utils.UtilsService.decrypt;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;
import static com.owomi.commons.utils.StringUtils.generateOTP;

@Component
public class PartenaireService implements MobilePartenaireConstant {
    private static Logger logger = LogManager.getLogger(PartenaireService.class);

    private static String[] mtn = {"04", "05", "06", "44", "45", "46", "54", "55", "56", "64", "65", "66", "74", "75", "76", "84", "85", "86", "95" };
    private static String[] orange = {"07", "08", "09", "47", "48", "49", "57", "58", "59", "67", "68", "69", "77", "78", "79", "87", "88", "89", "97", "98"};
    private static String[] moov = {"01", "02", "03", "40", "41", "42", "43", "50", "51", "52", "53", "70", "71", "72"};

    @Autowired
    PaysRepository paysRepository;
    @Autowired
    TerminalRepository terminalRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    PlanComptableRepository planComptableRepository;
    @Autowired
    PartenaireTerminalRepository partenaireTerminalRepository;
    @Autowired
    AuthentificationUtils authentificationUtils;
    @Autowired
    PartenaireValidator partenaireValidator;
    @Autowired
    CorporateService corporateService;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired
    ImageService imageService;
    @Autowired
    MoyenRecPartenaireRepository moyenRecPartenaireRepository;
    @Autowired
    RetraitRepository retraitRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    OperationStringRepository operationStringRepository;
    @Autowired
    CriteresRepository criteresRepository;
    @Autowired
    DeviseRepository deviseRepository;
    @Autowired
    AccountSoldeRepository accountSoldeRepository;
    @Autowired
    GrandLivreRepository grandLivreRepository;
    @Autowired
    AccountServiceClient accountServiceClient;
    @Autowired
    Mapper mapper;
    @Autowired
    VersionOwomiRepository versionOwomiRepository;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    ActivityLogCreator activityLogCreator;
    @Autowired
    PartenaireCorporateRepository partenaireCorporateRepository;
    @Autowired
    SpecialPartenaireRepository specialPartenaireRepository;
    @Autowired
    GradesRepository gradesRepository;
    @Autowired
    PartenaireServiceClient partenaireServiceClient;
    @Autowired
    ShopizerServiceClient shopizerServiceClient;
    @Autowired
    BoutiqueMarchandRepository boutiqueMarchandRepository;
    @Autowired
    CompensationRepository compensationRepository;
    @Autowired
    CommandeRepository commandeRepository;
    @Autowired
    HistoriqueCompensationRepository historiqueCompensationRepository;
    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;
    @Autowired
    AdressePartenaireRepository adressePartenaireRepository;
    @Autowired
    AchatNeo4jActiveMqPublisher achatNeo4jActiveMqPublisher;
    @Autowired
    FileService fileService;
    @Autowired
    StatutCommandeRepository statutCommandeRepository;
    @Autowired
    NetworkToolClient networkToolClient;

    @Autowired
    NotificationService notificationService;
    @Autowired
    PartenaireCodeOTPRepository partenaireCodeOTPRepository;
    @Autowired
    AuthentificationService authentificationService;
    @Autowired
    MobilePartenaireEncryptService mobilePartenaireEncryptService;

    @Transactional
    public CreatePartenaireResponseWs initialSubcribe(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, OwomiException, AuthentificationException, NotificationException {
        logger.debug("createPartenaireStepOneWs ==> " + createPartenaireStepOneWs);
        Pays pays = partenaireValidator.validate(requestContext, createPartenaireStepOneWs);
        Terminal terminal = createTerminal(requestContext, createPartenaireStepOneWs);
        Partenaire partenaire = createPartenaire(requestContext, createPartenaireStepOneWs, pays);
        PartenaireTerminal partenaireTerminal = createPartenaireTerminal(requestContext, createPartenaireStepOneWs, partenaire, terminal);
        /*PartenaireTerminal partenaireTerminal1 = partenaireTerminalRepository.findByPartenaire_TelephoneAndEtat(createPartenaireStepOneWs.getFullPhoneNumber(), 1).stream().reduce((first, last)->last).orElse(null);
        String findPreviousused = null;
        if (partenaireTerminal1 != null) findPreviousused = partenaireTerminal1.getIdunique();
        if (findPreviousused != null && findPreviousused.equals(partenaireTerminal.getIdunique())) findPreviousused = null;*/
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
        logger.debug(" send code verif to user {} on sms code {} ", partenaire.getTelephone(), partenaire.getCodeVerif());
        String sms = OWOMI_CODE + partenaire.getCodeVerif() + OWOMI_CODE_SUITE_2;
        String objet = "Code Verification";
        this.notificationService.sendSms(partenaire, objet, sms);
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        createPartenaireResponseWs.setIdunique(partenaireTerminal.getIdunique());
        if (partenaire.getSubscribed() != null) {
            if (partenaire.getSubscribed() == Boolean.TRUE) {
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_TRUE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.updated);
            } else {
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_FALSE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            }
        }else {
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_NULL_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        }
        return createPartenaireResponseWs;
    }

    @Transactional
    public CreatePartenaireResponseWs initialSubcribeNew(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs) throws  DataNotFoundException, OwomiException, AuthentificationException, WesternSmsException, NotificationException {
        logger.debug("createPartenaireStepOneWs ==> "+createPartenaireStepOneWs);
        Pays pays = partenaireValidator.validate(requestContext, createPartenaireStepOneWs);
        Terminal terminal = createTerminal(requestContext, createPartenaireStepOneWs);
        Partenaire partenaire = createPartenaire(requestContext, createPartenaireStepOneWs, pays);
        PartenaireTerminal partenaireTerminal = createPartenaireTerminal(requestContext, createPartenaireStepOneWs, partenaire, terminal);
        /*PartenaireTerminal partenaireTerminal1 = partenaireTerminalRepository.findByPartenaire_TelephoneAndEtat(createPartenaireStepOneWs.getFullPhoneNumber(), 1).stream().reduce((first, last)->last).orElse(null);
        String findPreviousused = null;
        if (partenaireTerminal1 != null) findPreviousused = partenaireTerminal1.getIdunique();
        if (findPreviousused != null && findPreviousused.equals(partenaireTerminal.getIdunique())) findPreviousused = null;*/
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        createPartenaireResponseWs.setIdunique(partenaireTerminal.getIdunique());
        if(partenaire.getSubscribed() != null){
            if (partenaire.getSubscribed() == Boolean.TRUE){
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_TRUE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.updated);
            }else{
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_FALSE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            }
        }else {
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_NULL_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        }
        return createPartenaireResponseWs;
    }

    @Transactional
    public CreatePartenaireResponseWs initialSubcribeApple(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs) throws  DataNotFoundException, OwomiException, AuthentificationException, WesternSmsException, NotificationException {
        logger.debug("createPartenaireStepOneWs ==> "+createPartenaireStepOneWs);
        Pays pays = paysRepository.findOneByIndicatif(createPartenaireStepOneWs.getIndicatif());
        EntityUtils.checkIfEntityIsNull(Pays.class, pays,"indicatif" ,createPartenaireStepOneWs.getIndicatif());
        Terminal terminal = createTerminal(requestContext, createPartenaireStepOneWs);
        Partenaire partenaire = createPartenaireApple(requestContext, createPartenaireStepOneWs, pays);
        PartenaireTerminal partenaireTerminal = createPartenaireTerminal(requestContext, createPartenaireStepOneWs, partenaire, terminal);
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        createPartenaireResponseWs.setIdunique(partenaireTerminal.getIdunique());
        if(partenaire.getSubscribed() != null){
            if (partenaire.getSubscribed() == Boolean.TRUE){
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_TRUE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.updated);
            }else{
                createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_FALSE_MESSAGE"));
                createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            }
        }else {
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_NULL_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
            notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        }
        return createPartenaireResponseWs;
    }

    @Transactional
    public CreatePartenaireResponseWs subcribeSecondStep(RequestContextWs requestContext,Partenaire partenaire, CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws OwomiException, FileUploadException, IOException {
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        partenaire.setNom(createPartenaireStepTwoWss.getNom());
        partenaire.setPrenoms(createPartenaireStepTwoWss.getPrenoms());
        partenaire.setSexe(createPartenaireStepTwoWss.getSexe());
       if(createPartenaireStepTwoWss.getDatenaissance()!=null) partenaire.setDatenaissance(StringUtils.fromISODate(createPartenaireStepTwoWss.getDatenaissance()));
        partenaireRepository.save(partenaire);
        if (createPartenaireStepTwoWss.getPhoto()!=null) imageService.uploadImage(requestContext,partenaire, createPartenaireStepTwoWss.getPhoto());
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        if(corporate != null) {
            corporate.setLibelle(partenaire.getNom()+" "+partenaire.getPrenoms());
            corporateRepository.save(corporate);
            PlanComptable planComptable = corporate.getNumeroCompte();
            planComptable.setLibelle(partenaire.getNom()+" "+partenaire.getPrenoms());
            planComptableRepository.save(planComptable);
        }
        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SECOND_SUBSCRIBE_SUCCESS_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
        return createPartenaireResponseWs;
    }

    @Transactional
    public CreatePartenaireResponseWs subcribeThirdStep(RequestContextWs requestContext,Partenaire partenaire, CreatePartenaireStepTreeWs createPartenaireStepTreeWs) throws NotificationException {
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        if(createPartenaireStepTreeWs.getPin().length() > 5){
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_BAD_SIZE_PIN_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.error);
            return createPartenaireResponseWs;
        }
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(createPartenaireStepTreeWs.getPin(), salt);
        partenaire.setSalt(salt);
        partenaire.setPin(hashed);
        partenaire.setSecour(createPartenaireStepTreeWs.getSecour());
        corporateService.createDefaultCorporate(requestContext,partenaire);
        partenaire.setSubscribed(true);
        partenaire.setSubcriptionCompleted(Boolean.TRUE);
        partenaire.setDateinscription(new Date());
        partenaireRepository.save(partenaire);
        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_SUCCESS_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
        return createPartenaireResponseWs;
    }

    //@Transactional
    public CreatePartenaireResponseWs subcribeThirdStep(RequestContextWs requestContext, Partenaire partenaire, DataInWs dataInWs, HttpServletRequest httpServletRequest, String url) throws NotificationException, OwomiException {
        PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()),httpServletRequest.getHeader("access-token"));
        logger.debug("URL VALUE =====> " + url);
        if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND)) throw new OwomiException("Key Error", ErrorCodeEnums.KEY_ERROR, "Cle de cryptage introuvable");
        String data = decrypt(dataInWs.getData(), privateKeyWs.getPrivateKey());
        Type type = new TypeToken<CreatePartenaireStepTreeWs>(){}.getType();
        CreatePartenaireStepTreeWs createPartenaireStepTreeWs = new Gson().fromJson(data, type);
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        if(createPartenaireStepTreeWs.getPin().length() > 5){
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_BAD_SIZE_PIN_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.error);
            return createPartenaireResponseWs;
        }
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(createPartenaireStepTreeWs.getPin(), salt);
        partenaire.setSalt(salt);
        partenaire.setPin(hashed);
        partenaire.setSecour(createPartenaireStepTreeWs.getSecour());
        corporateService.createDefaultCorporate(requestContext,partenaire);
        partenaire.setSubscribed(true);
        partenaire.setSubcriptionCompleted(Boolean.TRUE);
        partenaire.setDateinscription(new Date());
        partenaireRepository.save(partenaire);
        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_SUCCESS_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
        try {
            CommonResponseWs commonResponseWs = networkToolClient.getChefCommunity(partenaire.getEmail(),httpServletRequest.getHeader("access-token"));
            if(commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)){
                PartenaireWs partenaireWs = new Gson().fromJson(new Gson().toJson(commonResponseWs.getData()),PartenaireWs.class);
                logger.debug("Partenaire ===> " + partenaireWs);
                networkToolClient.joinCommunity(partenaireWs.getId(),httpServletRequest.getHeader("access-token"));
            }
        }catch (OwomiException e) {
            e.printStackTrace();
        }


        return createPartenaireResponseWs;
    }

    @Transactional
    public CommonResponseWs updateFireBaseToken(RequestContextWs requestContext,Partenaire partenaire, String fireBaseToken){
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        if (fireBaseToken == null || fireBaseToken.trim().isEmpty()) return new CommonResponseWs(WsOperationResponseStatus.failed, "fireBaseToken is null", 404, null);
        logger.debug("updateFireBaseToken partenaire {} token {} ", partenaire.getId(), fireBaseToken);
        partenaire.setFirebasetoken(fireBaseToken);
        partenaireRepository.save(partenaire);
        this.notificationService.createUser(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, i18n.internationalize("update.token.message"), 200, null);
    }

    @Transactional
    public PartenaireWs getInfos(RequestContextWs requestContext,String telephone) throws OwomiException, DataNotFoundException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
        EntityUtils.checkIfEntityIsNull(Partenaire.class,partenaire,"telephone", telephone);
        PartenaireWs partenaireWs = mapper.map(partenaire,PartenaireWs.class);
        return partenaireWs;
    }

    public Terminal createTerminal(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs) {
        Terminal terminal = terminalRepository.findByIdentifiant(createPartenaireStepOneWs.getIdphone())
                .stream().reduce((first, last) -> last).orElse(new Terminal());
        terminal.setAndroidversion(createPartenaireStepOneWs.getVersion());
        terminal.setMail(createPartenaireStepOneWs.getMail());
        terminal.setModel(createPartenaireStepOneWs.getModel());
        terminal.setOperateur(createPartenaireStepOneWs.getOperateur());
        terminal.setIdentifiant(createPartenaireStepOneWs.getIdphone());
        terminal = terminalRepository.save(terminal);
        return terminal;
    }

    private Partenaire createPartenaire(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs, Pays pays) throws OwomiException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(createPartenaireStepOneWs.getFullPhoneNumber());
        if (pays.getId() == 68 && createPartenaireStepOneWs.getFullPhoneNumber().trim().length() == 12) {
            partenaire = partenaireRepository.findOneByTelephone(createPartenaireStepOneWs.getFullPhoneNumber().trim().replaceFirst("0", ""));
        }
        FireBaseUtils fireBaseUtils = new FireBaseUtils(createPartenaireStepOneWs.getFirebaseToken());
        String fireBaseId = fireBaseUtils.getFireBaseId();
        String fireBaseEmail = fireBaseUtils.getFireBaseEmail();
        logger.debug("new firebase id is : " + fireBaseId);
        String code = StringUtils.generateCodeVerification();
        if (partenaire == null) {
            Partenaire firebasePartner = partenaireRepository.findOneByFirebaseid(fireBaseId);
            if (firebasePartner != null && firebasePartner.isSubcriptionCompleted()) {
                throw new OwomiException(i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"), ErrorCodeEnums.FIREBASE_ID_ALREADY_USED_ERROR, i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"));
            } else if (firebasePartner != null && !firebasePartner.isSubcriptionCompleted()) {
                firebasePartner.setTelephone(createPartenaireStepOneWs.getFullPhoneNumber().trim());
                if (pays.getId() == 68 && createPartenaireStepOneWs.getFullPhoneNumber().trim().length() == 12)
                    firebasePartner.setTelephone(createPartenaireStepOneWs.getFullPhoneNumber().trim().replaceFirst("0", ""));
                firebasePartner.setDateCode(new Date());
                firebasePartner.setPays(pays);
                firebasePartner.setCode(code);
                firebasePartner.setCodeVerif(code);
                partenaire = partenaireRepository.save(firebasePartner);
            } else {
                partenaire = new Partenaire();
                partenaire.setTelephone(createPartenaireStepOneWs.getFullPhoneNumber().trim());
                if(pays.getId()==68 && createPartenaireStepOneWs.getFullPhoneNumber().trim().length()==12) partenaire.setTelephone(createPartenaireStepOneWs.getFullPhoneNumber().trim().replaceFirst("0",""));
                partenaire.setEmail(createPartenaireStepOneWs.getMail());
                partenaire.setType(String.valueOf(TypePartenaireEnums.SIMPLE.getUid()));
                partenaire.setPays(pays);
                partenaire.setDateCode(new Date());
                partenaire.setCodeVerif(code);
                partenaire.setCode(code);
                partenaire.setConnectedSansNumero(Boolean.FALSE);
                partenaire.setSubscribed(Boolean.FALSE);
                partenaire.setFirebaseid(fireBaseId);
                partenaire.setEtapeInscriptionReseau(0);
                partenaire.setAddress(Boolean.FALSE);
                partenaire.setAutoriserResetPin(Boolean.TRUE);
                partenaire = partenaireRepository.save(partenaire);
            }

        } else {
            if (partenaire.getFirebaseid() == null && fireBaseEmail != null && fireBaseEmail.equals(partenaire.getEmail())) {
                partenaire.setFirebaseid(fireBaseId);
                partenaire = partenaireRepository.save(partenaire);
                this.notificationService.createUser(partenaire);
            }
            if (!fireBaseId.equals(partenaire.getFirebaseid()))
                throw new OwomiException(i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ERROR_MESSAGE"), ErrorCodeEnums.UNKNOWN_SOCIAL_ID, i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ERROR_MESSAGE"));
            partenaire.setFirebaseid(fireBaseId);
            partenaire.setDateCode(new Date());
            partenaire.setCodeVerif(code);
            if (partenaire.getTelephone().equals("22990947373")) partenaire.setCodeVerif("123456");
            partenaire.setAddress(partenaire.getAddress() == null ? Boolean.FALSE : partenaire.getAddress());
            partenaireRepository.save(partenaire);
        }
        return partenaire;
    }

    private Partenaire createPartenaireApple(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs, Pays pays) throws OwomiException {
        Partenaire partenaire = partenaireRepository.findOneByEmail(createPartenaireStepOneWs.getMail());
        String fireBaseId = new FireBaseUtils(createPartenaireStepOneWs.getFirebaseToken()).getFireBaseId();
        String code = StringUtils.generateCodeVerification();
        if(partenaire!=null){
            partenaire.setConnectedSansNumero(Boolean.TRUE);
            partenaire.setCodeVerif(code);
            partenaireRepository.save(partenaire);
            return partenaire;
        }
        partenaire = new Partenaire();
        partenaire.setEmail(createPartenaireStepOneWs.getMail());
        partenaire.setType(String.valueOf(TypePartenaireEnums.SIMPLE.getUid()));
        partenaire.setPays(pays);
        partenaire.setDateCode(new Date());
        partenaire.setSubscribed(Boolean.FALSE);
        partenaire.setFirebaseid(fireBaseId);
        partenaire.setNom("");
        partenaire.setPrenoms("");
        partenaire.setCodeVerif(code);
        partenaire.setConnectedSansNumero(Boolean.TRUE);
        partenaire.setEtapeInscriptionReseau(0);
        partenaire.setAddress(Boolean.FALSE);
        partenaire.setAutoriserResetPin(Boolean.TRUE);
        partenaire.setSubcriptionCompleted(Boolean.FALSE);
        partenaire.setTelephone(pays.getIndicatif()+MobileUtils.generateCompteNumberEightDigits(8));
        //partenaireMailerService.sendAMail(partenaire.getEmail(),"NUMERO DE TELEPHONE TEMPORAIRE OWOMI","votre numéro temporaire owomi est " + partenaire.getTelephone());
        return partenaireRepository.save(partenaire);

    }

    public PartenaireTerminal createPartenaireTerminal(RequestContextWs requestContext, CreatePartenaireStepOneWs createPartenaireStepOneWs, Partenaire partenaire, Terminal terminal) {
        PartenaireTerminal partenaireTerminal = partenaireTerminalRepository.findByTerminal_IdentifiantAndPartenaire_Telephone(createPartenaireStepOneWs.getIdphone(), createPartenaireStepOneWs.getNumero()).stream().reduce((first, last) -> last).orElse(null);
        if (partenaireTerminal == null) {
            String idunique = generateOTP(4) + createPartenaireStepOneWs.getIdphone() + createPartenaireStepOneWs.getNumero() + generateOTP(4);
            partenaireTerminal = new PartenaireTerminal();
            partenaireTerminal.setDateassociation(new Date());
            partenaireTerminal.setEtat(0);
            partenaireTerminal.setPartenaire(partenaire);
            partenaireTerminal.setTerminal(terminal);
            partenaireTerminal.setIdunique(idunique);
        } else {
            partenaireTerminal.setDateassociation(new Date());
        }
        return partenaireTerminalRepository.save(partenaireTerminal);
    }

    @Transactional
    public CommonResponseWs checkMoyenRecpStatus(RequestContextWs requestContext, Partenaire partenaire){
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        List<MoyenRecPartenaire> moyenRecPartenaire = moyenRecPartenaireRepository.findByPartenaire(partenaire);
        for(MoyenRecPartenaire target:moyenRecPartenaire){
            if(target.getStatut() == Boolean.TRUE || target.getStatut() == Boolean.FALSE){
                commonResponseWs.setStatus(WsOperationResponseStatus.success);
                commonResponseWs.setMessage(i18n.internationalize("moyen.rec.status1"));
            }else{
                commonResponseWs.setStatus(WsOperationResponseStatus.failed);
                commonResponseWs.setMessage(i18n.internationalize("moyen.rec.status2"));
            }
        }
        return commonResponseWs;
    }

    /*@Transactional
    public CommonResponseWs pinReinitializeStepOne(RequestContextWs requestContext,Partenaire partenaire) {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        String mail = partenaire.getEmail();
        String code = StringUtils.generateCodeVerification();
        partenaire.setCodeVerif(code);
        partenaireRepository.save(partenaire);
        partenaireMailerService.sendInfosPin(mail,code);
        commonResponseWs.setMessage("Un code de confirmation à été envoyer à votre adresse mail: "+ mail );
        return commonResponseWs;
    }*/

    @Transactional
    public CommonResponseWs pinReinitializeStepTwo(RequestContextWs requestContext, Partenaire partenaire, String code){
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        if(partenaire.getCodeVerif().equals(code)){
            commonResponseWs.setStatus(WsOperationResponseStatus.success);
            commonResponseWs.setMessage(i18n.internationalize("pin.reset.step2.message1"));
        }else {
            commonResponseWs.setStatus(WsOperationResponseStatus.error);
            commonResponseWs.setMessage(i18n.internationalize("pin.reset.step2.message2"));
        }
        return  commonResponseWs;
    }

    @Transactional
    public CommonResponseWs pinReinitializeStepThree(RequestContextWs requestContext, Partenaire partenaire, String newPin){
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(newPin, salt);
        partenaire.setSalt(salt);
        partenaire.setPin(hashed);
        partenaireRepository.save(partenaire);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        commonResponseWs.setMessage(i18n.internationalize("pin.reset.step3.message"));
        return commonResponseWs;
    }

    public CodeWs authenticate(RequestContextWs requestContext, String firebaseToken, String username) throws FirebaseAuthException, OwomiException, AuthentificationException {
        logger.debug("authenticate " + username);
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
        String result = decodedToken.getUid();
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(result);
        Partenaire partenaire = partenaireRepository.findOneByEmail(userRecord.getEmail());
        if (partenaire == null) throw new OwomiException("", ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR, "");
        String code = authentificationUtils.getCodeByUsernameAndPassword(username, partenaire.getCodeVerif());
        if (partenaire.getConnectedSansNumero()) return new CodeWs(code);
        Corporate corporate = this.corporateRepository.findOneByMatricule(partenaire.getTelephone());
        if (corporate == null) return new CodeWs(code);
        boolean provisoire = corporate.getProvisoire() != null && corporate.getProvisoire();
        if (provisoire) return new CodeWs(code);
        //logger.debug("phone number " + userRecord.getPhoneNumber().replaceFirst("[+]", "") + " e mail " + userRecord.getEmail());
        String firebaseNumber = Optional.ofNullable(userRecord.getPhoneNumber()).map(v -> v.replaceFirst("[+]", "")).orElse("");
        String newTelephoneRefactor = "";
        if (firebaseNumber.startsWith("241")) newTelephoneRefactor = getNewRefactoredNumber(firebaseNumber);
        String numTruncate = "";
        if (partenaire.getTelephone().startsWith("241") || partenaire.getTelephone().startsWith("233") || partenaire.getTelephone().startsWith("225")) {
            numTruncate = firebaseNumber.replaceFirst("0", "");
            logger.debug("numero truncated is  " + numTruncate);
        }
        if (!partenaire.getTelephone().equals(firebaseNumber)) {
            if (!numTruncate.equals(partenaire.getTelephone())) {
                if (!newTelephoneRefactor.equals(partenaire.getTelephone())) {
                    partenaire.setConnectedSansNumero(true);
                    //this.partenaireRepository.save(partenaire);
                    //throw new OwomiException("", ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR, "");
                }
            }
        }
        CodeWs codeWs = new CodeWs();
        codeWs.setCode(code);
        return codeWs;
    }

    public CommonResponseWs changementNumero (String numero,String newNumero) throws FirebaseAuthException, DataNotFoundException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(numero);
        EntityUtils.checkIfEntityIsNull(Partenaire.class,partenaire,"telephone",numero);
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,"matricule",numero);

        Partenaire partenaireNumber = partenaireRepository.findOneByTelephone(newNumero);
        if(partenaireNumber!=null && hasCorporate(partenaireNumber)) return new CommonResponseWs(WsOperationResponseStatus.failed,"le numéro est déjà associé a un autre compte",200,null);
        if(partenaireNumber!=null && !hasCorporate(partenaireNumber)) partenaireRepository.delete(partenaireNumber);
        String uid = partenaire.getFirebaseid();
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                .setPhoneNumber("+"+newNumero);
        UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
        logger.debug("phone number " + userRecord.getPhoneNumber());
        logger.debug("Email " + userRecord.getEmail());
        corporate.setMatricule(newNumero);
        corporateRepository.save(corporate);
        partenaire.setTelephone(newNumero);
        partenaireRepository.save(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success,"changement de numero avec succes" , 200,null);
    }

    public CommonResponseWs changementMail (String numero,String mail) throws FirebaseAuthException, DataNotFoundException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(numero);
        EntityUtils.checkIfEntityIsNull(Partenaire.class,partenaire,"telephone",numero);
        partenaire.setEmail(mail);
        partenaireRepository.save(partenaire);
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(partenaire.getFirebaseid())
                .setEmail(mail);
        UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);

        /*Partenaire partenaireMail = partenaireRepository.findOneByEmail(mail);
        UserRecord userRecordMail = FirebaseAuth.getInstance().getUserByEmail(mail);
        if(partenaireMail!=null && hasCorporate(partenaire)) return new CommonResponseWs(WsOperationResponseStatus.failed,"le mail est déjà associé a un autre compte",200,null);
        if(partenaireMail!=null && !hasCorporate(partenaire)){
            partenaireRepository.delete(partenaireMail);
            if(userRecordMail!=null) FirebaseAuth.getInstance().deleteUser(userRecordMail.getUid());
        }
        String uid = partenaire.getFirebaseid();
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                .setEmail(mail);
        UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
        logger.debug("phone number " + userRecord.getPhoneNumber());
        logger.debug("Email " + userRecord.getEmail());
        partenaire.setEmail(mail);
        partenaireRepository.save(partenaire);*/
        return new CommonResponseWs(WsOperationResponseStatus.success,"changement de mail avec succes" , 200,null);
    }

    private boolean hasCorporate(Partenaire partenaire) {
        PartenaireCorporate partenaireCorporate =partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true,partenaire);

        return partenaireCorporate!=null;
    }

    public void reajustement(RequestContextWs requestContextWs, String matricule , Double newSolde) throws DataNotFoundException, OwomiException {
        Corporate corporate = corporateRepository.findOneByMatricule(matricule);
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,"matricule",matricule);
        PlanComptable planComptableReajustement = planComptableRepository.findOneByLibelle("Reajustement de soldes");
        PartenaireCorporate partenaireCorporate = partenaireCorporateRepository.findOneByPk_Corporate(corporate);
        AccountSolde accountSolde = accountSoldeRepository.findOneByCompte(corporate.getNumeroCompte());
        Double pastSolde = accountSolde==null ? 0.0 : accountSolde.getSolde();
        Double reajustementValue = 0.0;
        String sens = "" ;
        if(newSolde < pastSolde ){
            reajustementValue= pastSolde - newSolde;
            sens = "0";
        }else {
            reajustementValue= newSolde - pastSolde;
            sens = "1";
        }
        String sensReajustement="";
        if(sens.equals("1")) sensReajustement="0";
        if(sens.equals("0")) sensReajustement="1";
        ResumeGrandLivreWs resume = new ResumeGrandLivreWs();
        //resume.setIdOperation(9999);
        //resume.setIdOperationString(9999);
        resume.setLibelle("Reajustement de solde !!!");
        resume.setMontant(reajustementValue);
        resume.setEcriture(IntegerUtils.generateEcriture());
        resume.setSens(sens);
        resume.setNumeroCompte(corporate.getNumeroCompte().getId());
        ResumeGrandLivreWs resume2 = new ResumeGrandLivreWs();
        //resume2.setIdOperation(9999);
        //resume2.setIdOperationString(9999);
        resume2.setLibelle("Reajustement de solde !!!");
        resume2.setMontant(reajustementValue);
        resume2.setEcriture(IntegerUtils.generateEcriture());
        resume2.setSens(sensReajustement);
        resume2.setNumeroCompte(planComptableReajustement.getId());

        List<ResumeGrandLivreWs> detailsOfOperation = new ArrayList<>();
        detailsOfOperation.add(resume);
        detailsOfOperation.add(resume2);
        logger.debug("details sent " + detailsOfOperation);
        accountServiceClient.saveOperation(detailsOfOperation);
        dataServiceClient.updateSolde(corporate.getNumeroCompte().getId(), newSolde, 0.0);
        activityLogCreator.create(partenaireCorporate.getPartenaire(), new ActivityLogObjetWs(ActivityLogObjetEnum.ROLLBACK_PARTENAIRE, new Object[0]), new ActivityLogContentWs(ActivityLogContentEnums.REFUSER_PARRAINAGE_EXECUTANT, partenaireCorporate.getPartenaire().getNom(), partenaireCorporate.getPartenaire().getPrenoms()), null);

    }

    public List<DeviseWs> getDeviseList(RequestContextWs requestContext) throws FirebaseMessagingException {
        List<DeviseWs> deviseWsList = new ArrayList<>();
        List<Devise> deviseList = deviseRepository.findAll();
        for(Devise devise : deviseList){
            deviseWsList.add(mapper.map(devise,DeviseWs.class));
        }
        return deviseWsList;
    }

    public DeviseWs getDevisePartenaire(RequestContextWs requestContext, Partenaire partenaire) {
        return mapper.map(partenaire.getPays().getDevise(),DeviseWs.class);
    }

    public VersionOwomiWs getVersion(RequestContextWs requestContext) {
        VersionOwomi versionOwomi = versionOwomiRepository.findAll().get(0);
        return mapper.map(versionOwomi,VersionOwomiWs.class);
    }

    public void creationPartenaireBoucle(RequestContextWs requestContextWs) throws OwomiException, AuthentificationException {
        List<SpecialPartenaire> specialPartenaireList = specialPartenaireRepository.findAll();
        for (SpecialPartenaire specialPartenaire : specialPartenaireList){
            creationPartenaire(requestContextWs,specialPartenaire.getId());
        }
    }

    public void creationPartenaire(RequestContextWs requestContextWs, Integer id) throws AuthentificationException, OwomiException {
        SpecialPartenaire specialPartenaire = specialPartenaireRepository.findOneById(id);
        Integer[] ids= {5689,5731,5801,5830,5763,5980};
        if(specialPartenaire!=null && !Arrays.asList(ids).contains(id)){
            Partenaire partenaire = new Partenaire();
            partenaire.setId(specialPartenaire.getId());
            partenaire.setTelephone(specialPartenaire.getTelephone());
            partenaire.setEmail(specialPartenaire.getEmail());
            partenaire.setType(String.valueOf(TypePartenaireEnums.SIMPLE.getUid()));
            partenaire.setPays(paysRepository.findOneByIndicatif(specialPartenaire.getTelephone().substring(0,3)));
            partenaire.setDateCode(new Date());
            partenaire.setCodeVerif(StringUtils.generateCodeVerification());
            partenaire.setCode(StringUtils.generateCodeVerification());
            partenaire.setFirebaseid(specialPartenaire.getFirebaseid());
            partenaire.setEtapeInscriptionReseau(0);
            partenaire.setAddress(Boolean.FALSE);
            partenaire.setAutoriserResetPin(Boolean.TRUE);
            partenaire.setNom(specialPartenaire.getNom());
            partenaire.setPrenoms(specialPartenaire.getPrenoms());
            partenaire.setSexe("0");
            partenaire.setActif(Boolean.TRUE);
            partenaire.setDebutActif(NetworkUtils.getDateOfFirstDayInMonth());
            partenaire.setDateActif(NetworkUtils.getDateOfLastDayInMonth());
            partenaire.setEtatActif("Niveau 2");
            partenaire.setDatenaissance(new Date());
            partenaire.setStatutBonusParrainage(true);
            partenaire.setStatutBonusPaticipationParrainage(true);
            partenaire.setChiffreAffaireReseau(Double.valueOf(id));
            partenaire.setPin("to init");
            Partenaire parent = null;
            Partenaire upline = null;
            if(specialPartenaire.getParent()!=null){
                parent = partenaireRepository.findOneById(specialPartenaire.getParent());
                if(parent==null) parent = partenaireRepository.findOneByChiffreAffaireReseau(specialPartenaire.getParent().doubleValue());
            }

            if(specialPartenaire.getUpline()!=null){
                upline = partenaireRepository.findOneById(specialPartenaire.getUpline());
                if(upline==null) upline = partenaireRepository.findOneByChiffreAffaireReseau(specialPartenaire.getUpline().doubleValue());
            }
            partenaire.setParent(parent);
            partenaire.setUpline(upline);
            if(specialPartenaire.getCommunaute()!=null)partenaire.setCommunaute(specialPartenaire.getCommunaute().toString());
            if(specialPartenaire.getEtapeInscriptionReseau()!=null)partenaire.setEtapeInscriptionReseau(specialPartenaire.getEtapeInscriptionReseau());
            if (specialPartenaire.getDateinscription() != null)
                partenaire.setDateparrainage(specialPartenaire.getDateinscription());
            if (specialPartenaire.getGrade() != null)
                partenaire.setGrade(gradesRepository.findOneById(specialPartenaire.getGrade()));
            partenaire = partenaireRepository.save(partenaire);
            Boolean respone = partenaireServiceClient.updatePartenaireId(id.toString(),partenaire.getId().toString());

            //TODO: CALL NEO4J TO UPDATE ID
            logger.debug("after create partenaire !!!! " + respone);
            PartenaireTerminal partenaireTerminal = new PartenaireTerminal();
            partenaireTerminal.setDateassociation(new Date());
            partenaireTerminal.setEtat(0);
            partenaireTerminal.setPartenaire(partenaire);
            CreatePartenaireStepOneWs createPartenaireStepOneWs = new CreatePartenaireStepOneWs();
            createPartenaireStepOneWs.setVersion("5.0.0");
            createPartenaireStepOneWs.setIdphone("idPhone" + StringUtils.generateCodeVerification());
            createPartenaireStepOneWs.setOperateur("MTN");
            createPartenaireStepOneWs.setMail("mail");
            createPartenaireStepOneWs.setModel("model");
            createPartenaireStepOneWs.setNumero(specialPartenaire.getTelephone());
            partenaireTerminal.setTerminal(createTerminal(requestContextWs,createPartenaireStepOneWs));
            partenaireTerminal.setIdunique( generateOTP(4) + createPartenaireStepOneWs.getIdphone() + createPartenaireStepOneWs.getNumero() + generateOTP(4));
            partenaireTerminal = partenaireTerminalRepository.save(partenaireTerminal);
            logger.debug("after partenaire terminal create !!!! " + respone);
            authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
            }

    }

    public void correctPartenaireNeo4j(RequestContextWs requestContextWs) throws OwomiException {
        List<Partenaire> partenaireList = partenaireRepository.findByChiffreAffaireReseauGreaterThan(5688.0);
        logger.debug("======= SIZE OF THE LIST RETRIEVED IS ========== " + partenaireList.size());
        if(partenaireList.size()==160){
            for(Partenaire partenaire : partenaireList){
                int value = (int) Math.round(partenaire.getChiffreAffaireReseau());
                Boolean response = partenaireServiceClient.correctPartenaireId(String.valueOf(value),partenaire.getId().toString());
                logger.debug(" Response from NEO4J CORRECT PARTENAIRE ID " + response);
            }
        }

        logger.debug("END OF THE PROCCESS");

    }

    public void updateCommandeOwomi(RequestContextWs requestContextWs, Integer idOrder) throws OwomiException, DataNotFoundException {
        Commande commande =commandeRepository.findOneByCommandeId(idOrder);
        CommandeShopizerWs commandeShopizerWs = shopizerServiceClient.getOrder(idOrder);
        Double sommePrixAchat = commandeShopizerWs.getProducts().stream().filter(produitShopizerWs -> !produitShopizerWs.getProduct().getDescription().getName().toLowerCase().contains("livraison")).mapToDouble(produitShopizerWs -> produitShopizerWs.getOrderedQuantity() * produitShopizerWs.getProduct().getProductWeight()).sum();
        BoutiqueMarchand boutiqueMarchand = boutiqueMarchandRepository.findOneById(commandeShopizerWs.getProducts().stream().filter(produitShopizerWs -> !produitShopizerWs.getProduct().getDescription().getName().toLowerCase().contains("livraison")).collect(Collectors.toList()).get(0).getProduct().getMarchand());
        logger.debug("id commande is ======= " + commande.getId());
        logger.debug("id corporate is ======= " + boutiqueMarchand.getCorporate().getId());
        Compensation compensation = compensationRepository.findByIdCommandeAndIdCorporateMarchand(commande.getId(),boutiqueMarchand.getCorporate().getId());

        logger.debug("compensation " + compensation.getDevise());
        logger.debug("sommePrixAchat " + sommePrixAchat);
        logger.debug("boutiqueMarchand.getCorporate() " + boutiqueMarchand.getCorporate());
        saveGrandLivre(boutiqueMarchand.getCorporate(),sommePrixAchat, compensation.getDevise(),idOrder);
        List<Compensation> compensationList = new ArrayList<>();
        compensationList.add(compensation);
        compensationList=reglerCompensation(compensationList);
        GlobalCompensation globalCompensation = mapToGlobalCompensation(compensationList);
        saveHistoriqueReglementCompensation(globalCompensation,compensationList);
        commande.setEtat(CommandStatusEnums.DELIVERED.getValue());
        commande.setPaid(true);
        commande.setFinalise(true);
        commandeRepository.save(commande);
    }

    private void saveGrandLivre(Corporate corporate, Double montant, String devise, Integer idOrder) throws OwomiException {
        OperationStringWs operationStringWs = new OperationStringWs();
        operationStringWs.setLibelle("COMPENSATION MARCHAND");
        operationStringWs.setMontant(montant);
        operationStringWs.setDeviseTransaction(devise);
        operationStringWs.setDeviseDefaut(devise);
        operationStringWs.setCodeProduit("compensation");
        operationStringWs.setActeurin(mapper.map(corporate.getNumeroCompte(), PlanComptableWs.class));
        operationStringWs.setActeurout(mapper.map(planComptableRepository.findOneById(100), PlanComptableWs.class));
        operationStringWs.setReference("F2F OWOMI");
        CommandeIdWs commandeIdWs = new CommandeIdWs();
        commandeIdWs.setId_order(idOrder);
        operationStringWs.setFormString(new Gson().toJson(commandeIdWs));

        CreateCommandeForOwomiWs createCommandeForOwomiWs = new CreateCommandeForOwomiWs();
        createCommandeForOwomiWs.setIdPartenaire(partenaireRepository.findByPrenoms("activite").getId());
        createCommandeForOwomiWs.setOperationStringWs(operationStringWs);
        createCommandeForOwomiWs.setCv(0d);

        CommonResponseWs commonResponseWs = dataServiceClient.createCommandeForOwomi(createCommandeForOwomiWs);
        operationStringWs.setId(commonResponseWs.getId());

        List<ResumeGrandLivreWs> listMouvement = dataServiceClient.getAccountMouvementDetail(operationStringWs);

        accountServiceClient.saveOperation(listMouvement);
        logger.debug("============ end of save grand livre ==========" );
    }

    private void saveHistoriqueReglementCompensation(GlobalCompensation globalCompensation, List<Compensation> listCompensation) {
        StringJoiner stringJoiner = new StringJoiner("#", "[", "]");
        for (Compensation compensation : listCompensation) {
            stringJoiner.add(compensation.getId().toString());
        }
        String idCompensationList = stringJoiner.toString();

        HistoriqueCompensation historique = new HistoriqueCompensation();
        historique.setIdCorporate(listCompensation.get(0).getIdCorporateMarchand());
        historique.setCompensations(idCompensationList);
        historique.setDateDebut(globalCompensation.getDate());
        historique.setDateFin(new Date());
        historique.setDatePayement(listCompensation.get(0).getDatePayement());
        historique.setDevise(listCompensation.get(0).getDevise());
        historique.setMontant(globalCompensation.getMontantNetCompenseGlobal());
        historiqueCompensationRepository.save(historique);

    }

    private List<Compensation> reglerCompensation(List<Compensation> listCompensation) {
        Date date = new Date();
        List<Compensation> listCompensationSaved = new ArrayList<>();

        for (Compensation compensation : listCompensation) {
            compensation.setStatut(StatutCompensation.REGLE.toString());
            compensation.setDatePayement(date);

            listCompensationSaved.add(compensationRepository.save(compensation));
        }

        return listCompensationSaved;
    }

    private GlobalCompensation mapToGlobalCompensation(List<Compensation> listCompensation) throws OwomiException, DataNotFoundException {
        Date firstDate = new Date();
        Double montantNetCompenseGlobal = 0d;
        Double montantBrutGlobal = 0d;
        Date dateDebut = listCompensation.get(0).getDateDebut();
        Date dateFin = listCompensation.get(0).getDateDebut();
        List<CompensationUnitaire> listCompensatonUnitaire = new ArrayList<>();
        List<CommandFilterListElementWs> commandesws = new ArrayList<>();
        for(Compensation compensation : listCompensation) {
            montantNetCompenseGlobal += compensation.getMontantNetCompense();
            montantBrutGlobal += compensation.getMontantBrut();

            if(dateDebut.after(compensation.getDateDebut())){
                dateDebut = compensation.getDateDebut();
            }

            if(dateFin.before(compensation.getDateFin())){
                dateFin = compensation.getDateFin();
            }

            Commande commande = commandeRepository.findOneById(compensation.getIdCommande());
            if(commande!=null)
                commandesws.add(parseCommandeToCommandFilterListElementWs(commande));

            listCompensatonUnitaire.add(mapToCompensatonUnitaire(compensation));

            if (compensation.getDateDebut().before(firstDate)) {
                firstDate = compensation.getDateDebut();
            }
        }

        String devise = listCompensation.get(0).getDevise();
        Corporate corporate = corporateRepository.findOneById(listCompensation.get(0).getIdCorporateMarchand());
        CorporateWs corporateWs = mapper.map(corporate, CorporateWs.class);

        GlobalCompensation globalCompensation = new GlobalCompensation();
        globalCompensation.setCorporate(corporateWs);
        globalCompensation.setMontantNetCompenseGlobal(montantNetCompenseGlobal);
        globalCompensation.setMontantBrutGlobal(montantBrutGlobal);
        globalCompensation.setDate(firstDate);
        globalCompensation.setDateDebut(dateDebut);
        globalCompensation.setDateFin(dateFin);
        globalCompensation.setCommandes(commandesws);
        globalCompensation.setDevise(devise);
        globalCompensation.setCompensationUnitaires(listCompensatonUnitaire);
        return globalCompensation;
    }

    private CompensationUnitaire mapToCompensatonUnitaire(Compensation compensation) throws DataNotFoundException, OwomiException {
        Commande commande = commandeRepository.findOneById(compensation.getIdCommande());
        EntityUtils.checkIfEntityIsNull(commande, "Erreur lors de la récupération de commande associée");

        Corporate corporate = new Corporate();
        corporate.setId(compensation.getIdCorporateMarchand());
        List<BoutiqueMarchand> listBoutiqueMarchand = boutiqueMarchandRepository.findByCorporate(corporate);
        List<Integer> listBoutiqueMarchandId = listBoutiqueMarchand.stream().map(BoutiqueMarchand::getId).collect(Collectors.toList());
        CommandeShopizerWs commandeShopizerWs = shopizerServiceClient.getOrder(commande.getCommandeId());
        List<ProduitShopizerWs> produitsFiltre = commandeShopizerWs.getProducts().stream()
                .filter(produitShopizerWs -> {
                    boolean verifie = !produitShopizerWs.getProduct().getDescription().getHighlights().equalsIgnoreCase("Livraison");
                    return verifie && listBoutiqueMarchandId.contains(produitShopizerWs.getProduct().getMarchand());
                })
                .collect(Collectors.toList());

        commandeShopizerWs.setProducts(produitsFiltre);

        CompensationUnitaire compensationUnitaire = new CompensationUnitaire();
        compensationUnitaire.setIdCompensation(compensation.getId());
        compensationUnitaire.setDatePayement(compensation.getDatePayement());
        compensationUnitaire.setStatut(compensation.getStatut());
        compensationUnitaire.setMontantBrut(compensation.getMontantBrut());
        compensationUnitaire.setMontantNetCompense(compensation.getMontantNetCompense());
        compensationUnitaire.setCommande(commandeShopizerWs);

        return compensationUnitaire;
    }

    private CommandFilterListElementWs parseCommandeToCommandFilterListElementWs(Commande commande){
        CommandFilterListElementWs out = new CommandFilterListElementWs();
        BoutiqueShopizer boutiqueShopizer = boutiqueShopizerRepository.findOneByCodeBoutique(commande.getStore());
        out.setId(commande.getId());
        out.setOrderStatus(commande.getEtat());
        out.setCartStatus(commande.getStatusPanier());
        out.setPaymentType(commande.getTypePayement());
        Partenaire partenaire = adressePartenaireRepository.findOneByAdresse(commande.getAdresse()).getPartenaire();
        Map<String, String> partner = new HashMap<>();
        partner.put("nom", partenaire.getNom());
        partner.put("prenoms", partenaire.getPrenoms());
        partner.put("telephone", partenaire.getTelephone());
        partner.put("email", partenaire.getEmail());
        out.setPartner(partner);
        out.setMontant(commande.getPrixFacture());
        out.setDevice(boutiqueShopizer == null ? null : boutiqueShopizer.getDevise());
        out.setDateCommand(commande.getDateCommande());
        out.setDateDelivered(commande.getDateLivraison());
        out.setDateReception(commande.getDateReception());
        out.setServiceOfDelivered(commande.getConfigServiceLivraison() == null || commande.getConfigServiceLivraison().getServiceDeLivraison() == null ? null : commande.getConfigServiceLivraison().getServiceDeLivraison().getNom());
        return out;
    }


    public void pushOperationNeo4j(RequestContextWs requestContext, Integer id) {
        Operation operation = operationRepository.findOneById(id);
        AchatWs achatWs = new AchatWs();
        achatWs.setCv(operation.getCommande().getCommissionengrangee());
        achatWs.setDateAchat(operation.getCommande().getDate().getTime());
        achatWs.setEtat(operation.getCommande().getEtat());
        achatWs.setIdCommandesP(operation.getCommande().getId().toString());
        achatWs.setIdParternaireOwomi(operation.getCommande().getPartenaire().getId().toString());
        achatWs.setStatus(operation.getCommande().getStatus());
        achatWs.setValidated(true);
        achatNeo4jActiveMqPublisher.sendAchat(new Gson().toJson(achatWs));
    }

    public void updatePartenaireCoteIvoire(RequestContextWs requestContextWs){
        List<Partenaire> partenaireList = partenaireRepository.findByPays(paysRepository.findOneById(38));

        for(Partenaire partenaire : partenaireList){
            if(partenaire.getTicketvalidation()==null && partenaire.getTelephone().length()==11){
                partenaire.setTicketvalidation(partenaire.getTelephone());
                String prefixeFound = partenaire.getTelephone().substring(3,5);
                if(Arrays.asList(moov).contains(prefixeFound)){
                    logger.debug("Prefixe moov trouvé pour l'utilisateur " + prefixeFound);
                    String newTelephone = "22501"+partenaire.getTelephone().substring(3,11);
                    partenaire.setTelephone(newTelephone);
                }else if(Arrays.asList(mtn).contains(prefixeFound)){
                    logger.debug("Prefixe mtn trouvé pour l'utilisateur " + prefixeFound);
                    String newTelephone = "22505"+partenaire.getTelephone().substring(3,11);
                    partenaire.setTelephone(newTelephone);
                }else if(Arrays.asList(orange).contains(prefixeFound)){
                    logger.debug("Prefixe orange trouvé pour l'utilisateur " + prefixeFound);
                    String newTelephone = "22507"+partenaire.getTelephone().substring(3,11);
                    partenaire.setTelephone(newTelephone);
                }else {
                    logger.debug("Prefixe non trouvé pour l'utilisateur avec le prefixe " + prefixeFound);
                }
                partenaireRepository.save(partenaire);
            }

        }
    }

    public void updateCorporateNumber(RequestContextWs requestContextWs) {
        List<Partenaire> partenaireList = partenaireRepository.findByPays(paysRepository.findOneById(38));
        for(Partenaire partenaire : partenaireList) {
            PartenaireCorporate partenaireCorporate = partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(Boolean.TRUE,partenaire);
            if(partenaireCorporate!=null){
                logger.debug("found corporate with number " + partenaire.getTelephone());
                Corporate corporate = partenaireCorporate.getCorporate();
                corporate.setMatricule(partenaire.getTelephone());
                corporateRepository.save(corporate);
            }
        }

    }

    public void updateNumberGabon(RequestContextWs requestContextWs) {
        List<Partenaire> partenaireList = partenaireRepository.findByPays(paysRepository.findOneById(68));
        for(Partenaire partenaire : partenaireList) {
            if(partenaire.getTelephone().length()==11){

                if(partenaire.getTelephone().substring(3,5).startsWith("07")){
                    String telephone = partenaire.getTelephone().replaceFirst("07","77");
                    partenaire.setTelephone(telephone);
                    partenaireRepository.save(partenaire);
                    refactorCorporate(partenaire,telephone);

                }else if(partenaire.getTelephone().substring(3,5).startsWith("06")){
                    String telephone = partenaire.getTelephone().replaceFirst("06","66");
                    partenaire.setTelephone(telephone);
                    partenaireRepository.save(partenaire);
                    refactorCorporate(partenaire,telephone);

                }else if(partenaire.getTelephone().substring(3,5).startsWith("02")){

                    String telephone = partenaire.getTelephone().replaceFirst("02","62");
                    partenaire.setTelephone(telephone);
                    partenaireRepository.save(partenaire);
                    refactorCorporate(partenaire,telephone);

                }else if(partenaire.getTelephone().substring(3,5).startsWith("04")){
                    String telephone = partenaire.getTelephone().replaceFirst("04","74");
                    partenaire.setTelephone(telephone);
                    partenaireRepository.save(partenaire);
                    refactorCorporate(partenaire,telephone);
                }
            }
        }

    }

    public String getNewRefactoredNumber(String telephone) {

        String toReturn = "";
        if (telephone.length() == 11 && telephone.substring(3, 5).startsWith("07")) {
            toReturn = telephone.replaceFirst("07", "77");
        } else if (telephone.length() == 11 && telephone.substring(3, 5).startsWith("06")) {
            toReturn = telephone.replaceFirst("06", "66");

        } else if (telephone.length() == 11 && telephone.substring(3, 5).startsWith("02")) {
            toReturn = telephone.replaceFirst("02", "62");

        }else if(telephone.length()==11 && telephone.substring(3,5).startsWith("04")){
                toReturn = telephone.replaceFirst("04","74");
        }

        return toReturn;
    }


    private void refactorCorporate(Partenaire partenaire, String telephone){
        PartenaireCorporate partenaireCorporate = partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(Boolean.TRUE,partenaire);
        if(partenaireCorporate!=null){
            logger.debug("found corporate with " + partenaire.getTelephone());
            Corporate corporate = partenaireCorporate.getCorporate();
            corporate.setMatricule(telephone);
            corporateRepository.save(corporate);
        }

    }

    public CommonResponseWs appleNumberAssociate(RequestContextWs requestContext, String telephone, Partenaire partenaire) throws WesternSmsException, OwomiException {
        String codeOtp = StringUtils.generateCodeVerification();
        corporateRepository.findOneByMatricule(telephone);
        if (telephone != null && !telephone.isEmpty()) {
            if (partenaireRepository.findOneByTelephone(telephone) != null)
                throw new OwomiException("Ce numéro est déja utilisé", ErrorCodeEnums.PARTENAIRE_NUMBER_ERROR, "Ce numéro est déja utilisé");
            partenaire.setCode(telephone);
        }
        String sms = "OTP PASSWORD : " + codeOtp;
        String objet = "Code OTP PASSWORD";
        this.notificationService.sendSms(partenaire, objet, sms);
        partenaire.setCodeVerif(codeOtp);
        partenaireRepository.save(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, null, null, null, null, null, null);

    }

    public CommonResponseWs appleNumberCheck(RequestContextWs requestContext, Partenaire partenaire, String codeOtp) throws OwomiException, NotificationException {
        if(!partenaire.getCodeVerif().equals(codeOtp)) throw new OwomiException("CODE IS INCORRECT");
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());

        if(corporate.getProvisoire()) {
            corporate.setMatricule(partenaire.getCode());
            corporate.setProvisoire(Boolean.FALSE);
            corporateRepository.save(corporate);
            partenaire.setTelephone(partenaire.getCode());
        }

        partenaire.setConnectedSansNumero(Boolean.FALSE);
        partenaireRepository.save(partenaire);
        corporateRepository.save(corporate);
        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire,"MISE A JOUR DU TELEPHONE", "votre numéro a bien été modifié", false, Collections.emptyMap());
        return new CommonResponseWs(WsOperationResponseStatus.success,null,null,null,null,null,null);


    }


    public void changeFirebaseNumberCiv (RequestContextWs requestContext) throws FirebaseAuthException {
        List<Partenaire> partenaireList = partenaireRepository.findByPays(paysRepository.findOneById(38));
        for(Partenaire partenaire : partenaireList){
            if(partenaire.getTelephone().length()==13){
                String uid = partenaire.getFirebaseid();
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                        .setPhoneNumber("+"+partenaire.getTelephone());
                UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
                logger.debug("phone number " + userRecord.getPhoneNumber());
                logger.debug("Email " + userRecord.getEmail());
            }

        }
    }

    public void dataPlatform(RequestContextWs requestContextWs) throws IOException, OwomiException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        Date start = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, 11); // 11 = december
        cal.set(Calendar.DAY_OF_MONTH, 31);
        Date end = cal.getTime();
        logger.debug(" START DATE IS " + start);
        logger.debug(" END DATE IS " + end);
        logger.debug("CALLING ACCOUNT SERVER WAITING FOR RESPONSE .....");
        List<GrandLivreWs> grandLivreWsList = accountServiceClient.getAll();
        Integer size = grandLivreWsList.size();
        logger.debug("RESPONSE RECEIVED SIZE IS " + size);
        List<DataUpdatedWs> dataUpdatedWsList = new ArrayList<>();

        for (GrandLivreWs grandLivreWs : grandLivreWsList){

                DataUpdatedWs dataUpdatedWs = new DataUpdatedWs();
                dataUpdatedWs.setTypeOperation(grandLivreWs.getLabel());
                dataUpdatedWs.setMontant(grandLivreWs.getMontant());
                dataUpdatedWs.setSens(grandLivreWs.getSens());
            Operation operation = operationRepository.findOneById(grandLivreWs.getIdOperation());
                if(operation==null) continue;
                Commandesp commandesp = operation.getCommande();
                dataUpdatedWs.setCv(commandesp.getCommissionengrangee());
                dataUpdatedWs.setDate(grandLivreWs.getDate());
                dataUpdatedWs.setDevise(commandesp.getPartenaire().getPays().getDevise().getLibelle());
                dataUpdatedWs.setNom(commandesp.getPartenaire().getNom());
                dataUpdatedWs.setPrenoms(commandesp.getPartenaire().getPrenoms());
                dataUpdatedWs.setPays(commandesp.getPartenaire().getPays().getNompays());
                dataUpdatedWs.setNumero(commandesp.getPartenaire().getTelephone());
                dataUpdatedWsList.add(dataUpdatedWs);

            size--;
            logger.debug("TAILLE RESTANTE IS " + size);
        }
        String str = new Gson().toJson(dataUpdatedWsList);
        BufferedWriter writer = new BufferedWriter(new FileWriter("commission"));
        writer.write(str);
        writer.close();

    }

    public void dataPlatform2(RequestContextWs requestContextWs) throws IOException, OwomiException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2022);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        Date start = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        cal.set(Calendar.YEAR, 2021);
        cal.set(Calendar.MONTH, 11); // 11 = december
        cal.set(Calendar.DAY_OF_MONTH, 31);
        Date end = cal.getTime();

       List<PlanComptable> planComptableList = planComptableRepository.findByDatecreationBefore(start);
       Integer size = planComptableList.size();

       List<PorteFeuilleWs> porteFeuilleWsList = new ArrayList<>();

       for(PlanComptable planComptable : planComptableList){

           PorteFeuilleWs porteFeuilleWs = new PorteFeuilleWs();
           AccountSolde accountSolde = accountSoldeRepository.findOneByCompte(planComptable);
           Double solde = accountSolde == null ? 0.0 : accountSolde.getSolde();
           CommonResponseWs commonResponseWs = accountServiceClient.getDifference(end,planComptable.getId());
           Double difference = (Double) commonResponseWs.getData();
           solde = solde + difference;
           porteFeuilleWs.setLibelle(planComptable.getLibelle());
           porteFeuilleWs.setMontant(solde);

           Corporate corporate = corporateRepository.findByNumeroCompte(planComptable);
           if(corporate ==null ) {
               porteFeuilleWs.setDevise("XOF");
               porteFeuilleWsList.add(porteFeuilleWs);
               size--;
               continue;
           }

           PartenaireCorporate partenaireCorporate;

           try {
               partenaireCorporate = partenaireCorporateRepository.findOneByPk_Corporate(corporate);
           }catch (Exception e) {
               porteFeuilleWs.setDevise("XOF");
               porteFeuilleWsList.add(porteFeuilleWs);
               size--;
               continue;
           }

           if(partenaireCorporate ==null ) {
               porteFeuilleWs.setDevise("XOF");
               porteFeuilleWsList.add(porteFeuilleWs);
               size--;
               continue;
           }
           String devise = partenaireCorporate.getPartenaire().getPays().getDevise().getCode();
           porteFeuilleWs.setDevise(devise);
           porteFeuilleWsList.add(porteFeuilleWs);
           size--;
           logger.debug("TAILLE DE LA LISTE RESTANT EST " + size);
       }

        String str = new Gson().toJson(porteFeuilleWsList);
        BufferedWriter writer = new BufferedWriter(new FileWriter("porteFeuille2021"));
        writer.write(str);
        writer.close();
    }

    public void achats(RequestContextWs requestContextWs) throws OwomiException, IOException {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2021);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        Date start = cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        cal.set(Calendar.YEAR, 2021);
        cal.set(Calendar.MONTH, 11); // 11 = december
        cal.set(Calendar.DAY_OF_MONTH, 31);
        Date end = cal.getTime();

        List<Commande> commandeList = commandeRepository.findByDateCommandeBetweenAndIsPaidAndEtat(start,end,true,CommandStatusEnums.DELIVERED.getValue());

        int size = commandeList.size();
        List<DataUpdatedWs> result = new ArrayList<>();

        for (Commande commande : commandeList){

            if(commande.getReferenceCommande()==null) continue;
            DataUpdatedWs dataUpdatedWs = new DataUpdatedWs();
            dataUpdatedWs.setNom(commande.getAdresse().getLast_name());
            dataUpdatedWs.setPrenoms(commande.getAdresse().getFirst_name());
            dataUpdatedWs.setNumero(commande.getAdresse().getMobile_number1());
            dataUpdatedWs.setTypeOperation("ACHAT");
            dataUpdatedWs.setDevise(commande.getAdresse().getPays().getDevise().getCode());
            dataUpdatedWs.setDate(commande.getDateCommande().toString());
            dataUpdatedWs.setMontant(commande.getPrixFacture());
            dataUpdatedWs.setPays(commande.getAdresse().getPays().getNompays());
            Operation operation = operationRepository.findOneByReferenceCommande(commande.getReferenceCommande());
            if(operation==null) continue;
            dataUpdatedWs.setCv(operation.getCommande().getCommissionengrangee());

            CommandeShopizerWs commandeShopizerWs = shopizerServiceClient.getOrder(commande.getCommandeId());

            List<ProduitShopizerWs> produitShopizerWs = commandeShopizerWs.getProducts().stream().filter(produitShopizerWs1 -> produitShopizerWs1.getProduct()!=null && !produitShopizerWs1.getProduct().getDescription().getName().toLowerCase().contains("livraison") && produitShopizerWs1.getProduct().getMarchand()!=null).collect(Collectors.toList());

            if(produitShopizerWs.size()<1) {
                dataUpdatedWs.setMarchand("N/A");
                result.add(dataUpdatedWs);
                size--;
                continue;
            }
            Integer idMarchand = produitShopizerWs.get(0).getProduct().getMarchand();

            BoutiqueMarchand boutiqueMarchand = boutiqueMarchandRepository.findOneById(idMarchand);

            if (boutiqueMarchand == null) {
                dataUpdatedWs.setMarchand("N/A");
                result.add(dataUpdatedWs);
                size--;
                continue;
            }
            dataUpdatedWs.setMarchand(boutiqueMarchand.getNom());
            result.add(dataUpdatedWs);
            size--;
            logger.debug("TAILLE RESTANTE " + size);
        }

        String str = new Gson().toJson(result);
        BufferedWriter writer = new BufferedWriter(new FileWriter("achats"));
        writer.write(str);
        writer.close();



    }

    public CommonResponseWs pushImage(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws IOException {
        String url = fileService.storeFile(createPartenaireStepTwoWss.getPhoto());
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, url);


    }

    public CommonResponseWs updateProfilImage(
            RequestContextWs requestContext, Partenaire partenaire, String url
    ) {
        partenaire.setPhotoProfil(url);
        this.partenaireRepository.save(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);


    }

    public CommonResponseWs partenaireForParent(Partenaire partenaire) throws OwomiException {
        Partenaire parent = partenaire.getParent();
        if (parent == null)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "parent not exist", 404, null);
        PartenaireWs result = mapper.map(parent, PartenaireWs.class);
        result.setPays(mapper.map(parent.getPays(), PaysWs.class));
        return new CommonResponseWs(WsOperationResponseStatus.success, "parent partenaire", 200, result);
    }

    public CommonResponseWs verifyPinPartenaire(Partenaire partenaire, String pin, String token) throws OwomiException {
        logger.debug("PARTENAIRE IS {} ########", partenaire);
        logger.debug("PARTENAIRE TOKEN IS {} ########", token);
        logger.debug("PARTENAIRE PIN IS {} ########", pin);
        PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()), token);
        if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND))
            throw new OwomiException("Key Error", ErrorCodeEnums.KEY_ERROR, "Cle de cryptage introuvable");
        String pinDecrypted = decrypt(pin, privateKeyWs.getPrivateKey());
        if (pinDecrypted == null || pinDecrypted.length() != 5)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid pin size", 415, null);
        boolean validPin = BCryptUtils.checkpw(pinDecrypted, partenaire.getPin());
        logger.debug("pin status valide " + validPin);
        if (!validPin)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid pin", 416, null);
        return new CommonResponseWs(WsOperationResponseStatus.success, "pin correct", 200, null);
    }

    public CommonResponseWs verifyPin(
            SessionInfoWs session, Partenaire partenaire, MobilePartenairePinWs ws
    ) throws Exception {
        CommonResponseWs commonResponseWs = this.mobilePartenaireEncryptService.decode(session, ws.getPin());
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        String pin = this.mobilePartenaireEncryptService.decodedData(commonResponseWs);
        commonResponseWs = this.validationPin(partenaire, pin);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;

        boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
        logger.debug(" pin statusPin {}", statusPin);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, statusPin);
    }

    public CommonResponseWs validationPin(Partenaire partenaire, String pin) {
        if (pin == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "pin empty", 410, null);
        if (pin.length() != 5) return new CommonResponseWs(WsOperationResponseStatus.failed, "BadSize Pin", 414, null);

        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
    }

    public CommonResponseWs modifyInfoPartenaire(Partenaire partenaire, PartenaireUpdateInfoWs partenaireUpdateInfoWs){
        logger.debug("Partenaire info to update are ====> {}", partenaireUpdateInfoWs);

        long timestamp = partenaireUpdateInfoWs.getDateNaissanceTimestamp();
        Date date = new Date(timestamp);
        logger.debug("new date is ##### {}", date);

        PartenaireCodeOTP codeOTP = this.findCodeOtp(partenaire, partenaireUpdateInfoWs.getCodeVerification());
        if (codeOTP == null)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "code otp null or expired", 205, null);
        partenaire.setNom(partenaireUpdateInfoWs.getNom());
        partenaire.setPrenoms(partenaireUpdateInfoWs.getPrenom());
        partenaire.setDatenaissance(new Date(partenaireUpdateInfoWs.getDateNaissanceTimestamp()));
        logger.debug("partenaire date naissance is ====> {}", partenaire.getDatenaissance());
        partenaire.setSexe(partenaireUpdateInfoWs.getSexe());
        partenaire = partenaireRepository.save(partenaire);
        logger.debug("partenaire date naissance is ====> {}", partenaire.getDatenaissance());
        PartenaireWs partenaireWs = mapper.map(partenaire, PartenaireWs.class);
        partenaireWs.setDatenaissanceTimestamp(partenaire.getDatenaissance().getTime());
        return new CommonResponseWs(WsOperationResponseStatus.success, "les informations du partenaire sont modifie", 200, partenaireWs);
    }

    public CommonResponseWs sendOtpModification(Partenaire partenaire) {
        PartenaireCodeOTP codeOTP = this.createCodeOTP(partenaire);
        try {
            String sms = OWOMI_CODE + codeOTP.getCode() + OWOMI_CODE_SUITE_2;
            String telephone = partenaire.getTelephone();
            String objet = "Code OTP modification Info partenaire";
            this.notificationService.sendSms(partenaire, objet, sms);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CommonResponseWs(WsOperationResponseStatus.success, "send code", 200, null);
    }
    private PartenaireCodeOTP findCodeOtp(Partenaire partenaire, String value) {
        return this.partenaireCodeOTPRepository.findOneByCodeAndPartenaireAndExpirationDateGreaterThanEqual(
                value, partenaire, Date.from(Instant.now())
        );
    }

    public PartenaireCodeOTP createCodeOTP(Partenaire p) {
        String generated = StringUtils.generateCodeVerification();
        Instant now = Instant.now();
        Instant expiration = now.plus(2, ChronoUnit.MINUTES);
        PartenaireCodeOTP code = this.partenaireCodeOTPRepository.findOneByCodeAndPartenaireAndExpirationDateGreaterThanEqual(
                generated, p, Date.from(now)
        );
        if (code == null) code = new PartenaireCodeOTP();
        code.setCode(generated);
        code.setPartenaire(p);
        code.setRecordDate(Date.from(now));
        code.setExpirationDate(Date.from(expiration));
        code = this.partenaireCodeOTPRepository.save(code);
        return code;
    }

}
