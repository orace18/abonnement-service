package com.clubchretien.abonnement.configs;


import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User: BSCAKO
 * Date: 11/06/2021 Time: 19:28
 */


public class Router {

    public static final String ALL = "/*";
    public static final String[] WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    public static final String[] WHITELIST_CONTROLLER = {
            "/v1.0/firebase",
    };

    public static final String[] DOCS = {
            "swagger-ui",
            "api-docs"
    };

    public static final String[] ACTUATOR = {
            "actuator",
    };
    public static final String[] DOC_MODULES = {
            "/v**/**",
            "/abonnement/v**/**",
            "/countries/v**/**",
    };
    public static final String[] DOC_MODULES_ROOT_EXCLUSION = {
//			"/v**/firebase/**",
    };

    public static Boolean IsMatch(String chemin, String[] chemins) {
        return Stream.of(chemins).anyMatch(chemin::contains);
    }

    public static Boolean IsStartWith(String chemin, String[] chemins) {
        return Stream.of(chemins).anyMatch(chemin::startsWith);
    }

    public static Boolean IsDocumentation(String chemin) {
        return Stream.of(DOCS).anyMatch(chemin::contains);
    }

    public static Boolean IsActuator(String chemin) {
        return Stream.of(ACTUATOR).anyMatch(chemin::contains);
    }

    public static Boolean Authorized(String chemin) {
        return IsMatch(chemin, WHITELIST) || IsStartWith(chemin, WHITELIST_CONTROLLER)
                || IsDocumentation(chemin) || IsActuator(chemin)
                ;
    }

    public static Boolean IsFilterAuthorized(HttpServletRequest httpServletRequest) {
        String chemin = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());
        return Router.Authorized(chemin) || Router.IsInternalUrl(httpServletRequest);
    }

    public static String[] ExclusionDocModules(String root) {
        return Arrays.stream(Router.DOC_MODULES).filter(v -> !root.equalsIgnoreCase(v)).collect(Collectors.toList()).toArray(new String[0]);
    }

    public static Boolean IsInternalUrl(HttpServletRequest httpServletRequest) {
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest).replacePath(null).build().toUriString();
        boolean state = baseUrl.contains(".owomi.mylocal");
        if (state) return true;
        //if (baseUrl.contains("-staging.owomi.com")) return true;
        return false;
    }
}




