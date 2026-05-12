package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.PaymentGatewayWithdrawalService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v2.0/payment-gateway")
@Slf4j
@Tag(name = "PaymentGateway", description = "PaymentGateway")
public class PaymentGatewayWithdrawalV2Controller {

	@Autowired
	PaymentGatewayWithdrawalService paymentGatewayWithdrawalService;


	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@Autowired
	PartenaireRepository partenaireRepository;

	@GetMapping("/withdrawal-means")
	//@ApiVersions({"2.0"})
	public CommonResponseWs getWithdrawalMeans(
			@RequestParam(required = false, defaultValue = "2.0") Double version
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.getWithdrawalMeans(requestContext, partenaire, version);
		return response;
	}

}
