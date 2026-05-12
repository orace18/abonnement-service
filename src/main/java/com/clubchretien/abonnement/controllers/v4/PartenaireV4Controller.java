package com.clubchretien.abonnement.controllers.v4;

import com.clubchretien.abonnement.services.PartenaireAuthenticationService;
import com.clubchretien.abonnement.ws.CreatePartenaireStepTwoWss;
import com.google.firebase.auth.FirebaseAuthException;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("v4.0/partenaire")
@Tag(name = "Partenaire", description = "Partenaire")
public class PartenaireV4Controller {
	private static Logger logger = LogManager.getLogger(PartenaireV4Controller.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireAuthenticationService partenaireAuthenticationService;

	//@PostMapping("/initinscription")
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStep(
			@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs
	) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireAuthenticationService.initialSubscription(requestContext, createPartenaireStepOneWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/inscription")
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStepNew(@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireAuthenticationService.initialSubscription(requestContext, createPartenaireStepOneWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PutMapping("/inscription")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CreatePartenaireResponseWs> initialSubscriptionApple(@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		CreatePartenaireResponseWs responseWs =  partenaireAuthenticationService.initialSubscriptionApple(requestContext,createPartenaireStepOneWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/complementinfo")
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireSecondStep(@RequestBody CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws AuthentificationException, OwomiException, FileUploadException, DataNotFoundException, IOException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireAuthenticationService.secondStepSubscription(requestContext, partenaire, createPartenaireStepTwoWss);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@GetMapping("/info")
	public CommonResponseWs infoPartenaire() throws Exception {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		return partenaireAuthenticationService.infoPartenaire(partenaire);
	}

	@PostMapping("/pininfo")
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireThirdStepCrypted(@RequestBody DataInWs dataInWs) throws AuthentificationException, NotificationException, DataNotFoundException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireAuthenticationService.thirdStepSubscription(requestContext, partenaire, dataInWs, httpServletRequest, "");
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@GetMapping("/pined")
	public CommonResponseWs pinedPartenaire() throws Exception {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		return partenaireAuthenticationService.pinedPartenaire(partenaire);
	}

	@PostMapping("/otp/request")
	public ResponseEntity<CommonResponseWs> sendOtpSubscription(@RequestBody CreatePartenaireOtpRequestWs ws) throws AuthentificationException, NotificationException, DataNotFoundException, OwomiException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs responseWs = partenaireAuthenticationService.sendOtpSubscription(requestContext, partenaire, ws);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/otp/pin")
	public ResponseEntity<CommonResponseWs> verifyPin(@RequestBody CreatePartenaireOtpRequestWs ws) throws AuthentificationException, NotificationException, DataNotFoundException, OwomiException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		String token = httpServletRequest.getHeader("access-token");
		CommonResponseWs responseWs = partenaireAuthenticationService.verifyPin(requestContext, partenaire, ws, token);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/otp/validation")
	public ResponseEntity<CommonResponseWs> otpValidationSubscription(@RequestBody CreatePartenaireOtpValidationWs ws) throws AuthentificationException, NotificationException, DataNotFoundException, OwomiException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs responseWs = partenaireAuthenticationService.otpValidationSubscription(requestContext, partenaire, ws);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}


	@PostMapping("/authenticate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CodeWs> authenticate(@RequestParam String firebaseToken, @RequestParam String username) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException, FirebaseAuthException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		CodeWs codeWs = partenaireAuthenticationService.authenticate(requestContext, firebaseToken, username);
		return new ResponseEntity<>(codeWs, HttpStatus.OK);
	}


}
