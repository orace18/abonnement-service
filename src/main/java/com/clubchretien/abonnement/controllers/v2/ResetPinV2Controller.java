package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.PartenaireService;
import com.clubchretien.abonnement.services.ResetPinService;
import com.google.firebase.auth.FirebaseAuthException;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
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
@RequestMapping("v2.0/resetPin")
@Tag(name = "ResetPin", description = "ResetPin")
public class ResetPinV2Controller {
	private static Logger logger = LogManager.getLogger(ResetPinV2Controller.class);
	@Autowired
	ResetPinService resetPinService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireService partenaireService;


    @PostMapping("/modif")
  //  @ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> modifierPin(@RequestBody DataInWs dataInWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = resetPinService.modifierPin(requestContext,partenaire,dataInWs,httpServletRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }





}
