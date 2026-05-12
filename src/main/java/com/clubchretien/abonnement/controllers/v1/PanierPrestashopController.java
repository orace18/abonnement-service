package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PanierService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PanierPrestashopWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/panierPrestashop")
public class PanierPrestashopController {
    @Autowired
    PanierService panierService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    @PostMapping("/payement")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> panierPayement(@RequestBody PanierPrestashopWs panierPrestashopWs , @RequestParam(name = "deviseTransaction", required = false, defaultValue ="XOF") String deviseTransaction ) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = panierService.addToPanier(requestContext, partenaire, panierPrestashopWs,deviseTransaction);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }
}
