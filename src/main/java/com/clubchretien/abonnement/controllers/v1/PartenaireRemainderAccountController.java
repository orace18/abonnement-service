package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.PartenaireRemainderAccountService;
import com.clubchretien.abonnement.services.PartenaireService;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/partenaire/remainder")
public class PartenaireRemainderAccountController {
	private static Logger logger = LogManager.getLogger(PartenaireRemainderAccountController.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireService partenaireService;

	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	SecurityUtils securityUtils;

	@Autowired
	PartenaireRemainderAccountService partenaireRemainderAccountService;

	@PostMapping("/solde")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeRemainder(
			@RequestParam(required = false) Integer idBoutique
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.soldeRemainder(partenaire, idBoutique);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/solde/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeRemainder(
			@RequestParam String telephone,
			@RequestParam(required = false) Integer idBoutique
	) throws Exception {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.soldeRemainder(partenaire, idBoutique);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/list")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listRemainder(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean used,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.listRemainder(partenaire, search, used, page, size);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/list/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listRemainder(
			@RequestParam String telephone,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean used,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws Exception {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.listRemainder(partenaire, search, used, page, size);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/releve")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> statementAccountRemainder(
			@RequestParam Integer idBoutique,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.statementAccountRemainder(
				partenaire, idBoutique, page, size
		);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/releve/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> statementAccountRemainder(
			@RequestParam String telephone, @RequestParam Integer idBoutique,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws Exception {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireRemainderAccountService.statementAccountRemainder(
				partenaire, idBoutique, page, size
		);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}


}
