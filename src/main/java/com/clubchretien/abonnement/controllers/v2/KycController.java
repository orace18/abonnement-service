package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.KycService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.kyc.KycValidationWs;
import com.owomi.database.entities.Partenaire;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController("KycController")
@RequestMapping("v2.0/kyc")
public class KycController {
	private static Logger logger = LogManager.getLogger(KycController.class);

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	SecurityUtils securityUtils;
	@Autowired
	KycService kycService;

	@PostMapping("/validation")
	public CommonResponseWs validationKyc(@RequestBody KycValidationWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		logger.debug("Partenaire is =============> {}", partenaire.getId());
		if (partenaire == null) return new  CommonResponseWs(WsOperationResponseStatus.failed, "Partenaire introuvable",404, null);
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		return kycService.validationKyc(partenaire, ws);
	}



}