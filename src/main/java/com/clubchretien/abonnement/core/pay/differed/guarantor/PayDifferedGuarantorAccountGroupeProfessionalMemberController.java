package com.clubchretien.abonnement.core.pay.differed.guarantor;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeMemberIntegrationWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeMemberProfessionalIntegrationAcceptationWs;
import com.owomi.commons.ws.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeMemberProfessionalInvitationWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.pay.differed.guarantor.PayDifferedGuarantorAccountGroupeProfessionalMemberServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/pay/differed/guarantor/account/groupe/professional/member")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class PayDifferedGuarantorAccountGroupeProfessionalMemberController {

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@Autowired
	SecurityUtils securityUtils;

	@Autowired
	PayDifferedGuarantorAccountGroupeProfessionalMemberServiceClient service;

	@GetMapping("state/step")
	public CommonResponseWs stateStepMemberGroupeProfessional(@RequestParam Long memberId) throws Exception {
		return this.service.stateStepMemberGroupeProfessional(memberId);
	}

	@GetMapping("state/step/recheck")
	public CommonResponseWs recheckStepMemberGroupeProfessional(@RequestParam Long memberId) throws Exception {
		return this.service.recheckStepMemberGroupeProfessional(memberId);
	}

	@PostMapping("invitation/create")
	public CommonResponseWs createMemberInvitationGroupeProfessional(@RequestBody PayDifferedGuarantorAccountGroupeMemberProfessionalInvitationWs ws) throws Exception {
		return this.service.createMemberInvitationGroupeProfessional(ws);
	}

	@PostMapping("integration/create")
	public CommonResponseWs createMemberIntegrationGroupeProfessional(@RequestBody PayDifferedGuarantorAccountGroupeMemberIntegrationWs ws) throws Exception {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "", 401, null);
		ws.setPartenaireId(partenaire.getId());
		return this.service.createMemberIntegrationGroupeProfessional(ws);
	}

	@PutMapping("integration/acceptance")
	public CommonResponseWs acceptanceMemberIntegrationGroupeProfessional(
			@RequestBody PayDifferedGuarantorAccountGroupeMemberProfessionalIntegrationAcceptationWs ws
	) throws Exception {
		return this.service.acceptanceMemberIntegrationGroupeProfessional(ws);
	}

	@DeleteMapping("revoke")
	public CommonResponseWs revokeMemberGroupeProfessional(@RequestParam Long memberId) throws Exception {
		return this.service.revokeMemberGroupeProfessional(memberId);
	}

	@GetMapping("list")
	public CommonResponseWs listMemberByGroupe(
			@RequestParam Long groupeId,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws Exception {
		return this.service.listMemberByGroupe(groupeId, page, size);
	}

	@GetMapping("list/employees")
	public CommonResponseWs listEmployeesByGroupe(
			@RequestParam Integer partenaireId
	) throws Exception {
		return this.service.listEmployeesByGroupe(partenaireId);
	}

	@GetMapping("add-guarantee")
	public CommonResponseWs addGuarantee(@RequestParam Integer guarantorId, @RequestParam Integer guaranteeId, @RequestParam Double caution) throws Exception {
		return this.service.addGuarantee(guarantorId, guaranteeId, caution);
	}

	@GetMapping("invite-guarantor")
	public CommonResponseWs inviteGuarantor(@RequestParam Integer guaranteeId, @RequestParam Integer guarantorId) throws Exception {
		return this.service.inviteGuarantor(guaranteeId, guarantorId);
	}

	@GetMapping("list/invitations/guarantor")
	public CommonResponseWs listInvitationsForGuarantor(@RequestParam Integer guarantorId) throws Exception {
		return this.service.listInvitationsForGuarantor(guarantorId);
	}

	@GetMapping("list/invitations/guarantee")
	public CommonResponseWs listInvitationsByGuarantee(@RequestParam Integer guaranteeId) throws Exception {
		return this.service.listInvitationsByGuarantee(guaranteeId);
	}

	@GetMapping("accept-invitation/guarantee")
	public CommonResponseWs acceptInvitationByGuarantee(@RequestParam Long invitationId, @RequestParam Double caution) throws Exception {
		return this.service.acceptInvitationByGuarantee(invitationId, caution);
	}

	@GetMapping("reject-invitation/guarantee")
	public CommonResponseWs rejectInvitationByGuarantee(@RequestParam Long invitationId) throws Exception {
		return this.service.rejectInvitationByGuarantee(invitationId);
	}

}
