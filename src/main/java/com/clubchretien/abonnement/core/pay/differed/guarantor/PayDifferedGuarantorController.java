package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorCreateWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorIndividualAcceptationWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorIndividualCreateWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorIndividualInvitationWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.account.PayDifferedAccountServiceClient;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	PayDifferedGuarantorServiceClient guarantorServiceClient;
	@Autowired
	PayDifferedAccountServiceClient payDifferedAccountServiceClient;

	@Autowired
	SecurityUtils securityUtils;

	@GetMapping("state/step")
	public CommonResponseWs stateStepGuarantor(@RequestParam Long guarantorId) throws Exception {
		return this.guarantorServiceClient.stateStepGuarantor(guarantorId);
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckGuarantor(@RequestParam Long guarantorId) throws Exception {
		return this.guarantorServiceClient.recheckGuarantor(guarantorId);
	}

	@PostMapping("/individual/demande")
	public CommonResponseWs demandeGuarantorIndividual(@RequestBody PayDifferedGuarantorIndividualInvitationWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.guarantorServiceClient.demandeGuarantorIndividual(ws);
	}

	@PutMapping("/individual/acceptation")
	public CommonResponseWs acceptationGuarantorIndividual(@RequestBody PayDifferedGuarantorIndividualAcceptationWs ws) throws Exception {
		return this.guarantorServiceClient.acceptationGuarantorIndividual(ws);
	}

	@PostMapping("/individual/create")
	public CommonResponseWs createGuarantorIndividual(@RequestBody PayDifferedGuarantorIndividualCreateWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setGuarantorPartenaireId(partenaire.getId());
		return this.guarantorServiceClient.createGuarantorIndividual(ws);
	}

	@GetMapping("/corporate/check")
	public CommonResponseWs checkCorporateGuarantor(@RequestParam String matricule) throws Exception {
		return this.guarantorServiceClient.checkCorporateGuarantor(matricule);
	}

	@PostMapping("/demande")
	public CommonResponseWs demandeGuarantor(@RequestBody PayDifferedGuarantorCreateWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.guarantorServiceClient.demandeGuarantor(ws);
	}

	@GetMapping("blocked")
	public CommonResponseWs blockedGuarantor(@RequestParam Long guarantorId, @RequestParam Boolean state) throws Exception {
		return this.guarantorServiceClient.blockedGuarantor(guarantorId, state);
	}

	@GetMapping("list/validated")
	public CommonResponseWs listGuarantorValidated(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		return this.guarantorServiceClient.listGuarantorValidated(partenaire.getId(), page, size);
	}

	@GetMapping("groupe/management/list")
	public CommonResponseWs listGarantGroupManaged(
			@RequestParam(required = false)String type,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25")  Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 401, null);
		return this.payDifferedAccountServiceClient.listGarantGroupManaged(partenaire.getId(), type, page, size);
	}

	@GetMapping("groupe/participant")
	public CommonResponseWs listGarantGroupeByParticipant(
			@RequestParam(required = false) Boolean validated,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25")  Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 401, null);
		return this.payDifferedAccountServiceClient.listGarantGroupeByParticipant(partenaire.getId(), validated, page, size);
	}

	@GetMapping("groupe/participant/invitation")
	public CommonResponseWs listGarantGroupeByParticipantInvitation(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25")  Integer size
	) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if(partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 401, null);
		return this.payDifferedAccountServiceClient.listGarantGroupeByParticipantInvitation(partenaire.getId(), page, size);
	}

	@GetMapping("groupe/members/find")
	public CommonResponseWs findMemberGarantGroupByGroupe(@RequestParam Long idMember) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if(partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 401, null);
		return this.payDifferedAccountServiceClient.findMemberGarantGroupByGroupe(idMember);
	}

	@GetMapping("/validate")
	public CommonResponseWs validateGuarantor(@RequestParam Long guarantorId, @RequestParam Boolean state) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if(partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire null", 401, null);
		return this.guarantorServiceClient.validateGuarantor(guarantorId, state);
	}

	@PostMapping("groupe/member/relance")
	public CommonResponseWs relanceMemberGarantGroup(@RequestParam Long idMember) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.payDifferedAccountServiceClient.relanceMemberGarantGroup(idMember);
	}

}
