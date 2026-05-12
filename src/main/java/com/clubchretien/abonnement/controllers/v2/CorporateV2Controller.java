package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.CorporateService;
import com.clubchretien.abonnement.services.TransactionFinanciereService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CorporateWs;
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
@RequestMapping("v2.0/corporate")
@Tag(name = "Corporate", description = "Corporate")
public class CorporateV2Controller {
	private static Logger logger = LogManager.getLogger(CorporateV2Controller.class);
	@Autowired
	CorporateService corporateService;
	@Autowired
	TransactionFinanciereService transactionFinanciereService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@GetMapping("/")
	//@ApiVersions({"2.0"})
	public ResponseEntity<List<CorporateWs>> getCorporate(
			@RequestParam(required = false, defaultValue = "false") Boolean certified
	) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		List<CorporateWs> listCorporates = corporateService.getCorporate(requestContext, partenaire, certified);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(listCorporates, httpStatus);
	}

	@PostMapping("/payKit/Marchand") // devenir marchand
	//@ApiVersions({"2.0"})
	public ResponseEntity<CommonResponseWs> payKitMarchandV2(@RequestParam Integer idCorporate, @RequestParam Integer corporate, @RequestParam String productName) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.payKitDemarrageMarchand(requestContext, partenaire, idCorporate, productName, corporate);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
