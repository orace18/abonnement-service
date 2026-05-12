package com.clubchretien.abonnement.authentication.uri.definition;

import com.clubchretien.abonnement.authentication.uri.UriCollection;

/**
 * Author Ibrahim ABLADON
 * Date : from 08/05/2020
 */

public class UriPhonePinDefinition {

    public static UriCollection<String> Definition(String contextPath) {
        UriCollection<String> urls = new UriCollection<>();
        urls.addAll(UriDeviceDefinition.Definition(contextPath));
        urls.add(contextPath + "/v2.0/validation/list/manager");
        return urls;
    }

}
