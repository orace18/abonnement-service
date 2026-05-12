package com.clubchretien.abonnement.core.reset.pin.controller;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.reset.pin.service.ResetPinMatcherService;
import com.clubchretien.abonnement.core.reset.pin.ws.PinUpdateWs;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.authentication.MatchingAuthWs;
import com.owomi.commons.ws.authentication.MatchingFormWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("core/v1.0/pin/reset")
@Tag(name = "ResetPinMatching", description = "ResetPinMatching API")
public class ResetPinMatcherController {

    @Autowired
    ResetPinMatcherService resetPinMatcherService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    
    @Autowired
    SecurityUtils securityUtils;

    @PostMapping("/match")
    public ResponseEntity<CommonResponseWs> matchResetPin(@RequestBody MatchingFormWs formWs) throws OwomiException {
        CommonResponseWs commonResponseWs = resetPinMatcherService.matchToGetResetCode(formWs);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/validate")
    public ResponseEntity<CommonResponseWs> validateResetCode(@RequestBody MatchingAuthWs ws) throws OwomiException {
        CommonResponseWs commonResponseWs = resetPinMatcherService.validateResetCode(ws);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResponseWs> updatePinHistory(@RequestBody PinUpdateWs ws) throws DataNotFoundException, AuthentificationException, NotificationException, OwomiException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        CommonResponseWs commonResponseWs = resetPinMatcherService.updatePinHistory(partenaire, ws);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
