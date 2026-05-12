package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PartenaireIdentityService;
import com.clubchretien.abonnement.services.ValidationService;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.ValidationKycAgentWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController("ValidationController")
@RequestMapping("v1.0/validation")
public class ValidationController {
	private static Logger logger = LogManager.getLogger(ValidationController.class);

	@Autowired
	ValidationService validationService;
	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	PartenaireIdentityService partenaireIdentityService;

	@Autowired
	UserService userService;

	@GetMapping("/kyc/express/authorized")
	public CommonResponseWs authorizedPreValidationKyc(@RequestParam(required = false) String telephone) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		if (telephone != null) partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		return partenaireIdentityService.authorizedPreValidationKyc(partenaire);
	}

	@GetMapping("/kyc/express/info")
	public CommonResponseWs detailPreValidationKyc(@RequestParam String telephone) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		return partenaireIdentityService.detailPreValidationKyc(partenaire, telephone);
	}

	@PostMapping("/kyc/express/validate")
	public CommonResponseWs detailPreValidationKyc(@RequestBody ValidationKycAgentWs ws) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		return partenaireIdentityService.preValidateKyc(partenaire, ws);
	}



}