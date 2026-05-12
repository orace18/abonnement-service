package com.clubchretien.abonnement.configs;

import com.owomi.jwt_lib.properties.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:3600000}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:86400000}")
    private Long refreshTokenExpiration;

    /**
     * Crée le bean JwtProperties manuellement
     */
    @Bean
    public JwtProperties jwtProperties() {
        // Essayer avec le constructeur par défaut puis setter les valeurs
        JwtProperties properties = new JwtProperties();

        // Utiliser la réflexion pour définir les champs privés
        try {
            setField(properties, "secret", secret);
            setField(properties, "accessTokenExpiration", accessTokenExpiration);
            setField(properties, "refreshTokenExpiration", refreshTokenExpiration);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la configuration de JwtProperties", e);
        }

        return properties;
    }

    /**
     * Méthode utilitaire pour définir un champ via réflexion
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = JwtProperties.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}