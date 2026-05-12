package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.PartenaireService;
import com.clubchretien.abonnement.ws.CreatePartenaireStepTwoWss;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.authentication.MobilePartenairePinWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.rest.client.compensationService.PartenaireBonusAccountServiceClient;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("v1.0/partenaire")
@Tag(name = "Partenaire", description = "Partenaire")
public class PartenaireV1Controller {
	private static Logger logger = LogManager.getLogger(PartenaireV1Controller.class);
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireService partenaireService;

	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	PartenaireBonusAccountServiceClient partenaireBonusAccountServiceClient;

    @Autowired
    SecurityUtils securityUtils;


	@PostMapping("/initinscription")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStep(@ModelAttribute CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        //  userService.getRequestContextBackOffice(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.initialSubcribe(requestContext, createPartenaireStepOneWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

    @PostMapping("/inscription")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStepNew(@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        //  userService.getRequestContextBackOffice(httpServletRequest);
        CreatePartenaireResponseWs responseWs =  partenaireService.initialSubcribeNew(requestContext,createPartenaireStepOneWs);
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }

    @PostMapping("/inscription/apple")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CreatePartenaireResponseWs> createPartenaireFirstStepApple(@RequestBody CreatePartenaireStepOneWs createPartenaireStepOneWs) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        //  userService.getRequestContextBackOffice(httpServletRequest);
        CreatePartenaireResponseWs responseWs =  partenaireService.initialSubcribeApple(requestContext,createPartenaireStepOneWs);
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/authenticate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CodeWs> authenticate(@RequestParam String firebaseToken, @RequestParam String username) throws DataNotFoundException, NotificationException, WesternSmsException, OwomiException, AuthentificationException, FirebaseAuthException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        //  userService.getRequestContextBackOffice(httpServletRequest);
		CodeWs codeWs = partenaireService.authenticate(requestContext, firebaseToken, username);
		return new ResponseEntity<>(codeWs, HttpStatus.OK);
	}

	@PostMapping("/complementinfo")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireSecondStep(@ModelAttribute CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws AuthentificationException, OwomiException, FileUploadException, DataNotFoundException, IOException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.subcribeSecondStep(requestContext, partenaire, createPartenaireStepTwoWss);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	/*@PostMapping("/pininfo")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CreatePartenaireResponseWs> createPartenaireThirdStep(@ModelAttribute CreatePartenaireStepTreeWs createPartenaireStepTreeWs) throws AuthentificationException, NotificationException, DataNotFoundException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CreatePartenaireResponseWs responseWs = partenaireService.subcribeThirdStep(requestContext,partenaire, createPartenaireStepTreeWs);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}*/
	@PostMapping("/updateFireBaseToken")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> updateFireBaseToken(@RequestParam("fireBaseToken") String fireBaseToken) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.updateFireBaseToken(requestContext,partenaire, fireBaseToken);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping("/checkStatusMRP")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> checkMoyenRecPartenaireStatus() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.checkMoyenRecpStatus(requestContext,partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping("/infos")
	//@ApiVersions({"1.0"})
    public ResponseEntity<PartenaireWs> infos(@RequestParam String telephone) throws OwomiException, AuthentificationException, DataNotFoundException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        PartenaireWs partenaireWs = partenaireService.getInfos(requestContext,telephone);
        return new ResponseEntity<>(partenaireWs, HttpStatus.OK);
    }

    @GetMapping("/associate/apple")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> appleNumberAssociate(@RequestParam String telephone) throws OwomiException, AuthentificationException, DataNotFoundException, WesternSmsException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.appleNumberAssociate(requestContext,telephone,partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping("/check/apple")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> appleNumberCheck(@RequestParam String codeOtp) throws OwomiException, AuthentificationException, DataNotFoundException, WesternSmsException, NotificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.appleNumberCheck(requestContext,partenaire,codeOtp);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    /*@PostMapping("/changePin/stepOne")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> pinReinitializeStepOne() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.pinReinitializeStepOne(requestContext,partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }*/

    @PostMapping("/changePin/stepTwo")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> pinReinitializeStepTwo(@RequestParam String code) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.pinReinitializeStepTwo(requestContext,partenaire,code);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/changePin/stepThree")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> pinReinitializeStepThree(@RequestParam String newPin) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.pinReinitializeStepThree(requestContext,partenaire, newPin);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }



    @GetMapping("/devises")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<DeviseWs>> getDevisesList() throws DataNotFoundException, AuthentificationException, FirebaseMessagingException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        List<DeviseWs> deviseWsList = partenaireService.getDeviseList(requestContext);
        return new ResponseEntity<>(deviseWsList, HttpStatus.OK);
    }

    @GetMapping("/devise")
    //@ApiVersions({"1.0"})
    public ResponseEntity<DeviseWs> getDevisePartenaire() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        DeviseWs devise = partenaireService.getDevisePartenaire(requestContext,partenaire);
        return new ResponseEntity<>(devise, HttpStatus.OK);
    }


