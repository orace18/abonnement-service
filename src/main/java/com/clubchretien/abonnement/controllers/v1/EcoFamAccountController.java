package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.EcoFamAccountService;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CreateRechargeInitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("v1.0/eco-fam")
@Tag(name = "EcoFam", description = "EcoFam")
public class EcoFamAccountController {
    private static Logger logger = LogManager.getLogger(EcoFamAccountController.class);
    @Autowired
    EcoFamAccountService ecoFamAccountService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    @Autowired
    SecurityUtils securityUtils;

    @GetMapping("/solde")
    public ResponseEntity<CommonResponseWs> accountEcoFamSolde() throws Exception {
        //artenaire partenaire = userService.getPartenaire(httpServletRequest);
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.accountEcoFamSolde(partenaire);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/profil/financier")
    public ResponseEntity<CommonResponseWs> profilFinancier() throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.profilFinancier(partenaire);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/guarantees/asked")
    public ResponseEntity<CommonResponseWs> listAskedGuarantees(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.listAskedGuarantees(partenaire, page, size);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/guarantees/given")
    public ResponseEntity<CommonResponseWs> listGivenGuarantees(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.listGivenGuarantees(partenaire, page, size);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/guarantees/received")
    public ResponseEntity<CommonResponseWs> listReceivedGuarantees(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.listReceivedGuarantees(partenaire, page, size);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/transactions/list")
    public ResponseEntity<CommonResponseWs> listAccountTransactions(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.listAccountTransactions(partenaire, page, size);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/recharge/init")
    public ResponseEntity<CommonResponseWs> initializeAccountEcoFamRecharge(
            @RequestBody CreateRechargeInitWs ws
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.initializeAccountEcoFamRecharge(partenaire, ws);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/recharge/list")
    public ResponseEntity<CommonResponseWs> listAccountEcoFamRecharge(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size
    ) throws Exception {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = ecoFamAccountService.listAccountEcoFamRecharge(partenaire, page, size);
        logger.debug("Out createCorporate ===> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
