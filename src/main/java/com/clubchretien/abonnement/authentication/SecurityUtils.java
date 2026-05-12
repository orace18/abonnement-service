package com.clubchretien.abonnement.authentication;

import com.clubchretien.abonnement.authentication.uri.UriAuthorizationAccess;
import com.clubchretien.abonnement.core.partenaire.services.MobilePartenaireAuthenticationService;
import com.clubchretien.abonnement.firebase.utils.FireBaseUtils;
import com.owomi.commons.constants.TokenConstant;
import com.owomi.commons.enums.authentication.SessionTypeEnums;
import com.owomi.commons.ws.authentication.SessionClaimsWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.commons.ws.firebase.FirebaseCustomClaimsWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.utils.RequestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class SecurityUtils implements TokenConstant {
	private static Logger logger = LogManager.getLogger(SecurityUtils.class);

	@Autowired
	private Mapper mapper;
	@Autowired
	private UriAuthorizationAccess uriAuthorizationAccess;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private MobilePartenaireAuthenticationService mobilePartenaireAuthenticationService;


	public static String getAccessToken(HttpServletRequest httpServletRequest) {
		logger.info("════════════════════════════════════════════");
		logger.info("GET ACCESS TOKEN");
		logger.info("════════════════════════════════════════════");

		String token = RequestUtils.header(httpServletRequest, ACCESS_TOKEN);

		if (token == null || token.isEmpty()) {
			logger.warn("❌ Access token absent ou vide - Header attendu: {}", ACCESS_TOKEN);
			return null;
		}

		logger.info("✅ Access token trouvé - Longueur: {}", token.length());
		return token;
	}

	public Partenaire getPartenaireFromSession(SessionClaimsWs session) {
		if (session == null) return null;
		return this.mobilePartenaireAuthenticationService.findPartenaire(session);
	}

	public Partenaire getPartenaireFromHeader(HttpServletRequest httpServletRequest) {
		SessionClaimsWs session = this.getSessionClaimsFromHeader(httpServletRequest);
		if (session == null) return null;
		return this.getPartenaireFromSession(session);
	}

	public SessionClaimsWs getSessionClaimsFromHeader(HttpServletRequest httpServletRequest) {
		try {
			String token = getAccessToken(httpServletRequest);
			if (token == null) return null;

			FireBaseUtils fireBaseUtils = new FireBaseUtils(token);
			FirebaseCustomClaimsWs claims = fireBaseUtils.claims();
			if (claims == null) return null;
			logger.info("claims : " + claims);
			return claims.getSession();
			//FireBaseUtils fireBaseUtils = new FireBaseUtils(token);
			//return fireBaseUtils.claims().getSession();
		} catch (Exception e) {
			logger.info("Exception : " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public SessionInfoWs getSessionInfoFromHeader(HttpServletRequest httpServletRequest) {
		try {
			return this.authenticationService.getSessionFromHeader(httpServletRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Boolean authorized(HttpServletRequest httpServletRequest) {
		boolean authorized = this.authorized(httpServletRequest, 0);
		return authorized;
	}

	public Boolean authorized(SessionClaimsWs session, Integer minimalLevel) {
		if (session == null) return false;
		boolean blocked = session.getBlocked() != null && session.getBlocked();
		if (blocked) return false;
		return SessionTypeEnums.minimal(session.getType(), minimalLevel);
	}
// commentaire
	public Boolean authorized(HttpServletRequest httpServletRequest, Integer minimalLevel) {
		SessionClaimsWs session = this.getSessionClaimsFromHeader(httpServletRequest);
		SessionTypeEnums type  = SessionTypeEnums.parse(session.getType());
		String url = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
		String method = httpServletRequest.getMethod();
		logger.debug(" check authorized type {} url {} method {}", type, url, method);
		boolean uriAuthorised = this.uriAuthorizationAccess.authorized(type, url, method);
		boolean authorized = this.authorized(session, minimalLevel);
		logger.debug(" check authorized authorized {} uriAuthorised ", authorized, uriAuthorised);
		return authorized;  //&& uriAuthorised;
	}


}
