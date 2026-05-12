package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.firebase.utils.FireBaseUtils;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.validators.PartenaireValidator;
import com.clubchretien.abonnement.ws.CreatePartenaireStepTwoWss;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owomi.commons.constants.MobilePartenaireConstant;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.FirebaseCodeEnums;
import com.owomi.commons.enums.TypePartenaireEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.utils.MobileUtils;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import com.owomi.message.publisher.publishers.AchatNeo4jActiveMqPublisher;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.neo4j.PartenaireServiceClient;
import com.owomi.rest.client.network.NetworkToolClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static com.clubchretien.abonnement.utils.UtilsService.decrypt;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;
import static com.owomi.commons.utils.StringUtils.generateOTP;

@Component
public class PartenaireAuthenticationService implements MobilePartenaireConstant {
    private static Logger logger = LogManager.getLogger(PartenaireAuthenticationService.class);

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
    NotificationService notificationService;
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
    NotificationTunnelSalesOnlineService notificationTunnelSalesOnlineService;

    @Autowired
    PartenaireService partenaireService;

    @Autowired
    HttpServletRequest httpServletRequest;

    public CommonResponseWs infoPartenaire(Partenaire partenaire)  {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        logger.debug("infoPartenaire ==> " + partenaire.getId());
        PartenaireWs result = mapper.map(partenaire, PartenaireWs.class);
        if (partenaire.getPays() != null) result.setPays(mapper.map(partenaire.getPays(), PaysWs.class));
        if (partenaire.getDatenaissance() != null) result.setDatenaissanceTimestamp(partenaire.getDatenaissance().getTime());
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs pinedPartenaire(Partenaire partenaire)  {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        logger.debug("infoPartenaire ==> " + partenaire.getId());
        Boolean result = partenaire.getPin() != null;
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }



    @Transactional
    public CreatePartenaireResponseWs initialSubscription(RequestContextWs requestContext, CreatePartenaireStepOneWs ws) throws DataNotFoundException, OwomiException, AuthentificationException, WesternSmsException, NotificationException {
        logger.debug("createPartenaireStepOneWs ==> " + ws);
        Pays pays = partenaireValidator.validate(requestContext, ws);
        Terminal terminal = partenaireService.createTerminal(requestContext, ws);
        Partenaire partenaire = this.createPartenaire(requestContext, ws, pays);
        logger.debug("createPartenaire id ==> " + partenaire.getId());
        logger.debug("createPartenaire id ==> " + partenaire.getEmail());
        logger.debug("createPartenaire id ==> " + partenaire.getTelephone());
        PartenaireTerminal partenaireTerminal = this.createPartenaireTerminal(requestContext, ws, partenaire, terminal);
        logger.debug("authentificationUtils.createUser id ==> " + partenaireTerminal.getIdunique());
        logger.debug("authentificationUtils.createUser code verif ==> " + partenaire.getCodeVerif());
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
//        String code = authentificationUtils.getCodeByUsernameAndPassword(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
//        logger.debug("authentificationUtils.createUser check ==> " + code);
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        createPartenaireResponseWs.setIdunique(partenaireTerminal.getIdunique());
        logger.debug("createPartenaire getSubscribed ==> " + partenaire.getSubscribed());
        boolean subscribed = partenaire.getSubscribed() != null && partenaire.getSubscribed();
        this.notificationTunnelSalesOnlineService.notificationTunnelInstallationAppFuture(partenaire);
        if (subscribed) {
            logger.debug("not new user ==> " + subscribed);
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_TRUE_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.updated);
            return createPartenaireResponseWs;
        }

        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_NULL_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);

        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());

        return createPartenaireResponseWs;
    }

    @Transactional
    public CreatePartenaireResponseWs initialSubscriptionApple(RequestContextWs requestContext, CreatePartenaireStepOneWs ws) throws DataNotFoundException, OwomiException, AuthentificationException, WesternSmsException, NotificationException {
        logger.debug("createPartenaireStepOneWs ==> " + ws);
        Pays pays = partenaireValidator.validate(requestContext, ws);
        Terminal terminal = partenaireService.createTerminal(requestContext, ws);
        Partenaire partenaire = this.createPartenaireApple(requestContext, ws, pays);
        logger.debug("createPartenaire id ==> " + partenaire.getId());
        logger.debug("createPartenaire id ==> " + partenaire.getEmail());
        logger.debug("createPartenaire id ==> " + partenaire.getTelephone());
        PartenaireTerminal partenaireTerminal = this.createPartenaireTerminal(requestContext, ws, partenaire, terminal);
        logger.debug("authentificationUtils.createUser id ==> " + partenaireTerminal.getIdunique());
        logger.debug("authentificationUtils.createUser code verif ==> " + partenaire.getCodeVerif());
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
//        String code = authentificationUtils.getCodeByUsernameAndPassword(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
//        logger.debug("authentificationUtils.createUser check ==> " + code);
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        createPartenaireResponseWs.setIdunique(partenaireTerminal.getIdunique());
        logger.debug("createPartenaire getSubscribed ==> " + partenaire.getSubscribed());
        boolean subscribed = partenaire.getSubscribed() != null && partenaire.getSubscribed();
        if (subscribed) {
            logger.debug("not new user ==> " + subscribed);
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_TRUE_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.updated);
            return createPartenaireResponseWs;
        }
        this.notificationTunnelSalesOnlineService.notificationTunnelInstallationAppFuture(partenaire);

        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SUBSCRIBED_NULL_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);

        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.INIT_SUBSCRIBE_NOTIF_TITLE"), false,  Collections.emptyMap());

        return createPartenaireResponseWs;
    }

    public CreatePartenaireResponseWs secondStepSubscription(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireStepTwoWss ws) throws OwomiException, FileUploadException, IOException {
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        partenaire.setNom(ws.getNom());
        partenaire.setPrenoms(ws.getPrenoms());
        partenaire.setSexe(ws.getSexe());
        if (ws.getPhotoProfil() != null) partenaire.setPhotoProfil(ws.getPhotoProfil());
        if (ws.getDatenaissance() != null) partenaire.setDatenaissance(StringUtils.fromISODate(ws.getDatenaissance()));
        partenaireRepository.save(partenaire);
        if (ws.getPhoto() != null) imageService.uploadImage(requestContext, partenaire, ws.getPhoto());
        Corporate corporate = corporateRepository.findOneByMatricule(partenaire.getTelephone());
        if (corporate != null) {
            corporate.setLibelle(partenaire.getNom() + " " + partenaire.getPrenoms());
            corporateRepository.save(corporate);
            PlanComptable planComptable = corporate.getNumeroCompte();
            planComptable.setLibelle(partenaire.getNom() + " " + partenaire.getPrenoms());
            planComptableRepository.save(planComptable);
        }
        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.SECOND_SUBSCRIBE_SUCCESS_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
        return createPartenaireResponseWs;
    }

    public CreatePartenaireResponseWs thirdStepSubscription(RequestContextWs requestContext, Partenaire partenaire, DataInWs dataInWs, HttpServletRequest httpServletRequest, String url) throws NotificationException, OwomiException {
        PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()), httpServletRequest.getHeader("access-token"));
        logger.debug("URL VALUE =====> " + url);
        if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND))
            throw new OwomiException("Key Error", ErrorCodeEnums.KEY_ERROR, "Cle de cryptage introuvable");
        String data = decrypt(dataInWs.getData(), privateKeyWs.getPrivateKey());
        Type type = new TypeToken<CreatePartenaireStepTreeWs>() {
        }.getType();
        CreatePartenaireStepTreeWs ws = new Gson().fromJson(data, type);
        CreatePartenaireResponseWs createPartenaireResponseWs = new CreatePartenaireResponseWs();
        if (ws == null || ws.getPin().length() != 5) {
            createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_BAD_SIZE_PIN_MESSAGE"));
            createPartenaireResponseWs.setStatus(WsOperationResponseStatus.error);
            return createPartenaireResponseWs;
        }
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(ws.getPin(), salt);
        partenaire.setSalt(salt);
        partenaire.setPin(hashed);
        partenaire.setSecour(Optional.ofNullable(ws.getSecour()).orElse(partenaire.getSecour()));
        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true, partenaire);
        if (pc == null) corporateService.createDefaultCorporate(requestContext, partenaire);
        partenaire.setSubscribed(true);
        partenaire.setSubcriptionCompleted(Boolean.TRUE);
        partenaire.setDateinscription(new Date());
        partenaire = partenaireRepository.save(partenaire);
        notificationService.notification(FirebaseCodeEnums.ACTIVITYLOG_LIST.getCode(), partenaire, i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_MESSAGE"), i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_NOTIF_TITLE"), false, Collections.emptyMap());
        createPartenaireResponseWs.setMessage(i18nCommons.internationalize("MobilePartenaireConstant.THIRD_SUBSCRIBE_SUCCESS_MESSAGE"));
        createPartenaireResponseWs.setStatus(WsOperationResponseStatus.success);
        try {
            CommonResponseWs commonResponseWs = networkToolClient.getChefCommunity(partenaire.getEmail(), httpServletRequest.getHeader("access-token"));
            if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) {
                PartenaireWs partenaireWs = new Gson().fromJson(new Gson().toJson(commonResponseWs.getData()), PartenaireWs.class);
                logger.debug("Partenaire  ===> " + partenaireWs);
                networkToolClient.joinCommunity(partenaireWs.getId(), httpServletRequest.getHeader("access-token"));
            }
        } catch (OwomiException e) {
            e.printStackTrace();
        }


        return createPartenaireResponseWs;
    }

    public CommonResponseWs validateSocial(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireOtpRequestWs ws) throws DataNotFoundException, OwomiException {
        Pays pays = partenaireValidator.validate(requestContext, mapper.map(ws, CreatePartenaireStepOneWs.class));
        FireBaseUtils fireBaseUtils = new FireBaseUtils(ws.getFirebaseToken());
        // get email and telephone on
        String fireBaseId = fireBaseUtils.getFireBaseId();
        String fireBaseEmail = fireBaseUtils.getFireBaseEmail();
        String fireBaseTelephone = fireBaseUtils.getFireBaseTelephone();
        if (!partenaire.getEmail().equalsIgnoreCase(fireBaseEmail))
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid token vs partenaire.email", 409, null);
        Partenaire p = this.partenaireRepository.findOneByTelephone(ws.getFullPhoneNumber());
        // check social correspondance phone and email
        if (p != null && !p.getEmail().equalsIgnoreCase(fireBaseEmail))
            return new CommonResponseWs(WsOperationResponseStatus.failed, "telephone associé deja a un autre utilisateur", 409, null);
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
    }

    public CommonResponseWs verifyPin(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireOtpRequestWs ws, String token) throws NotificationException, DataNotFoundException, OwomiException, AuthentificationException {
        PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()), token);
        if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND))
            throw new OwomiException("Key Error", ErrorCodeEnums.KEY_ERROR, "Cle de cryptage introuvable");
        String pinDecrypted = decrypt(ws.getPin(), privateKeyWs.getPrivateKey());
        if (pinDecrypted == null || pinDecrypted.length() != 5)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid pin size", 415, null);
        boolean validPin = BCryptUtils.checkpw(pinDecrypted, partenaire.getPin());
        if (!validPin) return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid pin", 416, null);
        CommonResponseWs commonResponseWs = this.validateSocial(requestContext, partenaire, ws);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
    }


    public CommonResponseWs sendOtpSubscription(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireOtpRequestWs ws) throws NotificationException, DataNotFoundException, OwomiException, AuthentificationException {
        CommonResponseWs commonResponseWs = this.validateSocial(requestContext, partenaire, ws);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        // send sms
        String code = StringUtils.generateCodeVerification();
        partenaire.setDateCode(new Date());
        partenaire.setCodeVerif(code);
        partenaire = partenaireRepository.save(partenaire);
        String objet = "Code Verification";
        String otp = partenaire.getCodeVerif();
        String telephone = ws.getFullPhoneNumber();
        if (telephone == null) telephone = partenaire.getTelephone();
        this.notificationService.sendOtp(telephone, otp, ws.getSendWhatsapp());
        CreatePartenaireStepOneWs stepOneWs = mapper.map(ws, CreatePartenaireStepOneWs.class);
        Terminal terminal = terminalRepository.findByIdentifiant(stepOneWs.getIdphone()).stream().reduce((first, last) -> last).orElse(new Terminal());
        PartenaireTerminal partenaireTerminal = this.createPartenaireTerminal(requestContext, stepOneWs, partenaire, terminal);
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
    }

    public CommonResponseWs otpValidationSubscription(RequestContextWs requestContext, Partenaire partenaire, CreatePartenaireOtpValidationWs ws) throws NotificationException, DataNotFoundException, OwomiException, AuthentificationException {
        CreatePartenaireStepOneWs stepOneWs = mapper.map(ws, CreatePartenaireStepOneWs.class);
        Pays pays = partenaireValidator.validate(requestContext, stepOneWs);
        FireBaseUtils fireBaseUtils = new FireBaseUtils(ws.getFirebaseToken());
        // get email and telephone on
        String fireBaseId = fireBaseUtils.getFireBaseId();
        String fireBaseEmail = fireBaseUtils.getFireBaseEmail();
        String fireBaseTelephone = fireBaseUtils.getFireBaseTelephone();
        if (!partenaire.getEmail().equalsIgnoreCase(fireBaseEmail))
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid token vs partenaire.email", 409, null);
        boolean alreadyValidateOtp = fireBaseTelephone != null && ws.getCodeOtp() == null;
        boolean validOtp = partenaire.getCodeVerif().equalsIgnoreCase(ws.getCodeOtp());
        logger.debug("verification current {} request {} ", partenaire.getCodeVerif(), ws.getCodeOtp());
        logger.debug("alreadyValidateOtp {} validOtp {}", alreadyValidateOtp, validOtp);
        if (!alreadyValidateOtp && !validOtp)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid otp verification", 420, null);
        logger.debug("alreadyValidateOtp {} fireBaseTelephone {} getFullPhoneNumber {}", alreadyValidateOtp, fireBaseTelephone, ws.getFullPhoneNumber());
        if (alreadyValidateOtp && !fireBaseTelephone.equalsIgnoreCase("+" + ws.getFullPhoneNumber()))
            return new CommonResponseWs(WsOperationResponseStatus.failed, "invalid token vs phone", 420, null);
        Partenaire p = this.partenaireRepository.findOneByTelephone(ws.getFullPhoneNumber());
        // check social correspondance phone and email
        if (p != null && !p.getEmail().equalsIgnoreCase(fireBaseEmail))
            return new CommonResponseWs(WsOperationResponseStatus.failed, "telephone associé deja a un autre utilisateur", 409, null);

        Terminal terminal = terminalRepository.findByIdentifiant(stepOneWs.getIdphone()).stream().reduce((first, last) -> last).orElse(new Terminal());
        PartenaireTerminal partenaireTerminal = this.createPartenaireTerminal(requestContext, stepOneWs, partenaire, terminal);
        authentificationUtils.createUser(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());
        String code = authentificationUtils.getCodeByUsernameAndPassword(partenaireTerminal.getIdunique(), partenaire.getCodeVerif());

//        PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByCurrentAndPk_Partenaire(true, partenaire);
//        Corporate corporate = pc.getCorporate();
        Corporate corporate = this.corporateRepository.findOneByMatricule(partenaire.getTelephone());
        logger.debug("save corporate and partenaire telephone {}", ws.getFullPhoneNumber());
        boolean provisoire = corporate.getProvisoire() != null && corporate.getProvisoire();
        boolean noNumber = partenaire.getConnectedSansNumero() != null && partenaire.getConnectedSansNumero();
        if (!(provisoire || noNumber))
            return new CommonResponseWs(WsOperationResponseStatus.success, "corporate non provisoire", 200, code);

        if (noNumber) {
            partenaire.setTelephone(ws.getFullPhoneNumber());
            partenaire.setPays(pays);
            partenaire.setConnectedSansNumero(Boolean.FALSE);
            partenaire = this.partenaireRepository.save(partenaire);
        }
        if (provisoire) {
            corporate.setAllowedCashOut(Boolean.TRUE);
            corporate.setProvisoire(Boolean.FALSE);
            corporate.setTelephone(ws.getFullPhoneNumber());
            corporate.setMatricule(ws.getFullPhoneNumber());
            corporate.setStatut(Boolean.TRUE);
            corporate = this.corporateRepository.save(corporate);
        }

        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, code);
    }

    private Partenaire savePartenaire(
            Pays pays, String fireBaseId, String code,
            Boolean connexionSansNumero, CreatePartenaireStepOneWs ws
    ) {
        Partenaire partenaire = new Partenaire();
        partenaire.setTelephone(ws.getFullPhoneNumber().trim());
        if (pays.getId() == 68 && ws.getFullPhoneNumber().trim().length() == 12)
            partenaire.setTelephone(ws.getFullPhoneNumber().trim().replaceFirst("0", ""));
        partenaire.setEmail(ws.getMail());
        partenaire.setType(String.valueOf(TypePartenaireEnums.SIMPLE.getUid()));
        partenaire.setPays(pays);
        partenaire.setDateCode(new Date());
        partenaire.setCodeVerif(code);
        partenaire.setCode(code);
        partenaire.setConnectedSansNumero(connexionSansNumero);
        partenaire.setSubscribed(Boolean.FALSE);
        partenaire.setFirebaseid(fireBaseId);
        partenaire.setEtapeInscriptionReseau(0);
        partenaire.setAddress(Boolean.FALSE);
        partenaire.setAutoriserResetPin(Boolean.TRUE);
        partenaire = partenaireRepository.save(partenaire);
        return partenaire;
    }

    private String uniqueProvisoireCompteNumberDigits(String indicatif) {
        String phone = MobileUtils.generateProvisoireCompteNumberDigits(6);
        for (int i = 0; i < 10; i++) {
            String full = indicatif + phone;
            Partenaire partenaire = this.partenaireRepository.findOneByTelephone(full);
            if (partenaire == null) return phone;
            phone = MobileUtils.generateProvisoireCompteNumberDigits(6);
        }
        return MobileUtils.generateProvisoireCompteNumberDigits(6);
    }

    private Partenaire createPartenaire(RequestContextWs requestContext, CreatePartenaireStepOneWs ws, Pays pays) throws OwomiException {
        logger.debug("createPartenaire is : " + ws);
        FireBaseUtils fireBaseUtils = new FireBaseUtils(ws.getFirebaseToken());
        String fireBaseId = fireBaseUtils.getFireBaseId();
        String fireBaseEmail = fireBaseUtils.getFireBaseEmail();
        String fireBaseTelephone = fireBaseUtils.getFireBaseTelephone();
        String code = StringUtils.generateCodeVerification();
        logger.debug("new firebase id is : " + fireBaseId);
        Partenaire partenaire = partenaireRepository.findOneByEmail(ws.getMail());
        if (partenaire != null) return partenaire;
        // check already used et subscription completed
        partenaire = partenaireRepository.findOneByFirebaseid(fireBaseId);
        boolean completed = partenaire != null && partenaire.isSubcriptionCompleted();
        if (partenaire != null && completed)
            throw new OwomiException(i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"), ErrorCodeEnums.FIREBASE_ID_ALREADY_USED_ERROR, i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"));
        if (partenaire != null) return partenaire;
        // create a new user
        ws.setNumero(this.uniqueProvisoireCompteNumberDigits(ws.getIndicatif()));
        partenaire = partenaireRepository.findOneByTelephone(ws.getFullPhoneNumber());
        if (partenaire != null) throw new OwomiException("Partenaire Numero Provisoire");

        boolean connectedWithPhone = fireBaseTelephone != null && fireBaseTelephone.equalsIgnoreCase(ws.getFullPhoneNumber());
        partenaire = this.savePartenaire(pays, fireBaseId, code, !connectedWithPhone, ws);
        return partenaire;
    }

    private Partenaire createPartenaireApple(RequestContextWs requestContext, CreatePartenaireStepOneWs ws, Pays pays) throws OwomiException {
        logger.debug("createPartenaire is : " + ws);
//        FireBaseUtils fireBaseUtils = new FireBaseUtils(requestContext, ws.getFirebaseToken());
//        String fireBaseId = fireBaseUtils.getFireBaseId(requestContext);
//        String fireBaseEmail = fireBaseUtils.getFireBaseEmail(requestContext);
//        String fireBaseTelephone = fireBaseUtils.getFireBaseTelephone(requestContext);
        String code = StringUtils.generateCodeVerification();
        //logger.debug("new firebase id is : " + fireBaseId);
        Partenaire partenaire = partenaireRepository.findOneByEmail(ws.getMail());
        if (partenaire != null) return partenaire;
        throw new OwomiException("need full authentication firebase");
        // check already used et subscription completed
//        partenaire = partenaireRepository.findOneByFirebaseid(fireBaseId);
//        boolean completed = partenaire != null && partenaire.isSubcriptionCompleted();
//        if (partenaire != null && completed)
//            throw new OwomiException(i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"), ErrorCodeEnums.FIREBASE_ID_ALREADY_USED_ERROR, i18nCommons.internationalize("MobilePartenaireConstant.FIREBASE_ID_ALREADY_USED_ERROR_MESSAGE"));
//        if (partenaire != null) return partenaire;
//        // create a new user
//        ws.setNumero(this.uniqueProvisoireCompteNumberDigits(ws.getIndicatif()));
//        partenaire = partenaireRepository.findOneByTelephone(ws.getFullPhoneNumber());
//        if (partenaire != null) throw new OwomiException("Partenaire Numero Provisoire");
//
//        boolean connectedWithPhone = fireBaseTelephone != null && fireBaseTelephone.equalsIgnoreCase(ws.getFullPhoneNumber());
//        partenaire = this.savePartenaire(pays, fireBaseId, code, !connectedWithPhone, ws);
//        return partenaire;
    }

    public PartenaireTerminal createPartenaireTerminal(RequestContextWs requestContext, CreatePartenaireStepOneWs ws, Partenaire partenaire, Terminal terminal) {
        PartenaireTerminal partenaireTerminal = partenaireTerminalRepository.findByTerminal_IdentifiantAndPartenaire_Email(ws.getIdphone(), partenaire.getEmail()).stream().reduce((first, last) -> last).orElse(null);
        if (partenaireTerminal == null) {
            String idunique = generateOTP(4) + ws.getIdphone() + partenaire.getTelephone() + generateOTP(4);
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

    public CodeWs authenticate(RequestContextWs requestContext, String firebaseToken, String username) throws FirebaseAuthException, OwomiException, AuthentificationException {
        logger.debug("authenticate " + username);
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
        String result = decodedToken.getUid();
        UserRecord userRecord = FirebaseAuth.getInstance().getUser(result);
        Partenaire partenaire = partenaireRepository.findOneByEmail(userRecord.getEmail());
        if (partenaire == null) throw new OwomiException("", ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR, "");
        String code = authentificationUtils.getCodeByUsernameAndPassword(username, partenaire.getCodeVerif());
        boolean connectedSansNumero = partenaire.getConnectedSansNumero() != null && partenaire.getConnectedSansNumero();
        if (connectedSansNumero) return new CodeWs(code);
        Corporate corporate = this.corporateRepository.findOneByMatricule(partenaire.getTelephone());
        if (corporate == null) return new CodeWs(code);
        boolean provisoire = corporate.getProvisoire() != null && corporate.getProvisoire();
        if (provisoire) return new CodeWs(code);
        String firebaseNumber = Optional.ofNullable(userRecord.getPhoneNumber()).map(v -> v.replaceFirst("[+]", "")).orElse("");
        logger.debug("phone number " + firebaseNumber + " e mail " + userRecord.getEmail());
        String newTelephoneRefactor = "";
        if (firebaseNumber.startsWith("241"))
            newTelephoneRefactor = this.partenaireService.getNewRefactoredNumber(firebaseNumber);
        String numTruncate = "";
        if (partenaire.getTelephone().startsWith("241") || partenaire.getTelephone().startsWith("233") || partenaire.getTelephone().startsWith("225")) {
            numTruncate = firebaseNumber.replaceFirst("0", "");
            logger.debug("numero truncated is  " + numTruncate);
        }
        if (!partenaire.getTelephone().equals(firebaseNumber)) {
            if (!numTruncate.equals(partenaire.getTelephone())) {
                if (!newTelephoneRefactor.equals(partenaire.getTelephone())) {
                    partenaire.setConnectedSansNumero(Boolean.TRUE);
                    this.partenaireRepository.save(partenaire);
//                    corporate.setProvisoire(Boolean.TRUE);
//                    this.corporateRepository.save(corporate);
                    //throw new OwomiException("", ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR, "");
                }
            }
        }
        CodeWs codeWs = new CodeWs();
        codeWs.setCode(code);
        return codeWs;
    }


}
