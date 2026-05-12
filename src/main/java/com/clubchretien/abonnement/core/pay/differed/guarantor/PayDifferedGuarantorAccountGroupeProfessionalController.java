package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeDocCreateWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeProfessionalContratWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeProfessionalCreateWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeProfessionalServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@Slf4j
@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/groupe/professional")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountGroupeProfessionalController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;


	@Autowired
	PayDifferedGuarantorAccountGroupeProfessionalServiceClient serviceClient;

	@GetMapping("state/step")
	public CommonResponseWs stateStepAccountGuarantorGroupeProfessional(@RequestParam Long groupeId) throws Exception {
		String token = securityUtils.getAccessToken(httpServletRequest);
		log.info("token : {}", token);
		return this.serviceClient.stateStepAccountGuarantorGroupeProfessional(groupeId, token);
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckStepAccountGuarantorGroupeProfessional(@RequestParam Long groupeId) throws Exception {
		String token = securityUtils.getAccessToken(httpServletRequest);
		log.info("token : {}", token);
		return this.serviceClient.recheckStepAccountGuarantorGroupeProfessional(groupeId, token);
	}

	@PostMapping("create")
	public CommonResponseWs createAccountGuarantorGroupeProfessional(@RequestBody PayDifferedGuarantorAccountGroupeProfessionalCreateWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		String token = securityUtils.getAccessToken(httpServletRequest);
		log.info("token : {}", token);
		return this.serviceClient.createAccountGuarantorGroupeProfessional(ws, token);
	}

	@PutMapping("update/docs")
	public CommonResponseWs configureDocsAccountGuarantorGroupeProfessional(@RequestBody PayDifferedGuarantorAccountGroupeDocCreateWs ws) throws Exception {
		return this.serviceClient.configureDocsAccountGuarantorGroupeProfessional(ws);
	}

	@PutMapping("update/contrat")
	public CommonResponseWs configureContratAccountGuarantorGroupeProfessional(@RequestBody PayDifferedGuarantorAccountGroupeProfessionalContratWs ws) throws Exception {
		String token = securityUtils.getAccessToken(httpServletRequest);
		log.info("token : {}", token);
		return this.serviceClient.configureContratAccountGuarantorGroupeProfessional(ws, token);
	}

}
