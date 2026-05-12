package com.clubchretien.abonnement.firebase.startup;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.annotations.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class FireBaseStartup implements ApplicationListener<ApplicationReadyEvent> {

	private static Logger logger = LogManager.getLogger(FireBaseStartup.class);

	@Value("${firebase.service.token.file}")
	private String firebaseServiceTokenFile;

	@Value("${firebase.config.url}")
	private String fileUrl;

	@Override
	public void onApplicationEvent(@NotNull ApplicationReadyEvent applicationReadyEvent) {
		logger.debug("####################### START FIREBASE PROCESS #######################");
		try {
			InputStream serviceAccountStream = new ClassPathResource(firebaseServiceTokenFile).getInputStream();
			GoogleCredentials googleCredentials = GoogleCredentials.fromStream(serviceAccountStream);
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(googleCredentials).setDatabaseUrl(fileUrl).build();
			FirebaseApp.initializeApp(options);
		} catch (Exception exception) {
			logger.error("Exception occured when I tried to get firebase configuration file from file " + firebaseServiceTokenFile, exception);
		}
		logger.debug(" ####################### END FIREBASE PROCESS #######################");

	}
}
