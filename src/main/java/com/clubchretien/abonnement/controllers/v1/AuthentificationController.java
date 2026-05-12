package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.AuthentificationService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/authentication")
public class AuthentificationController {
	private static Logger logger = LogManager.getLogger(AuthentificationController.class);
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	AuthentificationService authentificationService;

	@PostMapping("authenticate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> authenticate() throws DataNotFoundException, AuthentificationException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		logger.debug("context is {}", requestContext);
		CommonResponseWs commonResponseWs = this.authentificationService.authenticate();
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("authenticate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> tokenInfo(
			@RequestParam String token
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		logger.debug("context is {}", requestContext);
		CommonResponseWs commonResponseWs = this.authentificationService.tokenInfo(token);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("info")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> authenticate(
			@RequestParam String type, @RequestParam String token
	) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		logger.debug("context is {}", requestContext);
		CommonResponseWs commonResponseWs = this.authentificationService.checkAccessToken(type, token);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}
}
