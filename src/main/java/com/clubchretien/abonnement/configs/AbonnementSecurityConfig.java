package com.clubchretien.abonnement.configs;

import com.owomi.jwt_lib.configs.BaseSecurityConfig;
import com.owomi.jwt_lib.services.JwtService;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class AbonnementSecurityConfig extends BaseSecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**",
            "/v1.0/firebase/**",
            "/api/abonnements/callback",
            "/api/abonnements/callback/test"
    };

    public AbonnementSecurityConfig(JwtService jwtService, CustomJwtUtils customJwtUtils,
            UserDetailsService userDetailsService) {
        super(jwtService, customJwtUtils, userDetailsService);
    }

    // Petit commentaire
    @Override
    protected void configureAuthorization(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests()
                .antMatchers(AUTH_WHITELIST).permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic().disable()
                .formLogin().disable();
    }
}
