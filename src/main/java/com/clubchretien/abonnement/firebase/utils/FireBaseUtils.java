package com.clubchretien.abonnement.firebase.utils;

import com.clubchretien.abonnement.firebase.exceptions.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.firebase.FirebaseCustomClaimsWs;
import com.owomi.commons.ws.storage.UploadWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class FireBaseUtils {
	private static Logger logger = LogManager.getLogger(FireBaseUtils.class);
	private String token;

	public FireBaseUtils(String token) {
		this.token = token;
	}

	public static UploadWs getPicture(String url) {
		if (url == null) return null;
		return UploadWs.builder().type("external").url(url).path(url).name(url).build();
	}

	public FirebaseToken firebaseToken() throws FirebaseException {
		try {
			return FirebaseAuth.getInstance().verifyIdToken(token);
		} catch (Exception exception) {
			String errorMessage = exception.getMessage();
			logger.debug("Exception throw is ==>" + errorMessage);
			HashMap<String, Object> data = new HashMap<>();
			data.put("token", token);
			logger.debug("Exception occured when I tried to get Id firebase with this token ==> " + token);
			throw new FirebaseException("Erreur lors de la rÃ©cupÃ©ration de Firebase ID ", exception);
		}
	}

	public String getFireBaseId() throws OwomiException {
		return this.firebaseToken().getUid();
	}

	public String getFireBaseEmail() throws OwomiException {
		return this.firebaseToken().getEmail();
	}

	public UserRecord userRecord() throws OwomiException {
		try {
			String uid = this.getFireBaseId();
			logger.debug("userRecord uid {}", uid);
			return FirebaseAuth.getInstance().getUser(uid);
		} catch (Exception ignored) {
		}
		return null;
	}

	public String getFireBaseTelephone() {
		try {
			FirebaseToken decodedToken = this.firebaseToken();
			String uid = decodedToken.getUid();
			logger.debug("getFireBaseTelephone uid {}", uid);
			UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
			logger.debug("getFireBaseTelephone getPhoneNumber {}", userRecord.getPhoneNumber());
			return userRecord.getPhoneNumber().trim().replaceAll("\\+", "");
		} catch (Exception ignored) {
		}
		return null;
	}

	public String getPictureUrl() {
		try {
			FirebaseToken decodedToken = this.firebaseToken();
			String uid = decodedToken.getUid();
			logger.debug("getPictureUrl uid {}", uid);
			UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
			logger.debug("getPictureUrl getPhotoUrl {}", userRecord.getPhotoUrl());
			return userRecord.getPhotoUrl();
		} catch (Exception ignored) {
		}
		return null;
	}

	public FirebaseCustomClaimsWs claims() throws OwomiException {
		FirebaseToken decodedToken = this.firebaseToken();
		if (decodedToken == null) return null;
		return FirebaseCustomClaimsWs.From(decodedToken.getClaims());
	}

	public UploadWs getPicture() {
		String url = this.getPictureUrl();
		return getPicture(url);
	}


	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
