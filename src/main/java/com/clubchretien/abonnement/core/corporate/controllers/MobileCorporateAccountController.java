package com.clubchretien.abonnement.core.corporate.controllers;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.corporate.services.MobileCorporateService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/corporate")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class MobileCorporateAccountController {
	private static Logger logger = LogManager.getLogger(MobileCorporateAccountController.class);

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	MobileCorporateService service;
	@Autowired
	SecurityUtils securityUtils;


	@GetMapping("list")
	public CommonResponseWs listCorporates() throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.listCorporates(partenaire);
	}

	@GetMapping("transaction/historic/list")
	public CommonResponseWs getHistoriqueCompteByCorporate(
			@RequestParam("idCorporate") Integer idCorporate,
			@RequestParam("page") Integer page, @RequestParam("size") Integer size
	) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.getHistoriqueCompteByCorporate(partenaire, idCorporate, page, size);
	}

	@GetMapping("transaction/list/core")
	public CommonResponseWs listDistributionTransactionByCorporateMatricule(
			@RequestParam String matricule, @RequestParam Integer page, @RequestParam Integer size
	) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.listDistributionTransactionByCorporateMatricule(partenaire, matricule, page, size);
	}

	@GetMapping("solde")
	public CommonResponseWs solde(@RequestParam String matricule) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.soldeCorporate(partenaire, matricule);
	}

	@GetMapping("matricule")
	public CommonResponseWs getCorporateByMatricule(@RequestParam String matricule) throws AuthentificationException, DataNotFoundException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.getCorporateByMatricule(partenaire, matricule);
	}


}
