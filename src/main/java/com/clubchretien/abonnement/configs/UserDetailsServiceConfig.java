package com.clubchretien.abonnement.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

@Configuration
public class UserDetailsServiceConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            if (username == null || username.isEmpty()) {
                throw new UsernameNotFoundException("Username cannot be null or empty");
            }
            return new User(username, "", Collections.emptyList());
        };
    }
}
