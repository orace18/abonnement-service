package com.clubchretien.abonnement.authentication;

import com.clubchretien.abonnement.core.partenaire.services.MobilePartenaireAuthenticationService;
import com.owomi.commons.constants.MobilePartenaireConstant;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.auth.AuthenticationAuthorizationServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class AuthenticationService implements MobilePartenaireConstant {
    private static Logger logger = LogManager.getLogger(AuthenticationService.class);

    @Autowired
    AuthenticationAuthorizationServiceClient service;

    @Autowired(required = false)
    MobilePartenaireAuthenticationService mobilePartenaireAuthenticationService;

    @Autowired
    HttpServletRequest httpServletRequest;

    public SessionInfoWs getSessionFromHeader(HttpServletRequest httpServletRequest)  {
        return this.service.session(httpServletRequest);
    }

    public Partenaire getPartenaireFromHeader(HttpServletRequest httpServletRequest)  {
        SessionInfoWs session = this.service.session(httpServletRequest);
        if (session == null) return null;
        return this.mobilePartenaireAuthenticationService.findPartenaire(session);
    }

    public Boolean authorized(HttpServletRequest httpServletRequest)  {
        return this.service.authorized(httpServletRequest);
    }


}
