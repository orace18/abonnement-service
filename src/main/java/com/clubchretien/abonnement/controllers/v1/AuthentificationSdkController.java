package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.AuthentificationSdkService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.ws.SdkTokenResponseWs;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/authentificationSdk")
public class AuthentificationSdkController {
    private static Logger logger = LogManager.getLogger(AuthentificationSdkController.class);
    @Autowired
    AuthentificationSdkService authentificationSdkService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    @Autowired
    PartenaireRepository partenaireRepository;

    @GetMapping
	//@ApiVersions({"1.0"})
    public ResponseEntity<SdkTokenResponseWs> getTokenSdk(@RequestParam("idsdk") String idSdk) throws DataNotFoundException, AuthentificationException {
        SdkTokenResponseWs response = authentificationSdkService.getToken(userService.getRequestContext(httpServletRequest), userService.getPartenaire(httpServletRequest), idSdk);
        logger.debug("response return is {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
