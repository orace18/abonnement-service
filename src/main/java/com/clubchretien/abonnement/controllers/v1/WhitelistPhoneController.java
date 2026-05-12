package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.WhitelistPhoneService;
import com.owomi.commons.ws.CommonResponseWs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1.0/whitelist/phone")
public class WhitelistPhoneController {

	@Autowired
	private WhitelistPhoneService whitelistPhoneService;

	@PostMapping("/check")
	//@ApiVersions({"1.0"})
	public CommonResponseWs checkTelephone(
			@RequestParam String codePays, @RequestParam String telephone
	) {
		CommonResponseWs response = whitelistPhoneService.checkTelephone(codePays, telephone);
		return response;
	}
}
