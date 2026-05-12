package com.clubchretien.abonnement.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {
        "com.clubchretien.abonnement",
        "com.owomi.database",
        "com.owomi.commons",
        "com.owomi.rest",
        "com.owomi.jwt_lib",
        "com.owomi.message",
        "com.owomi.security.services",
        "com.owomi.velocity",
        "com.owomi.file.manager",
})
@Configuration
@EnableAsync
@EnableScheduling
@EnableJms
@EnableCaching
@EntityScan(basePackages = {
        "com.owomi.database.entities",
        "com.clubchretien.abonnement.abonnement.relance",
})
@EnableJpaRepositories(basePackages = {
        "com.owomi.database.repositories",
        "com.clubchretien.abonnement.abonnement.repositories"

})
public class AbonnementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AbonnementServiceApplication.class, args);
    }
}
