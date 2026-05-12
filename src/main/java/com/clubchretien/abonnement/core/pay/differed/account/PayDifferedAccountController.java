package com.clubchretien.abonnement.core.pay.differed.account;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.PayDifferedServiceClient;
import com.owomi.rest.client.pay.differed.account.PayDifferedAccountServiceClient;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeServiceClient;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountServiceClient;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorServiceClient;
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
@RequestMapping("/mobile/v1.0/pay/differed/account")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedAccountController {
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	PayDifferedAccountServiceClient accountServiceClient;

	@Autowired
	PayDifferedServiceClient payDifferedServiceClient;
	@Autowired
	PayDifferedGuarantorServiceClient payDifferedGuarantorServiceClient;
	@Autowired
	PayDifferedGuarantorAccountServiceClient payDifferedGuarantorAccountServiceClient;
	@Autowired
	PayDifferedGuarantorAccountGroupeServiceClient payDifferedGuarantorAccountGroupeServiceClient;

	@Autowired
	SecurityUtils securityUtils;

	@GetMapping("profil")
	public CommonResponseWs stateAccountProfil() throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.accountServiceClient.stateAccountProfil(partenaire.getId());
	}

	@GetMapping("/dues/list")
	public  CommonResponseWs partenaireDuesList(@RequestParam Integer partenaireId, @RequestParam Integer page, @RequestParam Integer size) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedServiceClient.listActivePayDifferedTransactionDeadlinesUnpaid(partenaire.getId(), page, size);
	}

	@GetMapping("/guarantee/received/list")
	public  CommonResponseWs guaranteeReceivedList(@RequestParam Integer page, @RequestParam Integer size) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorServiceClient.listReceivedGuarantees(partenaire.getId(), page, size);
	}

	@GetMapping("/allPartenaire/guarantee/received/list")
	public  CommonResponseWs AllPartenaireguaranteeReceivedList(@RequestParam Integer page, @RequestParam Integer size) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorServiceClient.listReceivedGuaranteesAllPartenaire(partenaire.getId(), page, size);
	}

	@GetMapping("guarantee/asked/list")
	public CommonResponseWs guaranteeAskedList (@RequestParam Integer partenaireId, @RequestParam Integer page, @RequestParam Integer size) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorServiceClient.listGuarantorAsked(partenaire.getId(), page, size);
	}

	@GetMapping("/guarantees/given/list")
	public CommonResponseWs listGivenGuarantees(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorServiceClient.listGuarantorGiven(partenaire.getId(), page, size);
	}


	@GetMapping("/groupe/member/state")
	public CommonResponseWs statsMembersGarantGroup(
			@RequestParam Long idGroupe,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorAccountServiceClient.statsMembersGarantGroup(idGroupe, page, size);
	}

	@GetMapping("/groupe/stats")
	public CommonResponseWs statsGarantGroup(
			@RequestParam Long idGroupe
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return payDifferedGuarantorAccountGroupeServiceClient.statsGarantGroup(idGroupe);
	}

	@GetMapping("groupe/dues")
	public CommonResponseWs duesGarantGroup(
			@RequestParam Long idGroupe,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.payDifferedGuarantorAccountGroupeServiceClient.duesGarantGroup(idGroupe, page, size);
	}

	@GetMapping("groupe/solde/corporate")
	public CommonResponseWs soldeCorporateGarantGroup(@RequestParam Long idGroupe) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.payDifferedGuarantorAccountGroupeServiceClient.soldeCorporateGarantGroup(idGroupe);
	}

	@GetMapping("/solde/corporate/caution")
	public CommonResponseWs soldeCorporateCautionGarantGroup(@RequestParam Long idGroupe) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.payDifferedGuarantorAccountGroupeServiceClient.soldeCorporateCautionGarantGroup(idGroupe);
	}

}
