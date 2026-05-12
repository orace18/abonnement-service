package com.clubchretien.abonnement.core.pay.differed.core;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.core.PayDifferedTransactionPaymentServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/core/transaction/payment")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedTransactionPaymentController {
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;

	@Autowired
	PayDifferedTransactionPaymentServiceClient serviceClient;

	@PostMapping("/initialise/deadline")
	public CommonResponseWs initialisePaiementDeadline(
			@RequestParam Long deadlineId, @RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.serviceClient.initialisePaiementDeadline(deadlineId, pushedPaiement);
	}

	@PostMapping("/initialise/deadline/multiple")
	public CommonResponseWs initialisePaiementDeadlines(
			@RequestBody Set<Long> ws, @RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.serviceClient.initialisePaiementDeadlines(ws, partenaire.getId(), pushedPaiement);
	}

	@PostMapping("/initialise/transaction")
	public CommonResponseWs initialisePaiementTransaction(
			@RequestParam Long transactionId, @RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		return this.serviceClient.initialisePaiementTransaction(transactionId, pushedPaiement);
	}

	@PostMapping("/initialise/transaction/partial")
	public CommonResponseWs initialisePartialPaiementTransaction(
			@RequestParam Long transactionId, @RequestParam Double amount,
			@RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		return this.serviceClient.initialisePartialPaiementTransaction(transactionId, amount, pushedPaiement);
	}

	@PostMapping("/initialise/transaction/multiple")
	public CommonResponseWs initialisePaiementTransactions(
			@RequestBody Set<Long> ws, @RequestParam(required = false, defaultValue = "false") Boolean pushedPaiement
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.serviceClient.initialisePaiementTransactions(ws, partenaire.getId(), pushedPaiement);
	}

	@GetMapping("/certify/customer")
	public CommonResponseWs certifiedPaiementCustomerDiffered(@RequestParam Long idTransactionPayment, @RequestParam Long idPaiement) throws Exception {
		return this.serviceClient.certifiedPaiementCustomerDiffered(idTransactionPayment, idPaiement);
	}

	@GetMapping(value = {"/certify/deadline"})
	public CommonResponseWs certifiedPayDifferedDeadline(@RequestParam Long idTransactionPayment, @RequestParam Long idPaiement) throws Exception {
		return this.serviceClient.certifiedPayDifferedDeadline(idTransactionPayment, idPaiement);
	}

	@GetMapping(value = {"/deadline/multiple", "/certify/transaction", "/certify/transaction/multiple"})
	public CommonResponseWs certifiedPayDifferedDeadlines(@RequestParam Long idTransactionPayment, @RequestParam Long idPaiement) throws Exception {
		return this.serviceClient.certifiedPayDifferedDeadlines(idTransactionPayment, idPaiement);
	}

	@GetMapping(value = {"/certify/transaction/partial"})
	public CommonResponseWs certifiedPartialPayDifferedDeadlines(@RequestParam Long idTransactionPayment, @RequestParam Long idPaiement) throws Exception {
		return this.serviceClient.certifiedPartialPayDifferedDeadlines(idTransactionPayment, idPaiement);
	}


}
