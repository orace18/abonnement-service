
package com.clubchretien.abonnement.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.NatureCorporateEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.communication.ValidFcmTokenWs;
import com.owomi.commons.ws.communication.WhatsappMessageOtpWs;
import com.owomi.database.entities.ActivityLog;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PartenaireCorporate;
import com.owomi.database.repositories.ActivityLogRepository;
import com.owomi.database.repositories.PartenaireCorporateRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.message.publisher.publishers.ActivitylogMessagePublisher;
import com.owomi.message.publisher.publishers.ActivitylogUserMessagePublisher;
import com.owomi.message.publisher.publishers.NotifMessagePublisher;
import com.owomi.rest.client.communicationServiceClient.CommunicationServiceClient;
import com.owomi.rest.client.notif.NotificationUtils;
import com.owomi.rest.client.storage.FileStorageServiceClient;
import com.owomi.velocity.clients.VelocityClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.clubchretien.abonnement.constants.MobileConstant.OWOMI_CODE_SUITE_2;
import static com.owomi.commons.constants.MobilePartenaireConstant.OWOMI_CODE;

@Component
public class NotificationService implements MobileConstant {

	public static final SimpleDateFormat ACTIVITY_LOG_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss");
	public static final String OWOMI_ACTIVITE = "activite";
	private static Logger logger = LogManager.getLogger(NotificationService.class);
	@Autowired(required = false)
	ActivitylogUserMessagePublisher activitylogUserMessagePublisher;
	@Autowired(required = false)
	PartenaireRepository partenaireRepository;
	@Autowired(required = false)
	VelocityClient velocityClient;
	@Autowired
	ActivityLogRepository activityLogRepository;
	@Autowired(required = false)
	NotifMessagePublisher notifMessagePublisher;
	@Autowired
	NotificationUtils notificationUtils;
	@Autowired(required = false)
	ActivitylogMessagePublisher activitylogMessagePublisher;
	@Autowired
	CommunicationServiceClient communicationServiceClient;
	@Autowired
	PartenaireCorporateRepository partenaireCorporateRepository;
	@Autowired
	FileStorageServiceClient fileStorageServiceClient;

	public CommonResponseWs createUser(Partenaire partenaire) {
		ActivityLogUserWs activityLogUserWs = new ActivityLogUserWs();
		activityLogUserWs.setApp(FirebaseAppEnums.MOBILE);
		activityLogUserWs.setFirebaseId(partenaire.getFirebaseid());
		activityLogUserWs.setFirebaseToken(partenaire.getFirebasetoken());
		activityLogUserWs.setEmail(partenaire.getEmail());
		activitylogUserMessagePublisher.sendForNewUser(activityLogUserWs);
		return new CommonResponseWs(WsOperationResponseStatus.success, null, null, null);
	}

	public CommonResponseWs createUser(ActivityLogUserWs activityLogUserWs) {
		activityLogUserWs.setApp(FirebaseAppEnums.MOBILE);
		activitylogUserMessagePublisher.sendForNewUser(activityLogUserWs);
		return new CommonResponseWs(WsOperationResponseStatus.success, null, null, null);
	}

	public CommonResponseWs updateUser(ActivityLogUserWs activityLogUserWs) {
		activityLogUserWs.setApp(FirebaseAppEnums.MOBILE);
		activitylogUserMessagePublisher.sendForNewUserToken(activityLogUserWs);
		return new CommonResponseWs(WsOperationResponseStatus.success, null, null, null);
	}

