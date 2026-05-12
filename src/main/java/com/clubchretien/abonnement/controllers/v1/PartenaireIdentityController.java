package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.PartenaireIdentityService;
import com.clubchretien.abonnement.services.PartenaireService;
import com.owomi.commons.enums.TypeDocumentEnums;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.kyc.KycDemandeWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("v1.0/partenaire")
@Tag(name = "Partenaire", description = "Partenaire")
public class PartenaireIdentityController {
	private static Logger logger = LogManager.getLogger(PartenaireIdentityController.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireService partenaireService;
	@Autowired
	PartenaireIdentityService partenaireIdentityService;
	@Autowired
	SecurityUtils securityUtils;

	@PostMapping("kyc/submission")
	//@ApiVersions({"1.0"})
	public CommonResponseWs submission(@RequestBody KycDemandeWs ws) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.partenaireIdentityService.submission(partenaire, ws);
	}


	@GetMapping("kyc/docs")
	//@ApiVersions({"1.0"})
	public CommonResponseWs kycDocs() throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.partenaireIdentityService.readKycDocs(partenaire);
	}

	@PostMapping("kyc/docs")
	//@ApiVersions({"1.0"})
	public CommonResponseWs kycDocs(@RequestBody Map<TypeDocumentEnums, String> documents) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		return this.partenaireIdentityService.uploadKycDocs(partenaire, documents);
	}


}
