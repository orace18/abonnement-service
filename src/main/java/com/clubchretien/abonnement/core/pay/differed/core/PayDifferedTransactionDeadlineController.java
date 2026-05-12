package com.clubchretien.abonnement.core.pay.differed.core;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.core.PayDifferedTransactionDeadlineServiceClient;
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
@RequestMapping("/mobile/v1.0/pay/differed/core/transaction/deadline")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedTransactionDeadlineController {
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;


	@Autowired
	PayDifferedTransactionDeadlineServiceClient service;

	@Autowired
	SecurityUtils securityUtils;

	@GetMapping("list/active")
	public CommonResponseWs listActivePayDifferedTransactionDeadlines(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.service.listActivePayDifferedTransactionDeadlines(partenaire.getId(), page, size);
	}

	@GetMapping("list/active/delay-unpaid")
	public CommonResponseWs listActivePayDifferedTransactionDeadlinesDelayOrUnpaid(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.service.listActivePayDifferedTransactionDeadlinesDelayOrUnpaid(partenaire.getId(), page, size);
	}

	@GetMapping("list/active/unpaid")
	public CommonResponseWs listActivePayDifferedTransactionDeadlinesUnpaid(
			@RequestParam Integer partenaireId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.service.listActivePayDifferedTransactionDeadlinesUnpaid(partenaire.getId(), page, size);
	}

}
