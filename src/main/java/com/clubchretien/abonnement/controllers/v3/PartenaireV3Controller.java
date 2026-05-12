package com.clubchretien.abonnement.controllers.v3;

import com.clubchretien.abonnement.controllers.v2.PartenaireV2Controller;
import com.clubchretien.abonnement.services.PartenaireService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CreatePartenaireResponseWs;
import com.owomi.commons.ws.DataInWs;
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

@RestController
@RequestMapping("v3.0/partenaire")
@Tag(name = "Partenaire", description = "Partenaire")
public class PartenaireV3Controller {
	private static Logger logger = LogManager.getLogger(PartenaireV2Controller.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireService partenaireService;

	@PostMapping("/pininfo")
	//@ApiVersions({"3.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireThirdStepCrypted(@RequestBody DataInWs dataInWs) throws AuthentificationException, NotificationException, DataNotFoundException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CreatePartenaireResponseWs responseWs = partenaireService.subcribeThirdStep(requestContext,partenaire, dataInWs,httpServletRequest,"");
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }


}
