package com.clubchretien.abonnement.authentication.uri;

import com.clubchretien.abonnement.authentication.uri.definition.UriAuthorizationDefinition;
import com.google.gson.Gson;
import com.owomi.commons.enums.authentication.SessionTypeEnums;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Author Ibrahim ABLADON
 * Date : from 08/05/2020
 */

@Component
public class UriAuthorizationAccess {

    private static Logger logger = LogManager.getLogger(UriAuthorizationAccess.class);

    private Gson gson = new Gson();

    @Value("server.servlet.context-path")
    private String contextPath;


    private Map<SessionTypeEnums, UriCollection<String>> rolesMap = new HashMap<>();

    public UriAuthorizationAccess(HttpServletRequest request){
        this.rolesMap = UriAuthorizationDefinition.Definition(contextPath);
    }



    public boolean authorized(SessionTypeEnums type, String uri, String method) {
        UriCollection<String> uris = this.rolesMap.get(type);
        if (uris != null && uris.contains(uri, method)) return true;
        return SessionTypeEnums.minimal(type, SessionTypeEnums.max().getLevel());
    }

}
