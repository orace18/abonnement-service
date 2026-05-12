package com.clubchretien.abonnement.controllers.v1;

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
@RequestMapping("v1.0/payment-gateway")
@Slf4j
@Tag(name = "PaymentGateway", description = "PaymentGateway")
public class PaymentGatewayV1Controller {


	@Autowired
	PaymentGatewayService paymentGatewayService;

	@Autowired
	CybersourceClient cybersourceClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@GetMapping("/payment-means")
	//@ApiVersions({"1.0"})
	public CommonResponseWs getPaymentsMeans() throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.getPaymentsMeans(requestContext, partenaire);
		return response;
	}

	@GetMapping("/resume")
	//@ApiVersions({"1.0"})
	public CommonResponseWs resume(
			@RequestParam Long idPaiement, @RequestParam Integer idPaymentMean
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		// CommonResponseWs response = paymentGatewayService.getResume(requestContext, idPaiement, idPaymentMean);
		CommonResponseWs response = paymentGatewayService.resumePaiement(requestContext, idPaiement, idPaymentMean);
		return response;
	}

	@GetMapping("/attente/paiement")
	//@ApiVersions({"1.0"})
	public CommonResponseWs attentePaiement(
			@RequestParam String matricule
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.attentePaiement(matricule);
		return response;
	}

	@GetMapping("/status/paiement")
	//@ApiVersions({"1.0"})
	public CommonResponseWs statutPaiement(
			@RequestParam Long idPaiement
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.statusPaiement(requestContext, idPaiement);
		//  CommonResponseWs response = paymentGatewayService.statutPaiement(requestContext, idPaiement);
		return response;
	}

	@PostMapping("/paiement")
	//@ApiVersions({"1.0"})
	public CommonResponseWs paiementV1(
			@RequestBody PaymentRequestWs paymentRequestWs
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		// CommonResponseWs response = paymentGatewayService.paiementV1(requestContext, paymentRequestWs);
		CommonResponseWs response = paymentGatewayService.paiement(requestContext, paymentRequestWs);
		return response;
	}

	@GetMapping("/process")
	//@ApiVersions({"1.0"})
	public CommonResponseWs processOwopay(
			@RequestParam Long idPaiement, @RequestParam Integer idPaymentMean,
			@RequestParam String corporate
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayService.processOwoPay(requestContext, idPaiement, idPaymentMean, corporate);
		//CommonResponseWs response = paymentGatewayService.processOwopay(requestContext, idPaiement, idPaymentMean, corporate);
		return response;
	}

	@GetMapping("/cybersource/order")
	//@ApiVersions({"1.0"})
	public CommonResponseWs cybersourceOrder(@RequestParam String reference) throws OwomiException, DataNotFoundException, AuthentificationException {
		return cybersourceClient.getOrder(reference);
	}

	@PutMapping("/cybersource/order")
	//@ApiVersions({"1.0"})
	public CommonResponseWs cybersourceOrder(@RequestParam String reference, @RequestBody Object data) throws OwomiException, DataNotFoundException, AuthentificationException {
		log.debug("update order  {} body {}", reference, data);
		return cybersourceClient.updateOrder(reference, data);
	}


}
