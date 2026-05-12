
package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.NotificationService;
import com.google.firebase.auth.FirebaseAuthException;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.ActivityLogRawWs;
import com.owomi.commons.ws.ActivityLogUserWs;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController("Notification")
@RequestMapping("v1.0/notification")
@Tag(name = NotificationController.NAME, description = NotificationController.DESCRIPTION)
public class NotificationController {

	public static final String NAME = "Notifications";
	public static final String DESCRIPTION = "All " + NAME + " End point";

	@Autowired
	NotificationService notificationService;

	@Autowired
	PartenaireRepository partenaireRepository;


	@Autowired
	HttpServletRequest httpServletRequest;


	@Autowired
	UserService userService;


	@PostMapping("/user/create")
	@Operation(
			summary = "Set User Firebase Token For Notification ",
			description = "Use this endpoint to Set User Firebase Token For Notification "
	)
	public ResponseEntity<CommonResponseWs> createUser(@RequestBody ActivityLogUserWs activityLogUserWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
		CommonResponseWs commonResponseWs = notificationService.createUser(activityLogUserWs);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/user/update")
	@Operation(
			summary = "Update User Firebase Token For Notification ",
			description = "Use this endpoint to Update User Firebase Token For Notification "
	)
	public ResponseEntity<CommonResponseWs> updateUser(@RequestBody ActivityLogUserWs activityLogUserWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
		CommonResponseWs commonResponseWs = notificationService.updateUser(activityLogUserWs);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/activity/logs/mark-read")
	public ResponseEntity<CommonResponseWs> activityLogs(
			@RequestBody List<UUID> ids
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = notificationService.activityLogsMarkAsRead(ids);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/activity/logs")
	public ResponseEntity<CommonResponseWs> activityLogs(
			@RequestParam(required = false, defaultValue = "MOBILE") FirebaseAppEnums app,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = notificationService.activityLogs(app, partenaire, page, size);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/activity/logs/master")
	public ResponseEntity<CommonResponseWs> activityLogs(
			@RequestParam String email,
			@RequestParam(required = false, defaultValue = "MOBILE") FirebaseAppEnums app,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws OwomiException {
		Partenaire partenaire = partenaireRepository.findOneByEmail(email);
		CommonResponseWs commonResponseWs = notificationService.activityLogs(app, partenaire, page, size);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}


	@PostMapping("/test/al")
	public CommonResponseWs testAl(
			@RequestBody ActivityLogRawWs ws
	) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		notificationService.notificationAl(FirebaseAppEnums.MOBILE, partenaire, ws);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

	@PostMapping("/welcome/app/email")
	public CommonResponseWs welcomeEmail(
			@RequestParam String email
	) throws Exception {
		Partenaire partenaire = this.partenaireRepository.findOneByEmail(email);
		notificationService.welcomeApp(partenaire);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

	@PostMapping("/welcome/app/phone")
	public CommonResponseWs welcomePhone(
			@RequestParam String phone
	) throws Exception {
		Partenaire partenaire = this.partenaireRepository.findOneByTelephone(phone);
		notificationService.welcomeApp(partenaire);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

}

