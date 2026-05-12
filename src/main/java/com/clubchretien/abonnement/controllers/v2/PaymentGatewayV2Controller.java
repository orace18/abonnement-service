package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.PaymentGatewayService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.owopay.PaymentRequestWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.flocash.CybersourceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v2.0/payment-gateway")
@Slf4j
@Tag(name = "PaymentGateway", description = "PaymentGateway")
public class PaymentGatewayV2Controller {


	@Autowired
	PaymentGatewayService paymentGatewayService;

	@Autowired
	CybersourceClient cybersourceClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;


	@GetMapping("/payment-means")
	//@ApiVersions({"2.0"})
	public CommonResponseWs getPaymentsMeansV2(
			@RequestParam(required = false, defaultValue = "2.0") Double version
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.getPaymentsMeans(requestContext, partenaire, version);
		return response;
	}

	@GetMapping("/resume")
	//@ApiVersions({"2.0"})
	public CommonResponseWs resumePaiement(
			@RequestParam Long idPaiement, @RequestParam Integer idPaymentMean
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		//Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.resumePaiement(requestContext, idPaiement, idPaymentMean);
		return response;
	}


	@GetMapping("/status/paiement")
	//@ApiVersions({"2.0"})
	public CommonResponseWs statusPaiement(
			@RequestParam Long idPaiement
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.statusPaiement(requestContext, idPaiement);
		return response;
	}

	@PostMapping("/paiement")
	//@ApiVersions({"2.0"})
	public CommonResponseWs paiement(
			@RequestBody PaymentRequestWs paymentRequestWs
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.paiement(requestContext, paymentRequestWs);
		return response;
	}

	@GetMapping("/process")
	//@ApiVersions({"2.0"})
	public CommonResponseWs processOwoPay(
			@RequestParam Long idPaiement, @RequestParam Integer idPaymentMean,
			@RequestParam String corporate
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.processOwoPay(requestContext, idPaiement, idPaymentMean, corporate);
		return response;
	}


}
