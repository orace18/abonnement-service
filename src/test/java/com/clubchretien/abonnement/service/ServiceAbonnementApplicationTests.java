package com.clubchretien.abonnement.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = ServiceAbonnementApplicationTests.TestConfiguration.class)
class ServiceAbonnementApplicationTests {

	@Configuration
	@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class
	})
	static class TestConfiguration {
		// Configuration de test minimale sans dépendances externes
	}

	@Test
	void contextLoads() {
		// Test minimal qui charge seulement la configuration de base
		// sans aucune dépendance vers database-service ou autres modules
	}

}
