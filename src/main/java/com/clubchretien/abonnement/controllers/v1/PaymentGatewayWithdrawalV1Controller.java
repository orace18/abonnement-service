package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PaymentGatewayWithdrawalService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DemandeRetraitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
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
public class PaymentGatewayWithdrawalV1Controller {

	@Autowired
	PaymentGatewayWithdrawalService paymentGatewayWithdrawalService;


	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@Autowired
	PartenaireRepository partenaireRepository;


	@GetMapping("/withdrawal-means")
	//@ApiVersions({"1.0"})
	public CommonResponseWs getWithdrawalMeans() throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.getWithdrawalMeans(requestContext, partenaire, 2.0);
		return response;
	}

	@GetMapping("/withdrawal-means/master")
	//@ApiVersions({"1.0"})
	public CommonResponseWs getWithdrawalMeans(
			@RequestParam String telephone,
			@RequestParam(required = false, defaultValue = "2.0") Double version
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.getWithdrawalMeans(requestContext, partenaire, version);
		return response;
	}

	@PostMapping("/withdrawal/resume")
	//@ApiVersions({"1.0"})
	public CommonResponseWs resume(
			@RequestParam Long idPaiement
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.resume(requestContext, idPaiement);
		return response;
	}

	@PostMapping("/withdrawal/demande")
	//@ApiVersions({"1.0"})
	public CommonResponseWs demandeRetrait(
			@RequestBody DemandeRetraitWs ws
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.demandeRetrait(requestContext, partenaire, ws);
		return response;
	}

	@PostMapping("/withdrawal/cancel")
	//@ApiVersions({"1.0"})
	public CommonResponseWs cancelRetrait(
			@RequestParam Long idPaiement
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.cancelRetrait(requestContext, idPaiement);
		return response;
	}

	@PostMapping("/withdrawal/process")
	//@ApiVersions({"1.0"})
	public CommonResponseWs process(
			@RequestParam Long idPaiement,
			@RequestParam String pin
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.process(requestContext, partenaire, idPaiement, pin);
		return response;
	}

	@GetMapping("/withdrawal/list")
	//@ApiVersions({"1.0"})
	public CommonResponseWs withdrawalList(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "0") Integer size,
			@RequestParam(required = false, defaultValue = "false") Boolean pending
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = paymentGatewayWithdrawalService.withdrawalList(requestContext, partenaire, page, size, pending);
		return response;
	}


}
