package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.exceptions.PermissionException;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.permission.InitPinOublier;
import com.owomi.commons.ws.permission.ValidatePinAuthorizationWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.permissionsService.PermissionsServiceClient;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static com.clubchretien.abonnement.constants.MobileConstant.*;
import static com.clubchretien.abonnement.utils.UtilsService.decrypt;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;


@Component
public class ResetPinService {

    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    PermissionsServiceClient permissionsServiceClient;
    @Autowired
    I18nFactory i18n;

    @Autowired
    DataServiceClient dataServiceClient;

    @Autowired
    HttpServletRequest request;

    @Autowired
    UserService userService;

    private static Logger logger = LogManager.getLogger(ResetPinService.class);

    public CommonResponseWs initPinOublier(InitPinOublier initPinOublier) {
        CommonResponseWs responseWs = permissionsServiceClient.initPinOublier(initPinOublier);
//        logger.debug("PIN OUBLIER RESPONSE :"+new Gson().toJson(responseWs));
        return responseWs;

    }

    public String createKey(HttpServletRequest request) throws OwomiException {
        PublicKeyWs key = dataServiceClient.createKey(request.getHeader("access-token"));
        return key.getPublicKey();
    }

    private CommonResponseWs returnNewKey() throws OwomiException {
        CommonResponseWs responseWs = new CommonResponseWs();
        String key = createKey(request);
        responseWs.setData(key);
        responseWs.setStatus(WsOperationResponseStatus.inProcess);
        responseWs.setCode(ErrorCodeEnums.PRIVATE_KEY_CODE_NOT_FOUND_ERROR.getErrorCode());
        return responseWs;

    }

    public String getPrivateKey(Integer idPartenaire, HttpServletRequest request) throws OwomiException {
        String privateKey = dataServiceClient.getPrivate(idPartenaire.longValue(), request.getHeader("access-token")).getPrivateKey();
        return privateKey;
    }

    public CommonResponseWs validatePinOublier(Partenaire partenaire,HashDataWs hashData) throws DataNotFoundException, AuthentificationException, OwomiException, PermissionException {
        CommonResponseWs responseWs = new CommonResponseWs();
        logger.info("starting pin validation");
        if (partenaire==null) throw new DataNotFoundException();
        String key =getPrivateKey(partenaire.getId(),request);
        if (key.equalsIgnoreCase(PRIVATE_KEY_CODE_NOT_FOUND)) {
            logger.info("regenerate key");
            return returnNewKey();
        }

        /**
         * decrypt hashOperation string to ValidatePinAuthorizationWs object
         */
        String operationString = decrypt(hashData.getHashData(),key);
        logger.info("decrypt data ############## : "+operationString);
//        ValidatePinAuthorizationWs authorizationWs = new ValidatePinAuthorizationWs();
        logger.info("starting desarialization ");
        ValidatePinAuthorizationWs authorizationWs = new Gson().fromJson(operationString,ValidatePinAuthorizationWs.class);

        if (authorizationWs==null) {
            throw  PermissionException.builder()
            .reference(partenaire.getTelephone())
            .errorCodeEnums(ErrorCodeEnums.PERMISSION_PIN_ERROR)
                    .detailMessage(ErrorCodeEnums.PERMISSION_PIN_ERROR.getErrorDescription())
                    .message("PERMISSION_PIN_ERROR")
                    .build();
        }
        logger.info("deserialize data ####### "+authorizationWs);
        responseWs = permissionsServiceClient.validatePinOublier(authorizationWs);
        return responseWs;
    }

    public CommonResponseWs getUserPendingPermission(Integer id){
        CommonResponseWs responseWs = permissionsServiceClient.getUserPermissions(id);
        return responseWs;
    }

    public CommonResponseWs getAuthorizationByToken(String token){
        CommonResponseWs responseWs = permissionsServiceClient.getAutorization(token);
        return responseWs;
    }
    public CommonResponseWs getPermissionByToken(String token){
        CommonResponseWs responseWs = permissionsServiceClient.getPermission(token);
        return responseWs;
    }

