package com.clubchretien.abonnement.core.devises.controllers;

import com.clubchretien.abonnement.core.devises.services.MobileDeviseService;
import com.owomi.commons.ws.CommonResponseWs;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("core/v1.0/devise")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class MobileDeviseController {

	@Autowired
	MobileDeviseService mobileDeviseService;


	@GetMapping("list")
	public CommonResponseWs all() {
		return mobileDeviseService.listDevises();
	}

}
