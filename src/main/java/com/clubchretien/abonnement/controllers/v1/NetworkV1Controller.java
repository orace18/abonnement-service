package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.NetworkService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/network")
@Tag(name = "Network", description = "Network")
public class NetworkV1Controller {
	@Autowired
	NetworkService networkService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;

	//TODO payer les frais d'ahesion
	@PostMapping
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> payMemberShipFees(@RequestParam Integer idCorporate) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = networkService.payMemberShipFees(requestContext, partenaire, idCorporate);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

    @PostMapping("/payMaintenanceFees")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> payMaintenanceFees(@RequestParam Integer idCorporate, @RequestParam(required = false, defaultValue = "1") Integer quantite) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = networkService.payMaintenanceFees(requestContext, partenaire, idCorporate, quantite);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/pay-maintenance-fee-using-corporate-matricule")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> payMaintenanceFeesUsingCorporateMatricule(@RequestParam String matriculeCorporate, @RequestParam(required = false, defaultValue = "1") Integer quantite) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = networkService.payMaintenanceFeesUsingCorporateMatricule(requestContext, partenaire, matriculeCorporate, quantite);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