    //@Transactional
    public CommonResponseWs requestPin(RequestContextWs requestContext, String demandeurTelephone, TelephoneWs telephoneWs) throws DataNotFoundException, OwomiException {

        if(telephoneWs.getDestinataireTelephone().equals(demandeurTelephone)) throw new OwomiException("",ErrorCodeEnums.PARTENAIRE_NUMBER_ERROR,"");
        Partenaire destinataire = partenaireRepository.findOneByTelephone(telephoneWs.getDestinataireTelephone());
        EntityUtils.checkIfEntityIsNull(Partenaire.class,destinataire,"telephone",telephoneWs.getDestinataireTelephone());
        CommonResponseWs commonResponseWs = new CommonResponseWs();

        if (destinataire.getParent() == null) throw new OwomiException("",ErrorCodeEnums.PARTENAIRE_RESEAUTEUR_ERROR,"");// Il est reseauteur, appeler la fonction demande permission
            CreatePermissionWs createPermissionWs = new CreatePermissionWs();
            createPermissionWs.setDemandeurTelephone(demandeurTelephone);
            createPermissionWs.setDestinataireTelephone(telephoneWs.getDestinataireTelephone());
            createPermissionWs.setConsommateur(RESET_PIN_CONSOMMATEUR);
            createPermissionWs.setCommentaire(i18n.internationalize("request.pin.commentaire"));
            createPermissionWs.setObjet(RESET_PIN_OBJECT);
            createPermissionWs.setExpiration(RESET_PIN_EXPIRATION);
            commonResponseWs = permissionsServiceClient.createPermission(createPermissionWs);
            if(commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)){
                // La demande s est bien cree
                commonResponseWs.setStatus(WsOperationResponseStatus.success);
                //J envoie l alerte generalisee
                String message = i18n.internationalize("request.pin.message");
                permissionsServiceClient.alerte(message,demandeurTelephone, i18n.internationalize("MobileConstant.RESET_PIN_TITLE"));
            }

