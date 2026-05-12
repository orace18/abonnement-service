package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeMemberIntegrationWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeMemberInvitationWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeSocialMemberServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/groupe/social/member")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountGroupeSocialMemberController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;


	@Autowired
	PayDifferedGuarantorAccountGroupeSocialMemberServiceClient service;

	@GetMapping("state/step")
	public CommonResponseWs stateStepMemberGroupeSocial(@RequestParam Long memberId) throws Exception {
		return this.service.stateStepMemberGroupeSocial(memberId);
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckStepMemberGroupeSocial(@RequestParam Long memberId) throws Exception {
		return this.service.recheckStepMemberGroupeSocial(memberId);
	}

	@PostMapping("invitation/create")
	public CommonResponseWs createMemberInvitationGroupeSocial(@RequestBody PayDifferedGuarantorAccountGroupeMemberInvitationWs ws) throws Exception {
		return this.service.createMemberInvitationGroupeSocial(ws);
	}

	@PostMapping("integration/create")
	public CommonResponseWs createMemberIntegrationGroupeSocial(@RequestBody PayDifferedGuarantorAccountGroupeMemberIntegrationWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.service.createMemberIntegrationGroupeSocial(ws);
	}

	@PutMapping("invitation/acceptance")
	public CommonResponseWs acceptanceMemberInvitationGroupeSocial(@RequestParam Long memberId, @RequestParam Boolean state) throws Exception {
		return this.service.acceptanceMemberInvitationGroupeSocial(memberId, state);
	}

	@PutMapping("invitation/acceptance/management")
	public CommonResponseWs acceptanceMemberManagementGroupeSocial(@RequestParam Long memberId, @RequestParam Boolean state) throws Exception {
		return this.service.acceptanceMemberManagementGroupeSocial(memberId, state);
	}

	@PutMapping("integration/acceptance")
	public CommonResponseWs acceptanceMemberIntegrationGroupeSocial(
			@RequestParam Long idMember, @RequestParam Boolean state,
			@RequestParam Double engagement
	) throws Exception {
		return this.service.acceptanceMemberIntegrationGroupeSocial(idMember, state, engagement);
	}

	@DeleteMapping("revoke")
	public CommonResponseWs revokeMemberGroupeSocial(@RequestParam Long memberId) throws Exception {
		return this.service.revokeMemberGroupeSocial(memberId);
	}

	@GetMapping("list")
	public CommonResponseWs listMemberByGroupe(
			@RequestParam Long groupeId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		return this.service.listMemberByGroupe(groupeId, page, size);
	}
}
