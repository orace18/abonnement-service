
package com.clubchretien.abonnement.core.notifications;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.NotificationService;
import com.google.firebase.auth.FirebaseAuthException;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.ActivityLogRawWs;
import com.owomi.commons.ws.ActivityLogUserWs;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;
import static com.owomi.commons.constants.TokenConstant.CORPORATE_ID;

@RestController
@RequestMapping("/core/v1.0/notification")
@SecurityRequirements( {@SecurityRequirement(name = ACCESS_TOKEN), @SecurityRequirement(name = CORPORATE_ID),})
public class NotificationController {

	@Autowired
	NotificationService notificationService;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	SecurityUtils securityUtils;


	@PostMapping("/user/create")
	public CommonResponseWs createUser(@RequestBody ActivityLogUserWs activityLogUserWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return notificationService.createUser(activityLogUserWs);
	}

	@PostMapping("/user/update")
	public CommonResponseWs updateUser(@RequestBody ActivityLogUserWs activityLogUserWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return notificationService.updateUser(activityLogUserWs);
	}

	@PutMapping("/activity/logs/mark-read")
	public CommonResponseWs activityLogsMarkAsRead(@RequestBody List<UUID> ids) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return notificationService.activityLogsMarkAsRead(ids);
	}

	@GetMapping("/activity/logs")
	public CommonResponseWs activityLogs(
			@RequestParam(required = false, defaultValue = "MOBILE") FirebaseAppEnums app,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return notificationService.activityLogs(app, partenaire, page, size);
	}

	@PostMapping("/test/al")
	public CommonResponseWs testAl(
			@RequestBody ActivityLogRawWs ws
	) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		notificationService.notificationAl(FirebaseAppEnums.MERCHANT, partenaire, ws);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
	}

}

