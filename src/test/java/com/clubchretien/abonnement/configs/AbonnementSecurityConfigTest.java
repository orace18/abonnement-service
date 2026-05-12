package com.clubchretien.abonnement.configs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AbonnementSecurityConfigTest {

    @Test
    void callbackJawupayEstAutoriseSansJwt() throws Exception {
        Field field = AbonnementSecurityConfig.class.getDeclaredField("AUTH_WHITELIST");
        field.setAccessible(true);

        String[] whitelist = (String[]) field.get(null);

        assertTrue(Arrays.asList(whitelist).contains("/api/abonnements/callback"));
    }
}
