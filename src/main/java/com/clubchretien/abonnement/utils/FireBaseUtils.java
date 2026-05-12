package com.clubchretien.abonnement.utils;//package com.owomi.mobile.service.utils;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseToken;
//import com.owomi.commons.enums.ErrorCodeEnums;
//import com.owomi.commons.exceptions.ExceptionUtils;
//import com.owomi.commons.exceptions.OwomiException;
//import com.owomi.commons.ws.RequestContextWs;
//import com.owomi.mobile.service.i18n.I18nFactory;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.HashMap;
//
//public class FireBaseUtils {
//    private static Logger logger = LogManager.getLogger(FireBaseUtils.class);
//    private String token;
//    private String firebasId;
//    private OwomiException owomiException;
//
//    @Autowired
//    I18nFactory i18n;
//
//    public FireBaseUtils(RequestContextWs requestContext, String token) {
//        this.token = token;
//    }
//
//    public String getFireBaseId(RequestContextWs requestContext) throws OwomiException {
//        String result = null;
//        try {
//            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
//            result = decodedToken.getUid();
//        } catch (Exception exception) {
//            String errorMessage = ExceptionUtils.getExceptionStackTrace(exception);
//            logger.debug("Exception throw is ==>" + errorMessage);
//            HashMap<String, Object> data = new HashMap<>();
//            data.put("token", token);
//            logger.debug("Exception occured when I tried to get Id firebase with this token ==> " + token);
//            owomiException = new OwomiException(i18n.internationalize("firebase.id.failed"), ErrorCodeEnums.FIREBASE_TOKEN_ERROR, errorMessage, data);
//        }
//        if (owomiException != null)
//            throw owomiException;
//        return result;
//    }
//
//    public String getFireBaseEmail(RequestContextWs requestContext) throws OwomiException {
//        String result = null;
//        try {
//            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
//            result = decodedToken.getEmail();
//        } catch (Exception exception) {
//            String errorMessage = ExceptionUtils.getExceptionStackTrace(exception);
//            logger.debug("Exception throw is ==>" + errorMessage);
//			HashMap<String, Object> data = new HashMap<>();
//			data.put("token", token);
//			logger.debug("Exception occured when I tried to get Id firebase with this token ==> " + token);
//			owomiException = new OwomiException(i18n.internationalize("firebase.id.failed"), ErrorCodeEnums.FIREBASE_TOKEN_ERROR, errorMessage, data);
//		}
//		if (owomiException != null)
//			throw owomiException;
//		return result;
//	}
//
//	public String getFireBaseTelephone(RequestContextWs requestContext) throws OwomiException {
//		String result = null;
//		try {
//			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
//			String uid = decodedToken.getUid();
//			result = FirebaseAuth.getInstance().getUser(uid).getPhoneNumber();
//		} catch (Exception exception) {
//			String errorMessage = ExceptionUtils.getExceptionStackTrace(exception);
//			logger.debug("Exception throw is ==>" + errorMessage);
//			HashMap<String, Object> data = new HashMap<>();
//			data.put("token", token);
//			logger.debug("Exception occured when I tried to get Id firebase with this token ==> " + token);
//			owomiException = new OwomiException(i18n.internationalize("firebase.id.failed"), ErrorCodeEnums.FIREBASE_TOKEN_ERROR, errorMessage, data);
//		}
//		if (owomiException != null) return null;
//		return result;
//	}
//
//	public String getToken() {
//		return token;
//	}
//
//	public void setToken(String token) {
//		this.token = token;
//	}
//
//	public String getFirebasId() {
//        return firebasId;
//    }
//
//    public void setFirebasId(String firebasId) {
//        this.firebasId = firebasId;
//    }
//}
