package com.clubchretien.abonnement.services;

import com.google.gson.Gson;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.HashDataWs;
import com.owomi.commons.ws.PublicKeyWs;
import com.owomi.commons.ws.permission.InitPinOublier;
import com.owomi.commons.ws.permission.ValidatePinAuthorizationWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.permissionsService.PermissionsServiceClient;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import static com.clubchretien.abonnement.utils.UtilsService.decrypt;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;

@Service
public class PermissionService {
    private static Logger logger = LogManager.getLogger(PermissionService.class);

    @Autowired
    DataServiceClient dataServiceClient;

    @Autowired
    HttpServletRequest request;

    @Autowired
    UserService userService;

    @Autowired
    PermissionsServiceClient permissionsServiceClient;

    public  CommonResponseWs initPinOublier(InitPinOublier initPinOublier){
        CommonResponseWs responseWs =  permissionsServiceClient.initPinOublier(initPinOublier);
//        logger.debug("PIN OUBLIER RESPONSE :"+new Gson().toJson(responseWs));
        return responseWs;

    }



    public CommonResponseWs validatePinOublier(Partenaire partenaire,HashDataWs hashData) throws DataNotFoundException, AuthentificationException, OwomiException {
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

        logger.info("deserialize data ####### "+authorizationWs.getToken());
        responseWs = permissionsServiceClient.validatePinOublier(authorizationWs);
        return responseWs;
    }

    public CommonResponseWs getUserPermission(Integer id){
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

	public CommonResponseWs getUserAuths(Integer id) {
		CommonResponseWs responseWs = permissionsServiceClient.getUserAuths(id);
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
		responseWs.setMessage("RE-ENCRYPTION REQUIRED WITH THE NEW GIVEN KEY");
		return responseWs;

	}

    public String getPrivateKey(Integer idPartenaire, HttpServletRequest request) throws OwomiException {
		String privateKey = dataServiceClient.getPrivate(idPartenaire.longValue(), request.getHeader("access-token")).getPrivateKey();
        return privateKey;
    }

    public CommonResponseWs initPinOublierStep2(Partenaire partenaire) {
        CommonResponseWs responseWs = permissionsServiceClient.validatePinOublierPermission(partenaire.getId());
        return responseWs;
    }

    public CommonResponseWs validatePinOublierPermission(Partenaire partenaire,HashDataWs hashData) throws DataNotFoundException, AuthentificationException, OwomiException {
        CommonResponseWs responseWs = new CommonResponseWs();
        logger.info("starting permission validation");
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

        logger.info("deserialize data ####### "+authorizationWs.getToken());
//        responseWs = permissionsServiceClient.validatePinOublierPermission(authorizationWs);
        return responseWs;
    }

}