            return commonResponseWs;
    }

    public List<PermissionWs> getListePermissions (RequestContextWs requestContextWs,String numero) throws OwomiException {
        return permissionsServiceClient.getListPermissions(numero);
    }


    public CommonResponseWs responsePin(RequestContextWs requestContext, String pin, Partenaire partenaire, PermissionResponseWs permissionResponseWs,String demandeurTelephone) throws OwomiException {
        boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
        if(!statusPin) throw new OwomiException("",ErrorCodeEnums.CHECK_PIN_ERROR,"");
        CommonResponseWs commonResponseWs = permissionsServiceClient.reponsePin(permissionResponseWs);
        if(commonResponseWs.getStatus().equals(WsOperationResponseStatus.success) && permissionResponseWs.getReponse()){
            Partenaire demandeur = partenaireRepository.findOneByTelephone(demandeurTelephone);
            demandeur.setAutoriserResetPin(true);
            demandeur.setExpirationPin(new Timestamp(new Date().getTime() + SMS_EXPIRATION));
            partenaireRepository.save(demandeur);
            CreatePermissionWs createPermissionWs = new CreatePermissionWs();
            createPermissionWs.setDemandeurTelephone("");
            createPermissionWs.setDestinataireTelephone(demandeurTelephone);
            createPermissionWs.setConsommateur(RESET_PIN_CONSOMMATEUR);
            createPermissionWs.setCommentaire(i18n.internationalize("response.pin.commentaire"));
            createPermissionWs.setObjet(RESET_PIN_OBJECT);
            createPermissionWs.setExpiration(SMS_EXPIRATION);
            commonResponseWs = permissionsServiceClient.createPermissionSMS(createPermissionWs);
        }
        return commonResponseWs;
    }

    public CommonResponseWs responseSms(RequestContextWs requestContext, Partenaire partenaire, PermissionResponseWs permissionResponseWs) throws OwomiException {
        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.failed);
        CommonResponseWs commonResponseWs = permissionsServiceClient.reponseSms(permissionResponseWs);
        if(commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)){
            //Si c'est bon, je mets à jour le champ autoriserResetPin de partenaire à true
            partenaire.setAutoriserResetPin(true);
            partenaireRepository.save(partenaire);
        }
        return commonResponseWs;
    }

    public CommonResponseWs resetPin (RequestContextWs requestContext,Partenaire partenaire,String pin,String firebaseToken, Integer id) throws OwomiException, FirebaseAuthException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
        String result = decodedToken.getUid();
        UserRecord userRecord= FirebaseAuth.getInstance().getUser(result);
        logger.debug("user Record mail " + userRecord.getEmail() + "user Record phone number " + userRecord.getPhoneNumber());
        Partenaire partenaireChecked = partenaireRepository.findOneByTelephoneAndEmail(userRecord.getPhoneNumber().replaceFirst("[+]",""),userRecord.getEmail());
        if(partenaireChecked == null) throw new OwomiException("",ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR,"");
        if (!partenaire.getAutoriserResetPin() || partenaire.getExpirationPin().getTime()<=new Date().getTime() ) throw new OwomiException("",ErrorCodeEnums.RESET_PIN_NOT_ALLOWED_ERROR,"");
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(pin, salt);
        partenaire.setPin(hashed);
        partenaire.setAutoriserResetPin(false);
        partenaireRepository.save(partenaire);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        PermissionResponseWs permissionResponseWs = new PermissionResponseWs();
        permissionResponseWs.setId(id);
        permissionsServiceClient.reponseSms(permissionResponseWs);
        return commonResponseWs;
    }



    public CommonResponseWs modifierPin(RequestContextWs requestContext, Partenaire partenaire, ModifPinWs modifPinWs) throws OwomiException, FirebaseAuthException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(modifPinWs.getFirebaseToken());
        String result = decodedToken.getUid();

        UserRecord userRecord= FirebaseAuth.getInstance().getUser(result);
        //Partenaire partenaireChecked = partenaireRepository.findOneByTelephoneAndEmail(userRecord.getPhoneNumber().replaceFirst("[+]",""),userRecord.getEmail());

        //if(partenaireChecked == null) throw new OwomiException("",ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR,"");
        boolean statusPin = BCryptUtils.checkpw(modifPinWs.getPin(), partenaire.getPin());

        if(!statusPin) throw new OwomiException("",ErrorCodeEnums.CHECK_PIN_ERROR,"");

        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(modifPinWs.getNewPin(), salt);
        partenaire.setPin(hashed);
        partenaireRepository.save(partenaire);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return commonResponseWs;
    }

    public CommonResponseWs modifierPin(RequestContextWs requestContext, Partenaire partenaire, DataInWs dataInWs, HttpServletRequest httpServletRequest) throws OwomiException, FirebaseAuthException {
        CommonResponseWs commonResponseWs = new CommonResponseWs();
        PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()), httpServletRequest.getHeader("access-token"));
        if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND)) throw new OwomiException("Key Error", ErrorCodeEnums.KEY_ERROR, "Cle de cryptage introuvable");
        String data = decrypt(dataInWs.getData(), privateKeyWs.getPrivateKey());
        Type type = new TypeToken<ModifPinWs>(){}.getType();
        ModifPinWs modifPinWs = new Gson().fromJson(data, type);
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(modifPinWs.getFirebaseToken());
        String result = decodedToken.getUid();
        UserRecord userRecord= FirebaseAuth.getInstance().getUser(result);
        //Partenaire partenaireChecked = partenaireRepository.findOneByEmail(userRecord.getEmail());
        //if(partenaireChecked == null) throw new OwomiException("",ErrorCodeEnums.AUTHENTICATE_FIREBASE_ERROR,"");
        boolean statusPin = BCryptUtils.checkpw(modifPinWs.getPin(), partenaire.getPin());
        if(!statusPin) throw new OwomiException("",ErrorCodeEnums.CHECK_PIN_ERROR,"");
        String salt = BCryptUtils.gensalt();
        String hashed = BCryptUtils.hashpw(modifPinWs.getNewPin(), salt);
        partenaire.setPin(hashed);
        partenaireRepository.save(partenaire);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return commonResponseWs;
    }

}
