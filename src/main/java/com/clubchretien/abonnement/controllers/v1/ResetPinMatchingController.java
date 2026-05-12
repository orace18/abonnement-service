package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.ResetPinMatchingService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/matching")
@Tag(name = "ResetPinMatching", description = "ResetPinMatching API")
public class ResetPinMatchingController {

    @Autowired
    ResetPinMatchingService resetPinMatchingService;

    @Autowired
    HttpServletRequest httpServletRequest;

    @Autowired
    UserService userService;

    @PostMapping("/match")
    public ResponseEntity<CommonResponseWs> matchResetPin(@RequestBody MatchingFormWs ws) {
        CommonResponseWs commonResponseWs = resetPinMatchingService.matchToResetPin(ws);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/code")
    public ResponseEntity<CommonResponseWs> pinResetCode(@RequestParam String phoneUser) throws OwomiException {
        CommonResponseWs commonResponseWs = resetPinMatchingService.getResetCode(phoneUser);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/validate")
    public ResponseEntity<CommonResponseWs> validateResetCode(@RequestBody MatchingAuthWs formWs) throws OwomiException {
        CommonResponseWs commonResponseWs = resetPinMatchingService.validateResetCode(formWs);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<CommonResponseWs> updatePinHistory(@RequestParam String code) throws OwomiException, DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs commonResponseWs = resetPinMatchingService.updatePinHistory(partenaire, code);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