	public CommonResponseWs activityLogs(FirebaseAppEnums app, Partenaire partenaire, Integer page, Integer size) throws OwomiException {
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 404, null);
		return communicationServiceClient.list(app, partenaire.getEmail(), page, size);
	}

	public CommonResponseWs activityLogsMarkAsRead(List<UUID> ids) throws OwomiException {
		activitylogMessagePublisher.sendForMarkAsRead(ids);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

	public FirebaseAppEnums app(Corporate corporate) {
		if (corporate == null) return FirebaseAppEnums.MOBILE;
		if (corporate.getNatureCorporate() == null) return FirebaseAppEnums.MOBILE;
		NatureCorporateEnums nature = NatureCorporateEnums.parse(corporate.getNatureCorporate().getLibelle());
		if (nature == null) return FirebaseAppEnums.MOBILE;
		if (NatureCorporateEnums.MARCHAND.equals(nature)) return FirebaseAppEnums.MERCHANT;
		if (NatureCorporateEnums.RIDER.equals(nature)) return FirebaseAppEnums.RYDER;
		return FirebaseAppEnums.MOBILE;
	}

	private Partenaire partenaire(Corporate corporate) {
		if (corporate == null) return null;
		PartenaireCorporate pc = this.partenaireCorporateRepository.findOneByPk_Corporate(corporate);
		if (pc == null) return null;
		return pc.getPartenaire();
	}

	public void notificationAndAl(Corporate corporate, String title, String message) throws NotificationException {
		FirebaseAppEnums app = this.app(corporate);
		Partenaire partenaire = this.partenaire(corporate);
		this.notificationAndAl(app, partenaire, title, message);
	}

	public void notificationAndAl(FirebaseAppEnums type, Partenaire partenaire, String title, String message) throws NotificationException {
		this.notificationAndAl(type, partenaire, title, message, false, new HashMap<>());
	}

	public void notificationAndAl(FirebaseAppEnums type, Partenaire partenaire, String title, String message, Boolean priority, Map<String, String> data) throws NotificationException {

		logger.debug("send notificationAndAl app {} to email {} ", type, partenaire.getEmail());

		Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
		int id = 10;
		data.putIfAbsent("id", Integer.toString(id));
		if (FirebaseAppEnums.MOBILE.equals(type)) {
			ActivityLog activityLog = new ActivityLog();
			activityLog.setActeur(acteur);
			activityLog.setDestinateur(partenaire);
			activityLog.setObjet(title);
			activityLog.setContenu(this.velocityClient.defaultTemp(message));
			activityLog.setHeure(ACTIVITY_LOG_DATE_FORMATTER.format(new Date()));
			activityLog.setStatut(Boolean.FALSE);
			activityLog.setPriority(priority);
			activityLog.setProfil("");
			activityLog.setAction1(" ");
			//activityLog.setData(new Gson().toJson(data));
			activityLogRepository.save(activityLog);
		}

		ActivityLogRawWs comLogs = new ActivityLogRawWs();
		comLogs.setActorALEmail(OWOMI_ACTIVITE);
		comLogs.setRecipientAlApp(type);
		comLogs.setRecipientAlEmail(partenaire.getEmail());
		comLogs.setObjet(title);
		comLogs.setContenu(this.velocityClient.defaultTemp(message));
		comLogs.setData(new Gson().toJson(data));
		comLogs.setPriority(priority);
		comLogs.setRead(false);
		this.createActivityLogs(comLogs);

		NotifWs notification = new NotifWs();
		notification.setApp(type);
		notification.setEmail(partenaire.getEmail());
		notification.setTitle(title);
		notification.setBody(message);
		notification.setData(data);
		boolean state = this.sendNotification(notification);
		message = StringUtils.StripHTML(message);
		if (!state && FirebaseAppEnums.MOBILE.equals(type))
			notificationUtils.notification(id, partenaire.getFirebasetoken(), message, title, new HashMap<>());
	}


	private void createActivityLogs(ActivityLogRawWs ws) {
		ws.setTime(new Date().getTime());
		try {
			communicationServiceClient.createActivityLogs(ws);
		} catch (Exception e) {
			logger.debug("createActivityLogs error {} ", e.getMessage());
			activitylogMessagePublisher.send(ws);
		}
	}

	private Boolean sendNotification(NotifWs ws) {
		try {
			communicationServiceClient.sendNotification(ws);
			return true;
		} catch (Exception e) {
			logger.debug("createActivityLogs error {} ", e.getMessage());
			notifMessagePublisher.send(ws);
		}
		return false;
	}


	public void notificationAl(
			FirebaseAppEnums type, Partenaire partenaire, ActivityLogRawWs notification
	) throws NotificationException {
		logger.debug("send notification {} to  {}", type, notification);
		if (notification.getRecipientAlApp().equals(FirebaseAppEnums.MOBILE)) {
			Partenaire p = this.partenaireRepository.findOneByEmail(notification.getRecipientAlEmail());
			this.notificationAndAl(notification.getActorALApp(), p, notification.getObjet(), notification.getContenu(), notification.getPriority(), new HashMap<>());
			return;
		}
		activitylogMessagePublisher.send(notification);
	}

	public void sendSms(Partenaire partenaire, String subject, String sms) throws OwomiException {
		logger.debug("send notificationSms {} to  {}", sms, partenaire.getTelephone());
		this.sendSms(partenaire.getTelephone(), sms);
		try {
			EmailWs ws = new EmailWs();
			ws.setTo(partenaire.getEmail());
			ws.setContent(sms);
			ws.setSubject(subject);
			this.communicationServiceClient.sendEmail(ws);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendOtp(String telephone, String otp, Boolean sendWhatsapp) throws OwomiException {
		logger.debug("send sendOtp {} to  {}", telephone, telephone);
		sendWhatsapp = sendWhatsapp != null && sendWhatsapp;
		if (!sendWhatsapp) otp = OWOMI_CODE + otp + OWOMI_CODE_SUITE_2;
		if (sendWhatsapp) this.sendWhatsapp(telephone, otp);
		else this.sendSms(telephone, otp);
	}

	public void sendWhatsapp(String telephone, String otp) throws OwomiException {
		logger.debug("send sendWhatsapp {} to  {}", otp, telephone);
		try {
			WhatsappMessageOtpWs ws = WhatsappMessageOtpWs.builder().otp(otp).to(telephone).build();
			this.communicationServiceClient.sendWhatsappOtp(ws);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendSms(String telephone, String sms) throws OwomiException {
		logger.debug("send notificationSms {} to  {}", sms, telephone);
		try {
			EmailWs ws = new EmailWs();
			ws.setTo(telephone);
			ws.setContent(sms);
			ws.setSubject(sms);
			this.communicationServiceClient.sendSms(ws);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Boolean validateFcm(String token) {
		logger.debug("validateFcm {} ", token);
		try {
			CommonResponseWs commonResponseWs = this.communicationServiceClient.validateFcmToken(token);
			Gson gson = new Gson();
			ValidFcmTokenWs ws = gson.fromJson(gson.toJson(commonResponseWs.getData()), ValidFcmTokenWs.class);
			return ws.getState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void notificationTunnelInstallationApp(Partenaire partenaire, Integer parentId) throws NotificationException {
		try {
			String title = "Prospect : Installation Application ";
			String message = "Votre propect avec l'email : " + partenaire.getEmail();
			message += " vient d'installer l'application owomi. ";
			Partenaire parrain = this.partenaireRepository.findOneById(parentId);
			this.notificationAndAl(FirebaseAppEnums.MOBILE, parrain, title, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Boolean sendEmail(EmailWs ws) {
		try {
			communicationServiceClient.sendEmail(ws);
			return true;
		} catch (Exception e) {
			logger.debug("createActivityLogs error {} ", e.getMessage());
		}
		return false;
	}

	public void welcomeApp(Partenaire partenaire) {
		String title = "Bienvenue sur JawuPay";
		String message = "Bienvenue sur <strong>JawuPay</strong><br/><br/>" +
				"Nous sommes heureux de vous accueillir sur la plus grande plateforme de Finance Amicale en Afrique. <br/><br/>" +
				"Achète et paie <strong>7 jours plus tard</strong>, c’est notre accompagnement pour vous faciliter le quotidien. <br/><br/>" +
				"Utilisez <strong>JawuPay</strong> au quotidien pour payer vos marchands et devenez fiable afin de bénéficier de services premium, tels que les abonnements à crédit.<br/><br/>" +
				"Parlez-en à votre Pasteur, à votre Imam, à votre Employeur, ils peuvent augmenter votre fiabilité et celle de vos proches à bénéficier de nos services premium.<br/><br/>" +
				"JawuPay, tu es quelqu’un d’important, <br/>" +
				"JawuPay, un monde amical. <br/>";
		try {
			this.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String url = "IMAGES/LOGO/63a56360-d52b-455a-9588-8b102c67c25a-1722498668011.jpeg";
		Map<String, String> base64 = this.fileStorageServiceClient.readBase64(url);
		String bytes = base64.getOrDefault("bytes", null);
		String compressed = base64.getOrDefault("compressed", bytes);
		String extension = base64.getOrDefault("extension", "png");
		EmailWs ws = new EmailWs();
		ws.setTo(partenaire.getEmail());
		ws.setSubject(title);
		ws.setContent(message);
		ws.setContentIsHtml(true);
		EmailAttachmentWs attachment = new EmailAttachmentWs();
		attachment.setName("bienvenue."+extension);
		attachment.setUrl(url);
		ws.setAttachments(Arrays.asList(attachment));
		this.sendEmail(ws);
	}

	public void notification(Integer code, Partenaire partenaire, String title, String message, boolean priority, Map<String, Object> map) {
		try {
			logger.debug("send notificationAndAl to " + partenaire.getEmail());
			map.putIfAbsent("id", Integer.toString(code));
			Gson gson = new Gson();
			Type listType = new TypeToken<Map<String, String>>() {}.getType();
			Map<String, String> data = gson.fromJson(gson.toJson(map), listType);
			this.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message, priority, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

   