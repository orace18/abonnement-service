package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.services.TransactionFinanciereService;
import com.clubchretien.abonnement.ws.DepotWss;
import com.clubchretien.abonnement.ws.RechargeFlocashWs;
import com.clubchretien.abonnement.ws.RechargeWs;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.*;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.flocash.FloCashSelectPaymentInputWs;
import com.owomi.commons.ws.stripe.CreateRechargeStripeWs;
import com.owomi.database.entities.MoyenDePaiement;
import com.owomi.database.entities.MoyenReception;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("v1.0/transaction/financiere")
@Tag(name = "Transaction financière", description = "API for rechage module")
public class TransactionFinanciereController {
    private static Logger logger = LogManager.getLogger(TransactionFinanciereService.class);

    @Autowired
    TransactionFinanciereService transactionFinanciereService;
    @Autowired
    UserService userService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    PartenaireRepository partenaireRepository;


    @PostMapping("/virement")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> virement(@ModelAttribute VirementWs virementWs) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		CommonResponseWs response = transactionFinanciereService.saveVirement(userService.getRequestContext(httpServletRequest), virementWs, userService.getPartenaire(httpServletRequest));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/depot")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> depot(@ModelAttribute DepotWss depotWss) throws AuthentificationException, DataNotFoundException, FileUploadException, ParseException, OwomiException {
		CommonResponseWs response = transactionFinanciereService.saveDepot(userService.getRequestContext(httpServletRequest), depotWss, userService.getPartenaire(httpServletRequest));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/destruction")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> destruction(@ModelAttribute DepotWss depotWss) throws AuthentificationException, DataNotFoundException, FileUploadException, ParseException, OwomiException {
		CommonResponseWs response = transactionFinanciereService.saveDestruction(userService.getRequestContext(httpServletRequest), depotWss, userService.getPartenaire(httpServletRequest));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/mobileMoney")
	//@ApiVersions({"1.0"})
	public ResponseEntity<MobileMoneyResponseWs> mobileMoney(@ModelAttribute MobileMoneyWs mobileMoneyWs) throws AuthentificationException, DataNotFoundException, OwomiException {
		MobileMoneyResponseWs response = transactionFinanciereService.mobileMoney(userService.getRequestContext(httpServletRequest), mobileMoneyWs, userService.getPartenaire(httpServletRequest));
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/configMoyenBancaire")
	//@ApiVersions({"1.0"})
    public ResponseEntity<ConfigMoyenResponseWs> configMoyenBancaire(@ModelAttribute ConfigMoyenBancaireWs configMoyenBancaireWs) throws AuthentificationException, DataNotFoundException, OwomiException {
        ConfigMoyenResponseWs response = transactionFinanciereService.configMoyenBancaire(userService.getRequestContext(httpServletRequest), configMoyenBancaireWs, userService.getPartenaire(httpServletRequest));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/configMoyenMobile")
    //@ApiVersions({"1.0"})
    public ResponseEntity<ConfigMoyenResponseWs> configMoyenMobile(@ModelAttribute ConfigMoyenMobileWs configMoyenMobileWs) throws AuthentificationException, DataNotFoundException, OwomiException {
        ConfigMoyenResponseWs response = transactionFinanciereService.configMoyenMobile(userService.getRequestContext(httpServletRequest), configMoyenMobileWs, userService.getPartenaire(httpServletRequest));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /*@PostMapping("/retirer")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> retirer(@ModelAttribute RetraitWs retraitWs) throws AuthentificationException, DataNotFoundException, OwomiException {
        CommonResponseWs response = transactionFinanciereService.retirer(userService.getRequestContext(httpServletRequest), retraitWs, userService.getPartenaire(httpServletRequest));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }*/

    @GetMapping("/moyenReception")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<MoyenReception>> moyenReception() throws AuthentificationException, DataNotFoundException {
        List<MoyenReception> moyenReceptionList = transactionFinanciereService.listMoyenReception(userService.getRequestContext(httpServletRequest));
        return new ResponseEntity<>(moyenReceptionList, HttpStatus.OK);
    }

    @GetMapping("/extraitcompte/{matricule}") //solde du compte
    //@ApiVersions({"1.0"})
    public ResponseEntity<Double> getExtraitCompte(@PathVariable("matricule") String matricule) throws AuthentificationException, DataNotFoundException {
        double solde = transactionFinanciereService.extraitCompte(userService.getRequestContext(httpServletRequest), matricule);
        return new ResponseEntity<>(solde, HttpStatus.OK);
    }

    @GetMapping("/moyens")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<MoyenRecPartenaireResponseWs>> moyens() throws AuthentificationException, DataNotFoundException {
        List<MoyenRecPartenaireResponseWs> moyenRecPartenaireResponseWsList = transactionFinanciereService.listMoyenRecPartenaire(userService.getRequestContext(httpServletRequest), userService.getPartenaire(httpServletRequest), null);
        return new ResponseEntity<>(moyenRecPartenaireResponseWsList, HttpStatus.OK);
    }

    @GetMapping("/list-moyen-payement")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> listMoyenDePayement() throws AuthentificationException, DataNotFoundException {
        CommonResponseWs listMoyenDePayement = transactionFinanciereService.listMoyenDePayement(userService.getPartenaire(httpServletRequest));
        return new ResponseEntity<>(listMoyenDePayement, HttpStatus.OK);
    }

    @GetMapping("/moyens/{idCorporate}")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<MoyenRecPartenaireResponseWs>> moyens(@PathVariable("idCorporate") Integer idCorporate) throws AuthentificationException, DataNotFoundException {
        List<MoyenRecPartenaireResponseWs> moyenRecPartenaireResponseWsList = transactionFinanciereService.listMoyenRecPartenaire(userService.getRequestContext(httpServletRequest), userService.getPartenaire(httpServletRequest), idCorporate);
        return new ResponseEntity<>(moyenRecPartenaireResponseWsList, HttpStatus.OK);
    }

    @GetMapping("/corporate/{idCorporate}")// releve du compte
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<GrandLivreWs>> getHistoriqueCompteByCorporate(@PathVariable("idCorporate") int idCorporate, @RequestParam("page") int page, @RequestParam("size") int size) throws DataNotFoundException, AuthentificationException, OwomiException {
        List<GrandLivreWs> grandLivreWsList = transactionFinanciereService.getHistoriqueCompteByCorporate(userService.getRequestContext(httpServletRequest), idCorporate, page, size);
        return new ResponseEntity<>(grandLivreWsList, HttpStatus.OK);
    }

    @GetMapping("/partenaire")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<GrandLivreWs>> getHistoriqueCompteByPartenaire(@RequestParam("page") int page, @RequestParam("size") int size) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<GrandLivreWs> grandLivreWsList = transactionFinanciereService.getHistoriqueCompteByPartenaire(requestContext, partenaire, page, size);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(grandLivreWsList, httpStatus);
    }

    @GetMapping("/banques")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<BanqueWs>> banques() throws AuthentificationException, DataNotFoundException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<BanqueWs> banqueWsList = transactionFinanciereService.list(requestContext);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(banqueWsList, httpStatus);
    }
    @PostMapping("/transfertO2O")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> sendTransfert(@ModelAttribute TransfertWs transfertWs,@RequestParam String deviseTransaction) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = transactionFinanciereService.doTransfert(requestContext,partenaire, transfertWs,deviseTransaction);
        HttpStatus httpStatus = null;
        if (response.getStatus() == WsOperationResponseStatus.success)
            httpStatus = HttpStatus.OK;
        else
            httpStatus = HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(response, httpStatus);
    }


