package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.services.TransactionFinanciereService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
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
@RequestMapping("v2.0/transaction/financiere")
@Tag(name = "Transaction financière", description = "API for rechage module")
public class TransactionFinanciereV2Controller {
    private static Logger logger = LogManager.getLogger(TransactionFinanciereService.class);

    @Autowired
    TransactionFinanciereService transactionFinanciereService;
    @Autowired
    UserService userService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    PartenaireRepository partenaireRepository;

    @GetMapping("/corporate/{idCorporate}") // releve du compte
    //@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getHistoriqueCompteByCorporate(
            @PathVariable("idCorporate") Integer idCorporate,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size
    ) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs commonResponseWs = transactionFinanciereService.getHistoriqueCompteByCorporate(
                partenaire, idCorporate, page, size
        );
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

}