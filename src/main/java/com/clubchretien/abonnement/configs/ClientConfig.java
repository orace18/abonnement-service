package com.clubchretien.abonnement.configs;

import com.owomi.rest.client.abonnementClient.UserInfosClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {



    @Bean
    public UserInfosClient userInfosClient() {
        return new UserInfosClient();
    }
}