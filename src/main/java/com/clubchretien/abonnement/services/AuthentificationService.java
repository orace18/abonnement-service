package com.clubchretien.abonnement.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.owomi.commons.enums.AuthenticationAccessTokenTypeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PartenaireWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@Component
public class AuthentificationService {
	private static Logger logger = LogManager.getLogger(AuthentificationService.class);

	@Autowired
	AuthentificationUtils authentificationUtils;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	NotificationService notificationService;

	@Autowired
	ResetPinMatchingService matchingService;


	@Autowired
	Mapper mapper;

	public CommonResponseWs tokenInfo(String token) {
		Map<String, Object> result = new HashMap<>();
		try {
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
			String uid = decodedToken.getUid();
			UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
			result.put("uid", uid);
			result.put("userRecord", userRecord);
			logger.debug("informations from userRecord Tel is " + userRecord.getPhoneNumber() + " email is " + userRecord.getEmail());
			return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponseWs(WsOperationResponseStatus.failed, "", 200, result);
	}

	public String getHeaderToken(HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader(ACCESS_TOKEN);
		if (StringUtils.isNull(token)) {
			Cookie[] cookies = httpServletRequest.getCookies();
			if (cookies == null || cookies.length == 0) return null;
			List<Cookie> cookiesList = Arrays.asList(cookies);
			Cookie cookie = cookiesList.stream().filter(item -> item.getName().equals(ACCESS_TOKEN)).findFirst().orElse(null);
			if (cookie == null) return null;
			token = cookie.getValue();
		}
		System.out.println(" TOKEN VALUE =======> " + token);
		return token;
	}

	public Partenaire getPartenaireFirebase(HttpServletRequest httpServletRequest) {
		String token = getHeaderToken(httpServletRequest);
		return this.getPartenaireFirebase(token);
	}

	private Partenaire getPartenaireFirebase(String token) {
		if (token == null) return null;
		try {
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
			String result = decodedToken.getUid();
			UserRecord userRecord = FirebaseAuth.getInstance().getUser(result);
			logger.debug("informations from userRecord Tel is " + userRecord.getPhoneNumber() + " email is " + userRecord.getEmail());
			return partenaireRepository.findOneByEmail(userRecord.getEmail());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private Partenaire getPartenaireMobile(HttpServletRequest httpServletRequest) {
		try {
			return this.userService.getPartenaire(httpServletRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Partenaire getPartenaireMobile(String token) {
		try {
			return this.userService.getPartenaire(token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Boolean checkBackOfficeToken(String token) {
		try {
			return this.authentificationUtils.checkTokenBackOffice(token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private Boolean validateFcmToken(String token) {
		return this.notificationService.validateFcm(token);
	}

	public CommonResponseWs checkAccessToken(String type, String token) {
		Partenaire partenaire = null;
		AuthenticationAccessTokenTypeEnums tokenType = AuthenticationAccessTokenTypeEnums.parse(type);
		if (AuthenticationAccessTokenTypeEnums.FIREBASE_TOKEN.equals(tokenType))
			partenaire = this.getPartenaireFirebase(token);
		if (AuthenticationAccessTokenTypeEnums.OAUTH_MOBILE.equals(tokenType))
			partenaire = this.getPartenaireMobile(token);
		if (AuthenticationAccessTokenTypeEnums.FCM_TOKEN.equals(tokenType) && validateFcmToken(token))
			return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
		if (AuthenticationAccessTokenTypeEnums.OAUTH_BACKOFFICE.equals(tokenType) && checkBackOfficeToken(token))
			return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		PartenaireWs result = mapper.map(partenaire, PartenaireWs.class);
		CommonResponseWs commonResponseWs = new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
		commonResponseWs.setId(partenaire.getId());
		return commonResponseWs;
	}

	public CommonResponseWs authenticate() {
		Partenaire partenaire = this.getPartenaireFirebase(httpServletRequest);
		if (partenaire == null) partenaire = this.getPartenaireMobile(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		PartenaireWs result = mapper.map(partenaire, PartenaireWs.class);
		CommonResponseWs commonResponseWs = new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
		commonResponseWs.setId(partenaire.getId());
		return commonResponseWs;
	}

	public Partenaire getPartenaire() {
		Partenaire partenaire = this.getPartenaireFirebase(httpServletRequest);
		if (partenaire == null) partenaire = this.getPartenaireMobile(httpServletRequest);
        return partenaire;
	}
}
