package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.CorporateService;
import com.clubchretien.abonnement.services.TransactionFinanciereService;
import com.clubchretien.abonnement.ws.CorporateDocumentWs;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.FileUploadException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
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
@RequestMapping("v1.0/corporate")
@Tag(name = "Corporate", description = "Corporate")
public class CorporateV1Controller {
	private static Logger logger = LogManager.getLogger(CorporateV1Controller.class);
	@Autowired
	CorporateService corporateService;
	@Autowired
	TransactionFinanciereService transactionFinanciereService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	SecurityUtils securityUtils;

	@PostMapping("/addCorporate")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CreateCorporateResponseWs> createCorporate(@ModelAttribute CreateCorporateWs createCorporateWs, @RequestParam(required = false, defaultValue = "none") String type) throws AuthentificationException, DataNotFoundException, OwomiException {
		logger.debug("In createCorporate ===> {}", createCorporateWs);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CreateCorporateResponseWs response = corporateService.createCorporate(requestContext, partenaire, createCorporateWs, type);
		logger.debug("Out createCorporate ===> {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


	@PostMapping("/addPartnersCorporate/{id}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<AddPartenaireToCorporateResponseWs> addParternsCorporate(@PathVariable("id") int id, @ModelAttribute PartenaireCorporateWs pcWs) throws AuthentificationException, OwomiException, DataNotFoundException {
		//Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		AddPartenaireToCorporateResponseWs response = corporateService.addPartenaireToCorporate(requestContext, id, pcWs);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PutMapping("/activeCorporate/{id}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> activePartenaireInvitation(@PathVariable("id") int id) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		corporateService.activedInvitations(requestContext, partenaire, id);
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		commonResponseWs.setStatus(WsOperationResponseStatus.success);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(commonResponseWs, httpStatus);
	}

	@GetMapping("/listFormeJuridique")
	// liste des forme juridiques, doit normalement etre recupéré pour le pays de l'uitilisateur
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<FormeJuridiqueWs>> listFormeJuridique() throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		List<FormeJuridiqueWs> formeJuridiqueWsList = corporateService.listFormeJuridique(requestContext, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(formeJuridiqueWsList, httpStatus);
	}


	@PostMapping("/associate/typeDocumentToFormeJuridique")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> associateTypeDocumentAndFormeJuridique(@RequestParam Integer idTypeDocument, @RequestParam Integer idFormeJuridique) throws DataNotFoundException, AuthentificationException {
		//Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		//RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		RequestContextWs requestContext = new RequestContextWs();
		CommonResponseWs commonResponseWs = corporateService.associateFormeJuridiqueToTypeDocument(requestContext, idTypeDocument, idFormeJuridique);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("/typeDocumentList/ByFormJuridique")// types documents pour une forme juridique
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> getListTypeDocumentByFormeJuridique(@RequestParam Integer idFormeJuridique) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = corporateService.getTypeDocumentByFormeJuridique(requestContext, idFormeJuridique);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping("/listTypeDocument")
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<TypeDocumentWs>> listTypeDocumentByFormeJuridique() throws AuthentificationException, DataNotFoundException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		List<TypeDocumentWs> typeDocumentList = corporateService.listTypeDocument(requestContext);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(typeDocumentList, httpStatus);
	}

	@PostMapping("/uploadDocument")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadDocuments(@ModelAttribute CorporateDocumentWs corporateDocumentWs) throws DataNotFoundException, AuthentificationException, FileUploadException, OwomiException, IOException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.uploadDocument(requestContext, corporateDocumentWs);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/")
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<CorporateWs>> getCorporate(
			@RequestParam(required = false, defaultValue = "false") Boolean certified
	) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		List<CorporateWs> listCorporates = corporateService.getCorporate(requestContext, partenaire, certified);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(listCorporates, httpStatus);
	}

	@GetMapping("/reservations")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> getReservations(
			@RequestParam String matricule,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "50") Integer size,
			@RequestParam(required = false, defaultValue = "false") Boolean leve
	) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = transactionFinanciereService.listReservation(
				matricule, leve, page, size
		);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	/**
	 * get validated corporate of the partenar
	 *
	 * @return
	 * @throws DataNotFoundException
	 * @throws AuthentificationException
	 */

	@GetMapping("/validated")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> getCorporateValide() throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs listCorporatesValide = corporateService.getCorporateValider(requestContext, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(listCorporatesValide, httpStatus);
	}

