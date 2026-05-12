package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.DifferedOperationService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PaimentDifferedWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v2.0/operation/differed")
public class DifferedOperationController {
    private static Logger logger = LogManager.getLogger(DifferedOperationController.class);
    @Autowired
    DifferedOperationService differedOperationService;

    @Autowired
    HttpServletRequest request;

    @Autowired
    UserService userService;

    @Autowired
    HttpServletRequest httpServletRequest;



    @PostMapping("/init")
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> initOperation(@RequestParam String productName, @RequestParam Integer quantite ) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = differedOperationService.initOperation(requestContext, partenaire, productName,quantite);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/payement")
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> payement(@RequestBody PaimentDifferedWs differedWs ) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = differedOperationService.payement(requestContext, partenaire, differedWs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



}
