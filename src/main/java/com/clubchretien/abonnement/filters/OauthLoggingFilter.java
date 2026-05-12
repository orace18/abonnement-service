package com.clubchretien.abonnement.filters;


import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.configs.Router;
import com.google.gson.Gson;
import com.owomi.commons.constants.TokenConstant;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;


public class OauthLoggingFilter implements Filter, TokenConstant {
	private static Logger logger = LogManager.getLogger(OauthLoggingFilter.class);
	private static String[] chemins = {
			"/swagger-ui.html",
			"/v1.0/payment-gateway/cybersource/order",
			"/v2.0/redirect-links/use",
			"/v1.0/partenaire/initinscription",
			"/v1.0/partenaire/inscription",
			"/v1.0/whitelist/phone/check",
			"/v1.0/version/owomi/check",
			"/v1.0/image/getByUrl",
			"/v1.0/image/upload/file",
			"/v1.0/partenaire/inscription/apple", "/v1.0/image/corporate/piece", "/v2.0/comptables/transferts-valider", "/v2.0/comptables/ventes", "/v1.0/image/corporate/photo",
			"/v1.0/partenaire/authenticate", "/v1.0/slider/get/slider/image", "/v1.0/resource/getByUrl", "/v2.0/partenaire/initinscription", "/v2.0/serviceInfo/check", "/trace", "/httptrace", "/actuator", "/resources", "/v1.0/api/callBack", "/v1.0/search/products/boutiques", "/v1.0/compensations/rollback",
			"/v1.0/shop/commande/owopay", "/v2.0/operations/validate_operation",
			"/v1.0/recharge/callback/owopay",
			"/v1.0/recharge/callback/core",
			"/v1.0/authentication/authenticate",
			"/v4.0/partenaire/inscription",
	};
	@Autowired
	UserService userService;
	@Autowired
	SecurityUtils securityUtils;

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String chemin = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
		logger.info(httpServletRequest.getMethod() + " Incomming request" + chemin);

		if (Arrays.asList(chemins).contains(chemin) || Router.IsFilterAuthorized(httpServletRequest)) {
			System.out.println("================== -> SWAGGER UI GOOD or " + chemin);
			chain.doFilter(request, response);
			return;
		}

//		boolean passed = RequestMethod.OPTIONS.name().equalsIgnoreCase(httpServletRequest.getMethod())
//				|| securityUtils.authorized(httpServletRequest);
//
//		if (!passed) {
//			this.unauthorized(httpServletRequest, response);
//			return;
//		}
//		logger.debug("Filter Header Corporate ==> PASSED");
		chain.doFilter(request, response);

    }

	private void unauthorized(HttpServletRequest httpServletRequest, ServletResponse response ) throws IOException {
		CommonResponseWs commonResponseWs = new CommonResponseWs(WsOperationResponseStatus.error, "", HttpStatus.UNAUTHORIZED.value(), null);
		commonResponseWs.setMessage(HttpStatus.UNAUTHORIZED.getReasonPhrase());
		commonResponseWs.setInfo(httpServletRequest.getServletPath());
		commonResponseWs.setReference(ACCESS_TOKEN);
		String errorResponse = new Gson().toJson(commonResponseWs);
		byte[] bytes = errorResponse.getBytes();
		((HttpServletResponse) response).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		((HttpServletResponse) response).setStatus(HttpStatus.UNAUTHORIZED.value());
		response.getOutputStream().write(bytes);
	}

    @Override
    public void destroy() {
    }
}
