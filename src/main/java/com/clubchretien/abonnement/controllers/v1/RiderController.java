package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.RiderService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/rider")
public class RiderController {
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	RiderService riderService;


	@GetMapping("/affiliation")
	public ResponseEntity<CommonResponseWs> listAffiliation(
			@RequestParam(required = false) String type,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = riderService.listAffiliation(context, type, page, size, partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("/stats/affiliation")
	public ResponseEntity<CommonResponseWs> statisticsAffiliation() throws DataNotFoundException, AuthentificationException, OwomiException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = riderService.statisticsAffiliation(partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}


	@GetMapping("/affiliation/master")
	public ResponseEntity<CommonResponseWs> listAffiliation(
			@RequestParam(required = false) String telephone,
			@RequestParam(required = false) String type,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = new Partenaire();
		partenaire.setTelephone(telephone);
		CommonResponseWs commonResponseWs = riderService.listAffiliation(context, type, page, size, partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("stats/affiliation/master")
	public ResponseEntity<CommonResponseWs> listAffiliation(
			@RequestParam(required = false) String telephone
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = new Partenaire();
		partenaire.setTelephone(telephone);
		CommonResponseWs commonResponseWs = riderService.statisticsAffiliation(partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("/affiliation/validation")
	public ResponseEntity<CommonResponseWs> listValidationAffiliation(
			@RequestParam(required = false, defaultValue = "false") Boolean validated,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = riderService.listValidationAffiliation(context, validated, page, size, partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PutMapping("/affiliation/validation")
	public ResponseEntity<CommonResponseWs> listValidationAffiliation(
			@RequestParam Integer idRider,
			@RequestParam(required = false, defaultValue = "false") Boolean validated
	) throws DataNotFoundException, AuthentificationException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = riderService.validationAffiliation(context, validated, idRider, partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

}