    @GetMapping("/ajust")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> ajustPartner() throws DataNotFoundException, AuthentificationException, OwomiException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.creationPartenaireBoucle(requestContext);
        return new ResponseEntity<>(new CommonResponseWs(WsOperationResponseStatus.success,null,null,null,null,null,null), HttpStatus.OK);
    }

    @GetMapping("/ajuster")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> correctIdPartenaire() throws DataNotFoundException, AuthentificationException, OwomiException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.correctPartenaireNeo4j(requestContext);
        return new ResponseEntity<>(new CommonResponseWs(WsOperationResponseStatus.success,null,null,null,null,null,null), HttpStatus.OK);
    }

    @GetMapping("/version")
	//@ApiVersions({"1.0"})
    public ResponseEntity<VersionOwomiWs> getVersion() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        VersionOwomiWs versionOwomiWs = partenaireService.getVersion(requestContext);
        return new ResponseEntity<>(versionOwomiWs, HttpStatus.OK);
    }

    @GetMapping("/update")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getVersion(@RequestParam Integer id) throws DataNotFoundException, AuthentificationException, OwomiException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.updateCommandeOwomi(requestContext,id);
        return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
    }



    @GetMapping("/push")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> pushOperationNeo4j(@RequestParam Integer id) throws DataNotFoundException, AuthentificationException, OwomiException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.pushOperationNeo4j(requestContext,id);
        return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
    }

    @GetMapping("/civ")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> bugCiv() throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException, IOException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.dataPlatform(requestContext);
        return new ResponseEntity<>(new CommonResponseWs(WsOperationResponseStatus.success,null,null,null,null,null,null), HttpStatus.OK);
    }

    @GetMapping("/civ2")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> bugCiv2() throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException, IOException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        partenaireService.achats(requestContext);
        return new ResponseEntity<>(new CommonResponseWs(WsOperationResponseStatus.success,"value",null,null,null,null,null), HttpStatus.OK);
	}

	@GetMapping("/ajustNumber")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> ajustNumber() throws DataNotFoundException, AuthentificationException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		partenaireService.updateNumberGabon(requestContext);
		return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
	}

	@PostMapping("/push/image")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> pushImage(@ModelAttribute CreatePartenaireStepTwoWss createPartenaireStepTwoWss) throws AuthentificationException, OwomiException, FileUploadException, DataNotFoundException, IOException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireService.pushImage(requestContext, partenaire, createPartenaireStepTwoWss);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/profil/image")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> updateProfilImage(@RequestParam String url) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireService.updateProfilImage(requestContext, partenaire, url);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/bonus/solde")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeBonusPartenaire() throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireBonusAccountServiceClient.soldeBonusResponse(partenaire.getId());
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/bonus/solde/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeBonusPartenaire(
			@RequestParam String telephone
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireBonusAccountServiceClient.soldeBonusResponse(partenaire.getId());
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/bonus/releve")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeBonusPartenaire(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs responseWs = partenaireBonusAccountServiceClient.statementAccountBonusResponse(
				partenaire.getId(), page, size, true
		);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}

	@PostMapping("/bonus/releve/master")
    //@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> soldeBonusPartenaire(
            @RequestParam String telephone,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer size
    ) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
        Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs responseWs = partenaireBonusAccountServiceClient.statementAccountBonusResponse(
                partenaire.getId(), page, size, true
        );
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }

    @GetMapping("/get/parent")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getParentPartenaire() throws Exception {
        //Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.partenaireForParent(partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/verify/pin")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> verifyPinPartenaire(
            @RequestParam String pin
    ) throws AuthentificationException, DataNotFoundException, OwomiException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        String token = httpServletRequest.getHeader("access-token");
        //String token = securityUtils.getAccessToken(httpServletRequest);
        CommonResponseWs responseWs = partenaireService.verifyPinPartenaire(partenaire, pin, token);
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }

    @PostMapping("/verify/partenaire/pin")
    public CommonResponseWs verifyPin(@RequestBody MobilePartenairePinWs ws) throws Exception {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        SessionInfoWs session = this.securityUtils.getSessionInfoFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        return partenaireService.verifyPin(session, partenaire, ws);
    }

    @PutMapping("/modify/validate/info")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> modifyInfoPartenaire(@RequestBody PartenaireUpdateInfoWs partenaireUpdateInfoWs) throws Exception {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        CommonResponseWs commonResponseWs = partenaireService.modifyInfoPartenaire(partenaire, partenaireUpdateInfoWs);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("send/code/modification")
    public ResponseEntity<CommonResponseWs> sendOtpModification() throws AuthentificationException, OwomiException, DataNotFoundException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        CommonResponseWs responseWs = partenaireService.sendOtpModification(partenaire);
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }

}
