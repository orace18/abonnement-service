package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.OperationService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PrestashopResponseWs;
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
@RequestMapping("v1.0/operation")
public class OperationController {
    private static Logger logger = LogManager.getLogger(OperationController.class);
    @Autowired
    OperationService operationService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    @PostMapping("/stepOneToBuy")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> stepOneToBuy(@RequestBody Integer[] ids, @RequestParam("id") int id, @RequestParam(name = "deviseTransaction", required = false, defaultValue = "XOF") String deviseTransaction) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = operationService.stepOneToBuy(requestContext,partenaire, ids, id,deviseTransaction,0);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @PostMapping("/stepTwoToBuy")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> stepTwoToBuy(@RequestParam("pin") String pin, @RequestParam("reference") String reference, @RequestParam(name = "deviseTransaction", required = false, defaultValue = "TEST") String deviseTransation) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = operationService.stepTwoToBuy(requestContext, partenaire, pin, reference, deviseTransation);
        HttpStatus httpStatus = null;
        if(commonResponseWs.getStatus() == WsOperationResponseStatus.success)
            httpStatus = HttpStatus.OK;
        else
            httpStatus = HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @GetMapping("/prestashopcv")
	//@ApiVersions({"1.0"})
    public ResponseEntity<PrestashopResponseWs> getterCvTieToPrestashop(@RequestParam("idPrestashop")  Integer idPrestashop, @RequestParam(required = false, defaultValue = "1.0")  Double quantite ) throws DataNotFoundException, OwomiException, AuthentificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        PrestashopResponseWs resultat = operationService.getCvTiedToIdPrestashop(requestContext,idPrestashop, quantite);
        return new ResponseEntity<>(resultat, HttpStatus.OK);
    }

    @PostMapping("/corporateValidation")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs>  corporateValidation(@ModelAttribute("pin") String pin, @ModelAttribute("refCommande") String referenceCommande) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = operationService.corporateValidation(requestContext,partenaire, pin, referenceCommande);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @GetMapping("/restart")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> restartSolde() throws DataNotFoundException, AuthentificationException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        operationService.restartSolde(requestContext);
        return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
    }
}
