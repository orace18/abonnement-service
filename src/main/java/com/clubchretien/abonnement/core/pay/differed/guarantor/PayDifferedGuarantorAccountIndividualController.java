package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountIndividualCreateWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountIndividualServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/individual")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountIndividualController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;



	@Autowired
	PayDifferedGuarantorAccountIndividualServiceClient serviceClient;

	@GetMapping("state/step")
	public CommonResponseWs stateStepAccountGuarantorIndividual() throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.serviceClient.stateStepAccountGuarantorIndividual(partenaire.getId());
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckAccountGuarantorIndividual() throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.serviceClient.recheckAccountGuarantorIndividual(partenaire.getId());
	}

	@PutMapping("update/engagement")
	public CommonResponseWs configureEngagementAccountGuarantorIndividual(@RequestBody PayDifferedGuarantorAccountIndividualCreateWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.serviceClient.configureEngagementAccountGuarantorIndividual(ws);
	}

}
