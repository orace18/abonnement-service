package com.clubchretien.abonnement.authentication.uri.definition;

import com.clubchretien.abonnement.authentication.uri.UriCollection;
import com.owomi.commons.enums.authentication.SessionTypeEnums;

import java.util.HashMap;
import java.util.Map;

/**
 * Author Ibrahim ABLADON
 * Date : from 08/05/2020
 */

public class UriAuthorizationDefinition {

    public static Map<SessionTypeEnums, UriCollection<String>> Definition(String contextPath) {
        Map<SessionTypeEnums, UriCollection<String>> rolesMap = new HashMap<>();
        rolesMap.put(SessionTypeEnums.DEVICE, UriDeviceDefinition.Definition(contextPath));
        rolesMap.put(SessionTypeEnums.PHONE_PIN, UriPhonePinDefinition.Definition(contextPath));
        rolesMap.put(SessionTypeEnums.EMAIL, EMPTY());
        rolesMap.put(SessionTypeEnums.PHONE, EMPTY());
        rolesMap.put(SessionTypeEnums.EMAIL_PHONE, EMPTY());
        return rolesMap;
    }

    public static UriCollection<String> EMPTY() {
        UriCollection<String> urls = new UriCollection<>();
        return urls;
    }

}
