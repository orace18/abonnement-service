package com.clubchretien.abonnement.services;

import com.owomi.commons.constants.MobilePartenaireConstant;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PartenaireCodeOTP;
import com.owomi.database.entities.Validation;
import com.owomi.database.repositories.BoutiqueShopizerRepository;
import com.owomi.database.repositories.PartenaireCodeOTPRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.ValidationRepository;
import com.owomi.rest.client.notif.NotificationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.clubchretien.abonnement.constants.MobileConstant.OWOMI_CODE_SUITE_2;

@Service
public class ReinitializationPinService implements MobilePartenaireConstant {

	private static Logger logger = LogManager.getLogger(ReinitializationPinService.class);
	@Autowired
	private NotificationService notificationService;
	@Autowired
	private PartenaireRepository partenaireRepository;
	@Autowired
	private PartenaireCodeOTPRepository partenaireCodeOTPRepository;
	@Autowired
	private ValidationRepository validationRepository;
	@Autowired
	private NotificationUtils notificationUtils;
	@Autowired
	ValidationService validationService;
	@Autowired
	BoutiqueShopizerRepository boutiqueShopizerRepository;


	public CommonResponseWs requestResetPinEmail(String email) throws OwomiException {
		Partenaire partenaire = this.partenaireRepository.findOneByEmail(email);
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 404, null);
		boolean authorized = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
		logger.debug("requestResetPinEmail partenaire {} authorized {}", partenaire.getId(), authorized);
		if (authorized)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "redirect : reset pin", 202, null);
		PartenaireCodeOTP codeOTP = this.createCodeOTP(partenaire);
		try {
			String title = "Réinitialisation de pin";
			String message = "Vous avez fait une demande de reinitialisation de votre pin depuis l'application Owomi. " +
					"Le code de verification est : " + codeOTP.getCode()
					+ ". Si vous n'êtes pas à l'origine de la demande bien vouloir ignorer le message.";
			notificationUtils.notificationEmail(email, title, message);
			Partenaire parent = partenaire.getParent();
			if (parent != null) {
				email = parent.getEmail();
				title = "Réinitialisation de pin de votre downline";
				message = "Votre downline " + partenaire.getNom() + " " + partenaire.getPrenoms() +
						" vient de faire une demande de reinitialisation de son pin depuis l'application Owomi. "
				;
				notificationUtils.notificationEmail(email, title, message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "demande soumis avec succes. checker votre mail", 200, null);
	}

	public CommonResponseWs requestResetPinTelephone(String telephone) throws OwomiException {
		Partenaire partenaire = this.partenaireRepository.findOneByTelephone(telephone);
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 404, null);
		boolean authorized = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
		logger.debug("requestResetPinEmail partenaire {} authorized {}", partenaire.getId(), authorized);
		if (authorized)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "redirect : reset pin", 202, null);
		PartenaireCodeOTP codeOTP = this.createCodeOTP(partenaire);
		try {
			String sms = OWOMI_CODE + codeOTP.getCode() + OWOMI_CODE_SUITE_2;
			String objet = "Code OTP Réinitialisation Pin";
			this.notificationService.sendSms(partenaire, objet, sms);
			Partenaire parent = partenaire.getParent();
			if (parent != null) {
				String title = "Réinitialisation de pin de votre downline";
				String message = "Votre downline " + partenaire.getNom() + " " + partenaire.getPrenoms() +
						" vient de faire une demande de reinitialisation de son pin depuis l'application Owomi. ";
				notificationUtils.notificationEmail(parent.getEmail(), title, message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new CommonResponseWs(WsOperationResponseStatus.success, "demande soumis avec succes. checker votre mail", 200, null);
	}

	public CommonResponseWs requestResetPinCertification(Partenaire partenaire, String code) {
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 404, null);
		boolean authorized = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
		if (authorized)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "redirect : reset pin", 202, null);
		if (partenaire.getPinValidationId() != null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "en attente de validation", 206, null);
		PartenaireCodeOTP codeOTP = this.findCodeOtp(partenaire, code);
		if (codeOTP == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "code otp null or expired", 205, null);
		Validation validation = this.sendInValidationDemande(codeOTP);
		partenaire.setPinValidationId(validation.getId());
		this.partenaireRepository.save(partenaire);
		return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
	}

	private Validation sendInValidationDemande(PartenaireCodeOTP codeOTP) {
		Partenaire partenaire = codeOTP.getPartenaire();
		Validation validation = this.validationRepository.findByIdrefAndReference(
				codeOTP.getId().intValue(), PartenaireCodeOTP.class.getSimpleName()
		);
		if (validation == null) validation = new Validation();
		validation.setReference(PartenaireCodeOTP.class.getSimpleName());
		validation.setReferenceId(codeOTP.getId().intValue());
		validation.setMotifassistant(partenaire.getTelephone());
		validation.setTableref(Partenaire.class.getSimpleName());
		validation.setStatut(0);
		validation.setIdref(partenaire.getId());
		validation.setNature("Demande de reinitialisation du pin");
		validation.setTicketvalidation("reset-pin");
		validation.setDateEnvoi(new Timestamp(System.currentTimeMillis()));
		validation.setMotifreceveur(partenaire.getNom() + " " + partenaire.getPrenoms());
		validation = validationRepository.save(validation);
		this.validationService.inSendValidation(partenaire, validation);
		return validation;
	}


	public CommonResponseWs resetPin(Partenaire partenaire, String pin) {
		if (partenaire == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 404, null);
		boolean authorized = partenaire.getAutoriserResetPin() != null && partenaire.getAutoriserResetPin();
		if (!authorized)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "unauthorized reset pin", 401, null);
		Date now = Date.from(Instant.now());
		Date expiration = now;
		if (partenaire.getExpirationPin() != null)
			expiration = new Date(partenaire.getExpirationPin().getTime());
		boolean expired = expiration.after(now);
		if (expired) {
			partenaire.setAutoriserResetPin(false);
			partenaire.setPinValidationId(null);
			partenaire.setExpirationPin(new Timestamp(now.getTime()));
			partenaire = partenaireRepository.save(partenaire);
			return new CommonResponseWs(WsOperationResponseStatus.failed, "expired time for reset pin. make a new request", 408, null);
		}
		String salt = BCryptUtils.gensalt();
		String hashed = BCryptUtils.hashpw(pin, salt);
		partenaire.setPin(hashed);
		partenaire.setSalt(salt);
		partenaire.setAutoriserResetPin(false);
		partenaire.setPinValidationId(null);
		partenaire.setExpirationPin(new Timestamp(now.getTime()));
		partenaire = partenaireRepository.save(partenaire);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}


	private PartenaireCodeOTP findCodeOtp(Partenaire partenaire, String value) {
		return this.partenaireCodeOTPRepository.findOneByCodeAndPartenaireAndExpirationDateGreaterThanEqual(
				value, partenaire, Date.from(Instant.now())
		);
	}

	public PartenaireCodeOTP createCodeOTP(Partenaire p) throws OwomiException {
		String generated = StringUtils.generateCodeVerification();
		Instant now = Instant.now();
		Instant expiration = now.plus(2, ChronoUnit.MINUTES);
		PartenaireCodeOTP code = this.partenaireCodeOTPRepository.findOneByCodeAndPartenaireAndExpirationDateGreaterThanEqual(
				generated, p, Date.from(now)
		);
		if (code == null) code = new PartenaireCodeOTP();
		code.setCode(generated);
		code.setPartenaire(p);
		code.setRecordDate(Date.from(now));
		code.setExpirationDate(Date.from(expiration));
		code = this.partenaireCodeOTPRepository.save(code);
		return code;
	}

}
