package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.CorporateDelegationService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CorporateDelegationCreateWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.CorporateRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/corporate/delegation")
public class CorporateDelegationController {
	private static Logger logger = LogManager.getLogger(CorporateDelegationController.class);
	@Autowired
	CorporateDelegationService corporateDelegationService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	CorporateRepository corporateRepository;

	@PostMapping("/delegate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> delegateCorporate(
			@RequestBody CorporateDelegationCreateWs ws
	) throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In delegateCorporate ===> {}", ws);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateDelegationService.createDelegation(ws, partenaire);
		logger.debug("Out delegateCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/revoke")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> revokeDelegation(
			@RequestBody CorporateDelegationCreateWs ws
	) throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In delegateCorporate ===> {}", ws);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateDelegationService.revokeDelegation(ws, partenaire);
		logger.debug("Out delegateCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/list/owner")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listDelegation() throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In delegateCorporate ===>");
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateDelegationService.listDelegationOwner(partenaire);
		logger.debug("Out delegateCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/list/owner/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listDelegation(@RequestParam String telephone) throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In delegateCorporate ===>");
		Partenaire partenaire = this.partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateDelegationService.listDelegationOwner(partenaire);
		logger.debug("Out delegateCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/list/manager")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listDelegationManager() throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In delegateCorporate ===> ");
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateDelegationService.listDelegationManager(partenaire);
		logger.debug("Out delegateCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


}
