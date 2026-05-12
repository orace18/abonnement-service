package com.clubchretien.abonnement.core.partenaire.controllers;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.partenaire.services.MobilePartenaireAuthenticationService;
import com.clubchretien.abonnement.core.partenaire.ws.MobilePartenaireCivilInformationCreateWs;
import com.clubchretien.abonnement.core.partenaire.ws.MobilePartenaireCountryCreateWs;
import com.clubchretien.abonnement.core.partenaire.ws.MobilePartenairePinCreateWs;
import com.clubchretien.abonnement.ws.DemandeSuppressionPartenaireWs;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.database.entities.Partenaire;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@RestController
@RequestMapping("/mobile/v1.0/partenaire/account")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})
public class MobilePartenaireAccountController {
	private static Logger logger = LogManager.getLogger(MobilePartenaireAccountController.class);

	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	MobilePartenaireAuthenticationService service;
	@Autowired
	SecurityUtils securityUtils;

	@GetMapping("session")
	public CommonResponseWs session() throws Exception {
		SessionInfoWs session = this.securityUtils.getSessionInfoFromHeader(httpServletRequest);
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, session);
	}


	@PostMapping("subscription/state")
	public CommonResponseWs stateSubscription() throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		SessionInfoWs session = this.securityUtils.getSessionInfoFromHeader(httpServletRequest);
		return this.service.stateSubscription(session);
	}

	@PostMapping("configuration/pin")
	public CommonResponseWs configurePin(@RequestBody MobilePartenairePinCreateWs ws) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		SessionInfoWs session = this.securityUtils.getSessionInfoFromHeader(httpServletRequest);
		return this.service.configurePin(session, ws);
	}

	@PostMapping("configuration/civil-information")
	public CommonResponseWs configureCivilInformation(@RequestBody MobilePartenaireCivilInformationCreateWs ws) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.configureCivilInformation(partenaire, ws);
	}

	@PostMapping("configuration/country")
	public CommonResponseWs configureCountry(@RequestBody MobilePartenaireCountryCreateWs ws) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.configureCountry(partenaire, ws);
	}

	@PostMapping("configuration/profil/image")
	public CommonResponseWs configureProfilPhoto(@RequestParam String url) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.service.configureProfilPhoto(partenaire, url);
	}

	@PutMapping("delete")
	public CommonResponseWs deleteAccount() throws Exception {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
        return this.service.deleteAccount(partenaire);
    }

	@GetMapping("isDeleted")
	public CommonResponseWs isDeleted() throws Exception {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        return this.service.isDeleted(httpServletRequest);
    }

	@PostMapping("initialize-delete")
	public CommonResponseWs initializeDelete(@RequestBody (required = true)DemandeSuppressionPartenaireWs demandeSuppressionPartenaireWs) throws Exception {
        return this.service.initializeDelete(demandeSuppressionPartenaireWs);
    }

	@PutMapping("cancel-delete")
	public CommonResponseWs cancelDelete(@RequestParam (required = true) Integer partenaireId) throws Exception {
        return this.service.cancelDelete(partenaireId);
    }

	@GetMapping("is-delete-in-progress")
	public CommonResponseWs isDeleteInProgress(@RequestParam (required = true) Integer partenaireId) throws Exception {
        return this.service.isDeleteInProgress(partenaireId);
    }
}
