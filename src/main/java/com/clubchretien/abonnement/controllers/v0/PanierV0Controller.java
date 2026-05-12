package com.clubchretien.abonnement.controllers.v0;

import com.clubchretien.abonnement.services.PanierService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PanierWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v0.0/panier")
@Tag(name = "Panier", description = "Panier")
public class PanierV0Controller {
	@Autowired
	PanierService panierService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;

	@PostMapping("/addToPanier")
	//@ApiVersions({"0.0"})
	public ResponseEntity<CommonResponseWs> addToPanier(@ModelAttribute PanierWs panierWs, @RequestParam(value = "directPay", defaultValue = "false") Boolean directPay, @RequestParam(value = "idCorporate", defaultValue = "0") int idCorporate, @RequestParam String deviseTransaction) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = null;
		if (directPay)
			commonResponseWs = panierService.directPay(requestContext, partenaire, panierWs, idCorporate, deviseTransaction);
		else
			commonResponseWs = panierService.addToPanier(requestContext, partenaire, panierWs, idCorporate, deviseTransaction);
		HttpStatus httpStatus = null;
		if (commonResponseWs.getStatus() == WsOperationResponseStatus.success)
			httpStatus = HttpStatus.OK;
		else
			httpStatus = HttpStatus.NOT_FOUND;
		return new ResponseEntity<>(commonResponseWs, httpStatus);
	}
}
