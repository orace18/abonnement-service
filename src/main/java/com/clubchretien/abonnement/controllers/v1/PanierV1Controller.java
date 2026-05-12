package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PanierService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PanierWs;
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
import java.util.List;

@RestController
@RequestMapping("v1.0/panier")
@Tag(name = "Panier", description = "Panier")
public class PanierV1Controller {
	private static Logger logger = LogManager.getLogger(PanierV1Controller.class);
	@Autowired
	PanierService panierService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@GetMapping
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<PanierWs>> panier() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<PanierWs> panierWsList = panierService.panier(requestContext,partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(panierWsList, httpStatus);
    }

    @PostMapping("/addToPanier")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> addToPanier(@RequestBody List<PanierWs> panierWs,@RequestParam String deviseTransaction) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = panierService.addToPanier(requestContext,partenaire, panierWs,deviseTransaction);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @DeleteMapping("/deleteToPanier/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> deleteToPanier(@PathVariable("id") int id) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = panierService.deleteToPanier(requestContext,id);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @PutMapping("/updateToPanier")
	//@ApiVersions({"1.0"})
    public ResponseEntity<PanierWs> updateToPanier(@ModelAttribute PanierWs panierWs) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        PanierWs panierWs1 = panierService.updateToPanier(requestContext,panierWs);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(panierWs1, httpStatus);
    }
}
