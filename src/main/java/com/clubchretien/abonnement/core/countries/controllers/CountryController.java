package com.clubchretien.abonnement.core.countries.controllers;

import com.clubchretien.abonnement.core.constants.DefaultConstants;
import com.clubchretien.abonnement.core.countries.models.CountryWs;
import com.clubchretien.abonnement.core.countries.payloads.params.CountryParams;
import com.clubchretien.abonnement.core.countries.services.CountryDataCenter;
import com.owomi.commons.ws.CommonResponseWss;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collection;

@RestController
@RequestMapping("countries/v1.0/country")
@Slf4j
public class CountryController {

	@GetMapping("default")
	public CommonResponseWss<Collection<CountryWs>> defaultCountry() throws Exception {
		CountryParams params = new CountryParams();
		params.setCode(DefaultConstants.COUNTRY_CODE);
		return CommonResponseWss.success(CountryDataCenter.all(params));
	}

	@GetMapping
	@Operation(security = {@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)})
	public CommonResponseWss<Collection<CountryWs>> all(
			@Valid CountryParams params
	) {
		log.info("all countries  params {} ", params);
		return CommonResponseWss.success(CountryDataCenter.all(params));
	}

	@GetMapping("find/phone-code")
	@Operation(security = {@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)})
	public CommonResponseWss<CountryWs> findByPhoneCode(
			@RequestParam String code
	) {
		log.info("findByPhoneCode countries  params {} ", code);
		return CommonResponseWss.success(CountryDataCenter.findByPhoneCode(code));
	}

	@GetMapping("list/phone-code")
	@Operation(security = {@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)})
	public CommonResponseWss<Collection<CountryWs>> listByPhoneCode(
			@RequestParam String code
	) {
		log.info("findByPhoneCode countries  params {} ", code);
		return CommonResponseWss.success(CountryDataCenter.allByPhoneCode(code));
	}


}
