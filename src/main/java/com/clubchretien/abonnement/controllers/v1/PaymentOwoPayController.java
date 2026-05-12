package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PaymentOwoPayService;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/payment/owo-pay")
@Slf4j
@Tag(name = "PaymentOwoPayController", description = "PaymentOwoPayController")
public class PaymentOwoPayController {


	@Autowired
	PaymentOwoPayService paymentOwoPayService;


	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	UserService userService;

	@PostMapping("/process")
	public CommonResponseWs processOwoPay(
			@RequestParam Long idPaiement, @RequestParam String corporate,
			@RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		return paymentOwoPayService.processOwoPay(idPaiement, corporate, pushedPaiement);
	}

	@PostMapping("/validate")
	public CommonResponseWs validateTransactionPaiementOwoPay(
			@RequestParam Long idPaiement,
			@RequestParam(required = false) String pin
	) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		return paymentOwoPayService.validateTransactionPaiementOwoPay(partenaire, idPaiement, pin);
	}

	@PostMapping("/reject")
	public CommonResponseWs rejectTransactionPaiementOwoPay(
			@RequestParam Long idPaiement
	) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		return paymentOwoPayService.rejectTransactionPaiementOwoPay(partenaire, idPaiement);
	}

}
