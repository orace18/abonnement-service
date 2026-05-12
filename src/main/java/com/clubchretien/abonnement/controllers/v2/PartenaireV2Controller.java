package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.PartenaireService;
import com.clubchretien.abonnement.ws.CreatePartenaireStepTwoWss;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.ws.CreatePartenaireResponseWs;
import com.owomi.commons.ws.CreatePartenaireStepOneWs;
import com.owomi.commons.ws.CreatePartenaireStepTreeWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("v2.0/partenaire")
@Tag(name = "Partenaire", description = "Partenaire")
public class PartenaireV2Controller {
	private static Logger logger = LogManager.getLogger(PartenaireV2Controller.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireService partenaireService;


	@PostMapping("/initinscription")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStep(@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.initialSubcribe(requestContext, createPartenaireStepOneWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/complementinfo")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireSecondStep(@RequestBody CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws AuthentificationException, OwomiException, FileUploadException, DataNotFoundException, IOException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.subcribeSecondStep(requestContext, partenaire, createPartenaireStepTwoWss);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/pininfo")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireThirdStep(@RequestBody CreatePartenaireStepTreeWs createPartenaireStepTreeWs) throws AuthentificationException, NotificationException, DataNotFoundException {
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.subcribeThirdStep(requestContext, partenaire, createPartenaireStepTreeWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}


}