    @GetMapping("/partenaire/historique")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<HistoriqueCompteWs>> releveCompte(@RequestParam(name = "page", required = false, defaultValue = "0")  Integer page,
                                                                 @RequestParam(name = "size", required = false, defaultValue = "20")  Integer size) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<HistoriqueCompteWs> historiqueCompteWs = transactionFinanciereService.releveCompte(requestContext, partenaire, page, size);
        return new ResponseEntity<>(historiqueCompteWs, HttpStatus.OK);
    }


    // F B R
    @Operation(summary = "Get partenaire moyen de paiement list", description = "get moyen de payement list of partenaire according to his country",security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "partner not found")
    })
    @GetMapping("/moyen/pays")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<ListMoyenDePaiementWs>> listMoyenOfPaysOfAPartenaire() throws DataNotFoundException, AuthentificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //RequestContextWs requestContext = new RequestContextWs();
        //Partenaire partenaire = partenaireRepository.findOneByTelephone("22998161930");
        List<ListMoyenDePaiementWs> toreturn =  transactionFinanciereService.getListMoyensOfPaysOfPartenaire(partenaire);
        return new ResponseEntity<>(toreturn,HttpStatus.OK);
    }

    @Operation(summary = "Reload an account ", description = "Reload an account according to the means of payment", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "data not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error")
    })
    @PostMapping("/recharge")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> recharge(@Parameter(description = "recharge to do, can not be null", required = true,
            schema = @Schema(implementation = RechargeWs.class)) @Valid @ModelAttribute RechargeWs createRechargeWs) throws AuthentificationException, DataNotFoundException, FileUploadException, ParseException, OwomiException, NotificationException {
		createRechargeWs.setDate(new Date());
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		MoyenDePaiement moyenDePaiement = transactionFinanciereService.getMoyenDePaiementRepository().findOneById(createRechargeWs.getIdMoyenDePaiement());
		EntityUtils.checkIfEntityIsNull(MoyenDePaiement.class, transactionFinanciereService.getMoyenDePaiementRepository().findOneById(createRechargeWs.getIdMoyenDePaiement()), createRechargeWs.getIdMoyenDePaiement());
		CommonResponseWs response = new CommonResponseWs();
		if (!moyenDePaiement.isAutomatic()) {
			response = transactionFinanciereService.initialisationRechargeManuelle(requestContext, createRechargeWs, partenaire);
		} else if (moyenDePaiement.isAutomatic() && !moyenDePaiement.isAllowInfoAcess()) {
			response = transactionFinanciereService.initialisationRechargeAutomatiqueNotAllowAccessInfo(requestContext, createRechargeWs, partenaire);
		} else if (moyenDePaiement.isAutomatic() && moyenDePaiement.isAllowInfoAcess()) {
			response = transactionFinanciereService.rechargeAutomatiqueAllowAccessInfo(requestContext, createRechargeWs, partenaire);
		} else {
			response.setMessage("Revoir le moyen de la recharge");
			response.setStatus(WsOperationResponseStatus.failed);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "recharge with flocash", description = "Reload an account according to the means of payment flocash", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "recharge not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error")
    })
    @PostMapping("/recharge/flocach")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> rechargeWithFlocach(@Parameter(description = "recharge to do to send to flocash, can not be null", required = true,
            schema = @Schema(implementation = RechargeFlocashWs.class)) @Valid @RequestBody RechargeFlocashWs createRechargeWs) throws AuthentificationException, DataNotFoundException, FileUploadException, FloCashException, OwomiException {
		//createRechargeWs.getCreateRecharge().setDate((System.currentTimeMillis()));
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //CommonResponseWs response = transactionFinanciereService.initialisationRechargeAutomatiqueNotAllowAccessInfoCaseFlocash(requestContext, partenaire, createRechargeWs);
        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage("this service is disable");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "get option payement info", description = "Returns the list of fields to be completed for payment at flocash according to the payment option chosen by the user", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "ressource not found")
    })
    @PostMapping("/select/flocash/payementOption")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> doRechargeFlocash(@Parameter(description = "flocash select payement input to send to flocash, can not be null", required = true,
            schema = @Schema(implementation = FloCashSelectPaymentInputWs.class)) @RequestBody FloCashSelectPaymentInputWs floCashSelectPaymentInputWs) throws FloCashException, OwomiCommonsException, AuthentificationException, DataNotFoundException, OwomiException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        //CommonResponseWs response = transactionFinanciereService.getPayementOptionInfos(requestContext, floCashSelectPaymentInputWs);
        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage("this service is disable");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "validation of payement", description = "return status of payement", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "ressource not found")
    })
    @GetMapping("/flocash/payement/validation")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> validationPayementFlocash(@RequestParam String refTransactionFlocash) throws AuthentificationException, DataNotFoundException, OwomiException, NotificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        //RequestContextWs requestContext = new RequestContextWs();
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs response = transactionFinanciereService.checkRechargeFlocashStatus(requestContext, partenaire, refTransactionFlocash);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/flocash/payement/validation/{traceNumber}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> providePaymentInfoFlocash(@PathVariable String traceNumber) throws AuthentificationException, DataNotFoundException, OwomiException, NotificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
//        RequestContextWs requestContext = new RequestContextWs();
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs response = transactionFinanciereService.validateTransactionValidate(traceNumber, httpServletRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Verification of recharge ", description = "Verification of the realization of the automatic recharge of the account", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "recharge not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error")
    })
    @PostMapping("/verifier/recharge/automatique")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> validationRechargeAutomatique(@Parameter(description = "recharge status to check, can not be null", required = true,
            schema = @Schema(implementation = CheckStatusWs.class)) @RequestBody CheckStatusWs checkStatusWs, @Parameter(description = "Reference of the transaction to be verified", required = true) @RequestParam String reference) throws AuthentificationException, DataNotFoundException, OwomiException, NotificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs commonResponseWs = transactionFinanciereService.checkRechargeStatus(requestContext, partenaire, checkStatusWs, reference);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @Operation(summary = "Initialization of recharging at owomi and stripe ", description = "Initialization of recharging at owomi and stripe", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "recharge not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error")
    })
    @PostMapping("/recharge/stripe")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> rechargeWithStripe(@Parameter(description = "recharge to create, can not be null", required = true,
            schema = @Schema(implementation = CheckStatusWs.class)) @RequestBody CreateRechargeStripeWs createRechargeStripeWs) throws DataNotFoundException, StripeException, AuthentificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        //RequestContextWs requestContext = new RequestContextWs();
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        //CommonResponseWs response = transactionFinanciereService.initialisationRechargeAutomatiqueNotAllowAccessInfoCaseStripe(requestContext, partenaire, createRechargeStripeWs);
        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage("this service is disable");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Checking the recharge", description = "Checking the recharge at stripe", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "recharge not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error")
    })
    @GetMapping("/stripe/payement/validation")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> validationPayementStripe( @Parameter(description = "Transaction reference register with owomi", required = true) @RequestParam String owomiReference, @Parameter(description = "Transaction reference send by stripe ", required = true) @RequestParam String idPaymentIntent) throws DataNotFoundException, OwomiException, AuthentificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        //RequestContextWs requestContext = new RequestContextWs();
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs response = transactionFinanciereService.checkRechargeStripeStatus(requestContext, partenaire, owomiReference, idPaymentIntent);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /*@Operation(summary = "do recharge to flooz", description = "do flooz recharge", security = { @SecurityRequirement(name = "access-token")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "recharge not found"),
            @ApiResponse(responseCode = "114", description = "Marchand recharge error"),
            @ApiResponse(responseCode = "500", description = "recharge failed")
    })
    @PostMapping("/recharge/moov")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> rechargeWithFlooz(@RequestBody CreateRechargeMoovWs createRechargeMoovWs) throws DataNotFoundException, OwomiException, AuthentificationException {
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        //RequestContextWs requestContext = new RequestContextWs();
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs response = transactionFinanciereService.doRechargeMoov(requestContext, partenaire, createRechargeMoovWs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }*/

//    @GetMapping("/list/moyenPaiement")
//    //@ApiVersions({"1.0"})
//    public ResponseEntity<CommonResponseWs> moyenPaiementList() throws OwomiCommonsException, AuthentificationException, DataNotFoundException {
//        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
//        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
//        CommonResponseWs response = transactionFinanciereService.moyenPaiementList(requestContext, partenaire);
//        return new ResponseEntity<>(response, HttpStatus.OK);
//
//    }
}