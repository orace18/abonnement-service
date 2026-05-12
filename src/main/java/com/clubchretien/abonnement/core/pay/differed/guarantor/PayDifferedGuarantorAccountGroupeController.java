package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/groupe")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountGroupeController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	PayDifferedGuarantorAccountGroupeServiceClient serviceClient;

	@GetMapping("managers/list")
	public CommonResponseWs listManagersGarantGroup(
			@RequestParam Long groupeId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		return this.serviceClient.listManagersGarantGroup(groupeId, page, size);
	}

}
