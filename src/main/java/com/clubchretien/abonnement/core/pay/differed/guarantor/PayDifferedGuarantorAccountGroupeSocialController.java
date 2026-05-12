package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeSocialCreateWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeSocialServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/groupe/social")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountGroupeSocialController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;


	@Autowired
	PayDifferedGuarantorAccountGroupeSocialServiceClient serviceClient;

	@GetMapping("state/step")
	public CommonResponseWs stateStepAccountGuarantorGroupeSocial(@RequestParam Long groupeId) throws Exception {
		return this.serviceClient.stateStepAccountGuarantorGroupeSocial(groupeId);
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckStepAccountGuarantorGroupeSocial(@RequestParam Long groupeId) throws Exception {
		return this.serviceClient.recheckStepAccountGuarantorGroupeSocial(groupeId);
	}

	@PostMapping("create")
	public CommonResponseWs createAccountGuarantorGroupeSocial(@RequestBody PayDifferedGuarantorAccountGroupeSocialCreateWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.serviceClient.createAccountGuarantorGroupeSocial(ws);
	}

}
