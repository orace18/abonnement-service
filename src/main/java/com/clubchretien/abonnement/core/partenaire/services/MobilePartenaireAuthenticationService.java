package com.clubchretien.abonnement.core.partenaire.services;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.partenaire.ws.*;
import com.clubchretien.abonnement.services.NotificationService;
import com.clubchretien.abonnement.ws.DemandeSuppressionPartenaireWs;
import com.owomi.commons.constants.TokenConstant;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.TypePartenaireEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.authentication.SessionClaimsWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.commons.ws.authentication.SessionUserWs;
import com.owomi.commons.ws.authentication.UserPinCreateWs;
import com.owomi.database.entities.DemandeSuppressionPartenaire;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.Pays;
import com.owomi.database.entities.enums.RaisonDemandeSuppression;
import com.owomi.database.entities.enums.StatutDemandeSuppression;
import com.owomi.database.repositories.DemandeSuppressionPartenaireRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.PaysRepository;
import com.owomi.rest.client.auth.AuthenticationSessionServiceClient;
import com.owomi.rest.client.dataservice.CorporateMatriculeServiceClient;
import com.owomi.rest.client.utils.RequestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class MobilePartenaireAuthenticationService implements TokenConstant {
    private static Logger logger = LogManager.getLogger(MobilePartenaireAuthenticationService.class);

    @Autowired
    Mapper mapper;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    PaysRepository paysRepository;
    @Autowired
    MobilePartenaireEncryptService mobilePartenaireEncryptService;
    @Autowired
    AuthenticationSessionServiceClient authenticationSessionServiceClient;

    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    NotificationService notificationService;

    @Autowired
    SecurityUtils securityUtils;

    @Autowired
    CorporateMatriculeServiceClient corporateMatriculeServiceClient;

    @Autowired
    DemandeSuppressionPartenaireRepository demandeSuppressionPartenaireRepository;

    private Boolean pinState(Partenaire entity) {
        if (entity == null) return false;
        return entity.getPin() != null && entity.getSalt() != null;
    }

    private Boolean countryState(Partenaire entity) {
        if (entity == null) return false;
        return entity.getPays() != null;
    }

    private Boolean subscribedState(Partenaire entity) {
        if (entity == null) return false;
        Boolean pin = this.pinState(entity);
        Boolean civilState = this.civilState(entity);
        Boolean emailVerified = entity.getEmailVerified() != null && entity.getEmailVerified();
        Boolean phoneVerified = entity.getPhoneNumberVerified() != null && entity.getPhoneNumberVerified();
        return pin && (emailVerified || phoneVerified) && civilState;
    }

    private Boolean subscriptionState(Partenaire entity) {
        if (entity == null) return false;
        Boolean pin = this.pinState(entity);
        Boolean civilState = this.civilState(entity);
        Boolean emailVerified = entity.getEmailVerified() != null && entity.getEmailVerified();
        Boolean phoneVerified = entity.getPhoneNumberVerified() != null && entity.getPhoneNumberVerified();
        return (emailVerified || phoneVerified) && (!pin || !civilState);
    }

    public Boolean civilState(Partenaire entity)  {
        if (entity == null) return false;
        Boolean named = entity.getNom() != null && entity.getPrenoms() != null;
        Boolean sex = entity.getSexe() != null;
        Boolean birthdate = entity.getDatenaissance() != null;
        return named && sex && birthdate;
    }

    private MobilePartenaireWs convertEntityToWs(Partenaire entity) {
        if (entity == null) return null;
        MobilePartenaireWs result = mapper.map(entity, MobilePartenaireWs.class);
        if (entity.getPays() != null) result.setPays(mapper.map(entity.getPays(), PaysWs.class));
        if (entity.getDatenaissance() != null) result.setDatenaissanceTimestamp(entity.getDatenaissance().getTime());
        if (entity.getDateActif() != null) result.setDateActifTimestamp(entity.getDateActif().getTime());
        return result;
    }

    private MobilePartenaireInscriptionStateWs convertToInscriptionState(Partenaire entity) {
        MobilePartenaireInscriptionStateWs result = new MobilePartenaireInscriptionStateWs();
        result.setPartenaire(this.convertEntityToWs(entity));
        result.setPinState(this.pinState(entity));
        result.setCivilState(this.civilState(entity));
        result.setSubscribed(this.subscribedState(entity));
        result.setSubscriptionState(this.subscriptionState(entity));
        Boolean emailVerified = entity.getEmailVerified() != null && entity.getEmailVerified();
        Boolean phoneVerified = entity.getPhoneNumberVerified() != null && entity.getPhoneNumberVerified();
        result.setPhoneNumberVerified(phoneVerified);
        result.setEmailVerified(emailVerified);
        result.setCountryState(this.countryState(entity));
        return result;
    }

    public Partenaire findPartenaire(SessionClaimsWs claims) {
        if (claims == null) return null;
        SessionInfoWs ws = this.mapper.map(claims, SessionInfoWs.class);
        return this.findPartenaire(ws);
    }

    public Partenaire findPartenaire(SessionInfoWs session) {
        if (session == null) return null;
        SessionUserWs user = session.getUser();
        if (user == null) return null;
        Partenaire partenaire = null;
        if (user.getFirebaseProviderId() != null)
            partenaire = this.partenaireRepository.findOneByFirebaseid(user.getFirebaseProviderId());
        boolean phoneVerified = user.getPhoneFullNumber() != null && user.getPhoneNumberVerified() != null && user.getPhoneNumberVerified();
        if (partenaire == null && phoneVerified)
            partenaire = this.partenaireRepository.findOneByTelephone(user.getPhoneFullNumber());
        boolean emailVerified = user.getEmail() != null && user.getEmailVerified() != null && user.getEmailVerified();
        if (partenaire == null && emailVerified)
            partenaire = this.partenaireRepository.findOneByEmail(user.getEmail());

        if (partenaire != null && partenaire.getRevoked() != null && partenaire.getRevoked())
            return null;

        return partenaire;
    }

    private Partenaire createPartenaire(SessionInfoWs session) {
        if (session == null) return null;
        SessionUserWs user = session.getUser();
        if (user == null) return null;
        Partenaire partenaire = new Partenaire();
        boolean phoneVerified = user.getPhoneFullNumber() != null && user.getPhoneNumberVerified() != null && user.getPhoneNumberVerified();
        boolean emailVerified = user.getEmail() != null && user.getEmailVerified() != null && user.getEmailVerified();
        if (phoneVerified && user.getPhoneCodeNumber() != null) {
            Pays pays = this.paysRepository.findOneByIndicatif(user.getPhoneCodeNumber());
            partenaire.setPays(pays);
        }
        partenaire.setDateinscription(new Date());
        partenaire.setFirebaseid(user.getFirebaseProviderId());
        if (phoneVerified) {
            partenaire.setTelephone(user.getPhoneFullNumber());
            partenaire.setPhoneFullNumber(user.getPhoneFullNumber());
            partenaire.setPhoneNumberVerified(user.getPhoneNumberVerified());
            partenaire.setPhoneCodeNumber(user.getPhoneCodeNumber());
            partenaire.setPhoneNumber(user.getPhoneNumber());
        }
        if (emailVerified) {
            partenaire.setEmail(user.getEmail());
            partenaire.setEmailFaked(user.getEmailFaked());
            partenaire.setEmailVerified(user.getEmailVerified());
        }
        partenaire.setPrenoms(user.getDisplayName());
        partenaire.setPhotoProfil(user.getPhotoProfil());
        partenaire.setEtapeInscriptionReseau(0);
        partenaire.setSubscribed(Boolean.FALSE);
        partenaire.setAutoriserResetPin(Boolean.TRUE);
        partenaire.setType(String.valueOf(TypePartenaireEnums.SIMPLE.getUid()));
        partenaire.setRevoked(Boolean.FALSE);
        partenaire.setSubscribed(this.subscribedState(partenaire));
        partenaire = this.partenaireRepository.save(partenaire);
        return partenaire;
    }

    public void createUserNotification(SessionInfoWs session, Partenaire partenaire) {
        if (session == null) return  ;
        SessionUserWs user = session.getUser();
        if (user == null) return ;
        ActivityLogUserWs ws = new ActivityLogUserWs();
        ws.setApp(FirebaseAppEnums.MOBILE);
        ws.setFirebaseId(user.getFirebaseProviderId());
        ws.setFirebaseToken(RequestUtils.header(httpServletRequest, ACCESS_TOKEN));
        ws.setEmail(partenaire.getEmail());
        CompletableFuture.runAsync(() -> {
            try {
                this.notificationService.createUser(ws);
                this.notificationService.welcomeApp(partenaire);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Partenaire creationPartenaire(SessionInfoWs session) {
        Partenaire partenaire = this.createPartenaire(session);
        this.createUserNotification(session, partenaire);
        return partenaire;
    }

    private Partenaire updatePartenaire(Partenaire partenaire, SessionInfoWs session) {
        if (partenaire == null) return null;
        if (session == null) return partenaire;
        if (partenaire.getRevoked() != null && partenaire.getRevoked()) return null;
        SessionUserWs user = session.getUser();
        if (user == null) return partenaire;
        boolean phoneVerified = user.getPhoneFullNumber() != null && user.getPhoneNumberVerified() != null && user.getPhoneNumberVerified();
        boolean emailVerified = user.getEmail() != null && user.getEmailVerified() != null && user.getEmailVerified();
        if (phoneVerified) {
            if (user.getPhoneFullNumber() != null && partenaire.getTelephone() == null) partenaire.setTelephone(user.getPhoneFullNumber());
            if (user.getPhoneFullNumber() != null && partenaire.getPhoneFullNumber() == null) partenaire.setPhoneFullNumber(user.getPhoneFullNumber());
            if (user.getPhoneCodeNumber() != null && partenaire.getPhoneCodeNumber() == null) partenaire.setPhoneCodeNumber(user.getPhoneCodeNumber());
            if (user.getPhoneNumber() != null && partenaire.getPhoneNumber() == null) partenaire.setPhoneNumber(user.getPhoneNumber());
            if (user.getPhoneNumberVerified() != null) partenaire.setPhoneNumberVerified(user.getPhoneNumberVerified());
            if (user.getPhoneCodeNumber() != null && partenaire.getPays() == null) {
                Pays pays = this.paysRepository.findOneByIndicatif(user.getPhoneCodeNumber());
                partenaire.setPays(pays);
            }
        }
        if (emailVerified) {
            if (user.getEmail() != null && partenaire.getEmail() == null) partenaire.setEmail(user.getEmail());
            if (user.getEmailFaked() != null) partenaire.setEmailFaked(user.getEmailFaked());
            if (user.getEmailVerified() != null) partenaire.setEmailVerified(user.getEmailVerified());
        }
        if (user.getDisplayName() != null && partenaire.getPrenoms() == null) partenaire.setPrenoms(user.getDisplayName());
        if (user.getPhotoProfil() != null && partenaire.getPhotoProfil() == null) partenaire.setPhotoProfil(user.getPhotoProfil());
        if (partenaire.getRevoked()) partenaire.setRevoked(Boolean.FALSE);
        partenaire.setSubscribed(this.subscribedState(partenaire));
        partenaire = this.partenaireRepository.save(partenaire);
        return partenaire;
    }

    public CommonResponseWs stateSubscription(SessionInfoWs session) {
        logger.debug("stateSubscription ==> {}");
        if (session == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "session", 401, null);
        Partenaire partenaire = this.findPartenaire(session);
        if (partenaire == null)  partenaire = this.creationPartenaire(session);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 404, null);
        this.updatePartenaire(partenaire, session);
        MobilePartenaireInscriptionStateWs result = this.convertToInscriptionState(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    private CommonResponseWs validationPin(Partenaire partenaire, String pin) {
        if (pin == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "pin empty", 410, null);
        if (pin.length() != 5) return new CommonResponseWs(WsOperationResponseStatus.failed, "BadSize Pin", 414, null);
        boolean resetPin = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
        if (this.pinState(partenaire) && !resetPin) return new CommonResponseWs(WsOperationResponseStatus.success, "pin already setted", 200, null);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
    }

    private Partenaire savePin(Partenaire partenaire, String pin) {
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(pin, salt);
        partenaire.setSalt(salt);
        partenaire.setPin(hashed);
        partenaire.setAutoriserResetPin(Boolean.FALSE);
        partenaire = this.partenaireRepository.save(partenaire);
        return partenaire;
    }

    private CommonResponseWs createSessionPin(Partenaire partenaire, MobilePartenairePinCreateWs ws) {
        String token = RequestUtils.header(httpServletRequest, ACCESS_TOKEN);
        UserPinCreateWs request = new UserPinCreateWs();
        request.setPin(ws.getPin());
        return this.authenticationSessionServiceClient.createSessionUserPin(token, request);
    }

    public CommonResponseWs configurePin(SessionInfoWs session, MobilePartenairePinCreateWs ws) {
        logger.debug("configurePin ==> {}", ws);
        if (session == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        Partenaire partenaire = this.findPartenaire(session);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        if (!Objects.equals(partenaire.getId(), ws.getPartenaireId())) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire vs input partenaire", 401, null);
        CommonResponseWs commonResponseWs = this.mobilePartenaireEncryptService.decode(session, ws.getPin());
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        String pin = this.mobilePartenaireEncryptService.decodedData(commonResponseWs);
        commonResponseWs = this.validationPin(partenaire, pin);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        commonResponseWs = this.createSessionPin(partenaire, ws);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return commonResponseWs;
        partenaire = this.savePin(partenaire, pin);
        MobilePartenaireInscriptionStateWs result = this.convertToInscriptionState(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs configureCivilInformation(Partenaire partenaire, MobilePartenaireCivilInformationCreateWs ws) throws OwomiException {
        logger.debug("configureCivilInformation ==> {}", ws);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        if (!Objects.equals(partenaire.getId(), ws.getPartenaireId())) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire vs input partenaire", 401, null);
        if (ws.getNom() != null) partenaire.setNom(ws.getNom());
        if (ws.getPrenoms() != null)  partenaire.setPrenoms(ws.getPrenoms());
        if (ws.getSexe() != null) partenaire.setSexe(ws.getSexe());
        if (ws.getPhotoProfil() != null) partenaire.setPhotoProfil(ws.getPhotoProfil());
        if (ws.getDatenaissance() != null) partenaire.setDatenaissance(StringUtils.fromISODate(ws.getDatenaissance()));
        partenaire = this.partenaireRepository.save(partenaire);
        MobilePartenaireInscriptionStateWs result = this.convertToInscriptionState(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs configureCountry(Partenaire partenaire, MobilePartenaireCountryCreateWs ws) throws OwomiException {
        logger.debug("configureCivilInformation ==> {}", ws);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        if (!Objects.equals(partenaire.getId(), ws.getPartenaireId())) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire vs input partenaire", 401, null);
        Pays pays = this.paysRepository.findOneByIndicatif(ws.getPhoneCodeNumber());
        if (pays == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "pays", 404, null);
        boolean phoneVerified = partenaire.getPhoneFullNumber() != null && partenaire.getPhoneNumberVerified() != null && partenaire.getPhoneNumberVerified();
        boolean countryState = this.countryState(partenaire);
        boolean samePhoneCounty = phoneVerified && countryState && pays.getIndicatif().equalsIgnoreCase(partenaire.getPhoneCodeNumber());
        if (phoneVerified && countryState && !samePhoneCounty)
            return new CommonResponseWs(WsOperationResponseStatus.failed, "country compatibility phone", 404, null);
        partenaire.setPays(pays);
        partenaire = this.partenaireRepository.save(partenaire);
        MobilePartenaireInscriptionStateWs result = this.convertToInscriptionState(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    public CommonResponseWs configureProfilPhoto(Partenaire partenaire, String url) {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        partenaire.setPhotoProfil(url);
        partenaire = this.partenaireRepository.save(partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
    }


    public CommonResponseWs deleteAccount(Partenaire partenaire) {
        CommonResponseWs response = new CommonResponseWs();

        if (partenaire == null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("partenaire introuvable");
            response.setCode(401);
            response.setData(null);
        } else {
            partenaire.setRevoked(true);
            partenaire = this.partenaireRepository.save(partenaire);

            AnonymizeRequestWs anonymizeRequestWs = new AnonymizeRequestWs();
            anonymizeRequestWs.setFrom(partenaire.getEmail());
            CommonResponseWs anonymizeResponseWs = new CommonResponseWs();
            logger.info("anonymizeRequestWs : {}", anonymizeRequestWs);

            try {
                anonymizeResponseWs = this.corporateMatriculeServiceClient.anonymize(anonymizeRequestWs);
                logger.info("anonymizeRequestWs : {}", anonymizeRequestWs);
            } catch (OwomiException e) {
                return  new CommonResponseWs(WsOperationResponseStatus.failed, e.getMessage(), 401, null);
            }

            if (anonymizeResponseWs.getStatus() != WsOperationResponseStatus.success)
                return new CommonResponseWs(WsOperationResponseStatus.failed, anonymizeResponseWs.getMessage(), 401, null);
            response.setStatus(WsOperationResponseStatus.success);
            response.setMessage("partenaire supprimé");
            response.setCode(200);
            response.setData(null);
        }

        return response;
    }

    public CommonResponseWs isDeleted(HttpServletRequest httpServletRequest) {
        CommonResponseWs response = new CommonResponseWs();

        SessionUserWs sessionUserWs = this.securityUtils.getSessionInfoFromHeader(httpServletRequest).getUser();
        if (sessionUserWs.getPhoneFullNumber() != null && !("").equals(sessionUserWs.getPhoneFullNumber())) {
            Partenaire partenaire = partenaireRepository.findOneByTelephone(sessionUserWs.getPhoneFullNumber());
            if (partenaire != null && partenaire.getRevoked() != null && partenaire.getRevoked()) {
                response.setStatus(WsOperationResponseStatus.success);
                response.setMessage("partenaire supprimé");
                response.setCode(200);
                response.setData(true);
            } else {
                response.setStatus(WsOperationResponseStatus.success);
                response.setMessage("partenaire non supprimé");
                response.setCode(200);
                response.setData(false);
            }
        } else if (sessionUserWs.getEmail() != null && !("").equals(sessionUserWs.getEmail())) {
            Partenaire partenaire = partenaireRepository.findOneByEmail(sessionUserWs.getEmail());
            if (partenaire != null && partenaire.getRevoked() != null && partenaire.getRevoked()) {
                response.setStatus(WsOperationResponseStatus.success);
                response.setMessage("partenaire supprimé");
                response.setCode(200);
                response.setData(true);
            } else {
                response.setStatus(WsOperationResponseStatus.success);
                response.setMessage("partenaire non supprimé");
                response.setCode(200);
                response.setData(false);
            }
        } else {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("partenaire introuvable");
            response.setCode(400);
            response.setData(null);
        }
        return response;
    }

    public CommonResponseWs initializeDelete(DemandeSuppressionPartenaireWs ws) {
        CommonResponseWs response = new CommonResponseWs();
        DemandeSuppressionPartenaire demande = new DemandeSuppressionPartenaire();
        Partenaire partenaire = partenaireRepository.findOneById(ws.getPartenaireId());
        if (partenaire == null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("partenaire introuvable");
            response.setCode(401);
            response.setData(null);
            return response;
        }
        DemandeSuppressionPartenaire oldDemande = demandeSuppressionPartenaireRepository.findByPartenaireIdAndStatus(ws.getPartenaireId(), StatutDemandeSuppression.EN_COURS.getValue());
        if (oldDemande != null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("une demande de suppression est déjà en cours");
            response.setCode(402);
            response.setData(null);
            return response;
        }
        demande.setPartenaire(partenaire);
        demande.setDateDemande(new Date());
        demande.setRaison(RaisonDemandeSuppression.valueOf(ws.getRaison()).getValue());
        demande.setStatus(StatutDemandeSuppression.EN_COURS.getValue());
        demande.setAvis(ws.getAvis());
        demande = demandeSuppressionPartenaireRepository.save(demande);

        EmailWs emailWs = new EmailWs();
        String motif = "";
        String title = "Processus de suppression de compte enclenché";
        Boolean isHtml = false;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(demande.getDateDemande());
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        Date dateLimite = calendar.getTime();
        if (!demande.getAvis().isEmpty() && !demande.getAvis().equals("") && demande.getAvis() != null && !demande.getAvis().equals(demande.getRaison()))
            motif = demande.getAvis();
        else
            motif = demande.getRaison();
        String message = "DEMANDE DE SUPPRESSION DE COMPTE \n" +
                "  - Motif : " + motif + " \n" +
                "  - Date de la demande : " + demande.getDateDemande().toString() + " \n\n" +
                "Votre demande a été bien prise en compte et est en cours de traitement. \n" +
                "Si ceci est une erreur de manipulation ou si vous changez d'avis vous avez la possibilité d'annuler cette demande. \n " +
                "Dans le cas contraire 7 jours après la réception de ce mail nous procèderons à la suppression de votre compte.\n" +
                "Date limite d'annulation de la demande: " + StringUtils.FormatDate(dateLimite) + ".\n" +
                "Meilleures salutations.\n" +
                "L'équipe JAWUPAY.";
        emailWs.setTo(partenaire.getEmail());
        emailWs.setSubject(title.toUpperCase());
        emailWs.setContent(message);
        emailWs.setContentIsHtml(isHtml);

        Boolean sent = notificationService.sendEmail(emailWs);
        if (sent)
            logger.info("Email envoyé avec succès à {}", partenaire.getEmail());

        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage("demande de suppression enregistrée");
        response.setCode(200);
        response.setData(null);
        return response;
    }

    public CommonResponseWs cancelDelete(Integer partenaireId) {
        CommonResponseWs response = new CommonResponseWs();
        Partenaire partenaire = partenaireRepository.findOneById(partenaireId);
        if (partenaire == null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("partenaire introuvable");
            response.setCode(401);
            response.setData(null);
            return response;
        }
        DemandeSuppressionPartenaire demande = demandeSuppressionPartenaireRepository.findByPartenaireIdAndStatus(partenaireId, StatutDemandeSuppression.EN_COURS.getValue());
        if (demande == null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("demande de suppression introuvable");
            response.setCode(401);
            response.setData(null);
            return response;
        }
        demande.setStatus(StatutDemandeSuppression.CANCELLED.getValue());
        demande = demandeSuppressionPartenaireRepository.save(demande);

        EmailWs emailWs = new EmailWs();
        String motif = "";
        String title = "Processus de suppression de compte annulé";
        Boolean isHtml = false;
        if (!demande.getAvis().isEmpty() && !demande.getAvis().equals("") && demande.getAvis() != null && !demande.getAvis().equals(demande.getRaison()))
            motif = demande.getAvis();
        else
            motif = demande.getRaison();
        String message = "ANNULATION DE LA SUPPRESSION DU COMPTE \n" +
                "Votre précédente demande de suppression de compte était constituée comme suit : \n" +
                "  - Motif : " + motif + " \n" +
                "  - Date de la demande : " + demande.getDateDemande().toString() + " \n\n" +
                "Elle a été annulée avec succès. Merci de continuer à nous faire confiance. \n" +
                "Meilleures salutations.\n" +
                "L'équipe JAWUPAY.";
        emailWs.setTo(partenaire.getEmail());
        emailWs.setSubject(title.toUpperCase());
        emailWs.setContent(message);
        emailWs.setContentIsHtml(isHtml);

        Boolean sent = notificationService.sendEmail(emailWs);
        if (sent)
            logger.info("Email envoyé avec succès à {}", partenaire.getEmail());
        
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage("demande de suppression annulée");
        response.setCode(200);
        response.setData(null);

        return response;
    }

    public CommonResponseWs isDeleteInProgress(Integer partenaireId) {
        CommonResponseWs response = new CommonResponseWs();
        Partenaire partenaire = partenaireRepository.findOneById(partenaireId);
        if (partenaire == null) {
            response.setStatus(WsOperationResponseStatus.failed);
            response.setMessage("partenaire introuvable");
            response.setCode(401);
            response.setData(null);
            return response;
        }
        DemandeSuppressionPartenaire demande = demandeSuppressionPartenaireRepository.findByPartenaireIdAndStatus(partenaireId, StatutDemandeSuppression.EN_COURS.getValue());
        if (demande == null) {
            response.setData(false);
            response.setMessage("pas de demande de suppression en cours");
        }
        else {
            response.setData(true);
            response.setMessage("demande de suppression en cours");
        }

        response.setStatus(WsOperationResponseStatus.success);
        response.setCode(200);

        return response;
    }
}