	@GetMapping("/habilete") // liste des habiletés
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<HabileteWs>> getHabileteList() throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		List<HabileteWs> habileteList = corporateService.getHabileteList(requestContext);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(habileteList, httpStatus);
	}

	@GetMapping("/listMembersCorporate") // membres d'un corporate
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<PartenaireWs>> getCorporateMembers(@RequestParam Integer idCorporate) throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		List<PartenaireWs> partenaireWsList = corporateService.getCorporateMembers(requestContext, idCorporate);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(partenaireWsList, httpStatus);
	}

	@PostMapping("/prestashopId/{id}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> setCorporateIdPrestashop(@PathVariable("id") Integer idPrestashop) throws DataNotFoundException, AuthentificationException, FileUploadException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.setCorporatePrestashopId(requestContext, partenaire, idPrestashop);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	//TODO: collection agent-master
	@PostMapping("/corporateNature/Agent")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> setCorporateNatureAgent(@ModelAttribute AgentWs agentWs) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.traitementForBecomeAgent(requestContext, partenaire, agentWs);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	//TODO: collection agent-master
	@PostMapping("/corporateNature/master")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> setCorporateNatureMaster(@ModelAttribute AgentWs agentWs) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.traitementForBecomeMaster(requestContext, partenaire, agentWs);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/checkAgentMasterAllowed")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> checkAgentMasterAllowed() throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		CommonResponseWs response = corporateService.checkMasterAgentAllowed(partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/corporateNature/Marchand/{idCorporate}") // devenir marchand second API
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> setCorporateNatureMarchand(@PathVariable Integer idCorporate) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs response = corporateService.traitementForBecomeMarchand(requestContext, partenaire, idCorporate);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}



    @GetMapping("/prestashopId")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CorporateResponseWs> getCorporateList() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CorporateResponseWs corporateWsList = corporateService.getCorporateIdPrestashop(requestContext,partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(corporateWsList, httpStatus);
    }

    @GetMapping("/checkDocument") // check documents API
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<CheckDocumentWs>> checkDocuments(@RequestParam Integer idCorporate) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        List<CheckDocumentWs> checkDocumentWsList = corporateService.checkDocuments(requestContext,idCorporate);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(checkDocumentWsList, httpStatus);
    }

    @PostMapping("/payKit/Agent")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> payKitAgent(@RequestParam Integer idCorporate) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs response = corporateService.payKitDemarrageAgent(requestContext,partenaire, idCorporate);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/payKit/Marchand") // devenir marchand
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> payKitMarchand(@RequestParam Integer idCorporate,@RequestParam Integer typeKit) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs response = corporateService.payKitDemarrageMarchand(requestContext,partenaire, idCorporate,typeKit);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/natureCorporate") // nature corporate list
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<NatureCorporateWs>> getNatureCorporate() throws AuthentificationException, DataNotFoundException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        List<NatureCorporateWs> natureCorporateWsList = corporateService.getNatureCorporate(requestContext);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(natureCorporateWsList, httpStatus);
    }

    @GetMapping("/product/merchants") // nature corporate list
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getProductMerchant() throws AuthentificationException, DataNotFoundException {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs responseWs = corporateService.getProductMerchantList(requestContext);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(responseWs, httpStatus);
    }

    @GetMapping("/isIdentifier") // is identifier
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> isIdentifier() throws AuthentificationException, DataNotFoundException, OwomiException {
        //Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = corporateService.isIdentifier(requestContext,partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

	@GetMapping("isIdentify")
	public ResponseEntity<CommonResponseWs> isIdentify() throws AuthentificationException, DataNotFoundException, OwomiException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = corporateService.isIdentify(requestContext,partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(commonResponseWs, httpStatus);
	}

    @GetMapping("/isNotIdentifier")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> isNotIdentifier() throws AuthentificationException, DataNotFoundException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = corporateService.isNotIdentifier(requestContext,partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

    @GetMapping("/checkPiece")
	// check piecemobile-service_mobile-service.0.mhuz19o7nwge@vmi200001.contaboserver.net    | 2020-04-28 17:05:23.817 UTC [http-nio-7000-exec-2] c.o.s.s.UserService DEBUG: le token pour recupérer les infos est ====================> b5ccdcfe3fc729d950e1739921a3b0a1
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> checkPiece() throws AuthentificationException, DataNotFoundException {
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = corporateService.checkPiece(requestContext,partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(commonResponseWs, httpStatus);
    }

	@GetMapping("solde")
	//@ApiVersion({"2.0"})
	public CommonResponseWs solde(@RequestParam String matricule) throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		return corporateService.soldeCorporate(partenaire, matricule);
	}

    @PostMapping("/deleteDocument") // delete document
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> deleteDocument(@RequestParam Integer idCorporate,@RequestParam String abrev) throws AuthentificationException, DataNotFoundException {
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = corporateService.deleteDocument(requestContext, idCorporate, abrev);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(commonResponseWs, httpStatus);
	}


	@GetMapping("/matricule") // get corporate by matricule
	//@ApiVersions({"1.0"})
	public ResponseEntity<CorporateWs> getCorporateByMatricule(@RequestParam String matricule) throws AuthentificationException, DataNotFoundException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		CorporateWs corporateWs = corporateService.getCorporateByMatricule(requestContext, matricule);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(corporateWs, httpStatus);
	}

	@DeleteMapping("/account")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> deleteAccount() throws Exception {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
		CommonResponseWs commonResponseWs = this.corporateService.deleteAccount(partenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}
}
