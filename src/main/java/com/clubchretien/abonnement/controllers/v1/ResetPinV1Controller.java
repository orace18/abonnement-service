package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.PartenaireService;
import com.clubchretien.abonnement.services.ResetPinService;
import com.google.firebase.auth.FirebaseAuthException;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.exceptions.PermissionException;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.permission.InitPinOublier;
import com.owomi.commons.ws.permission.InitPinOublierWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;

@RestController
@RequestMapping("v1.0/resetPin")
@Tag(name = "ResetPin", description = "ResetPin")
public class ResetPinV1Controller {
	private static Logger logger = LogManager.getLogger(ResetPinV1Controller.class);
	@Autowired
	ResetPinService resetPinService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireService partenaireService;


    @PostMapping("/request")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> requestPin(@RequestBody TelephoneWs telephoneWs) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        String demandeurTelephone = partenaire.getTelephone();
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = resetPinService.requestPin(requestContext,demandeurTelephone,telephoneWs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/permissions")
    //@ApiVersions({"1.0"})
    public ResponseEntity<List<PermissionWs>> getListePermissions() throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<PermissionWs> response = resetPinService.getListePermissions(requestContext,partenaire.getTelephone());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/response/pin")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> responsePin(@RequestParam String pin,@ModelAttribute PermissionResponseWs permissionResponseWs,@RequestParam String demandeurTelephone) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = resetPinService.responsePin(requestContext, pin, partenaire, permissionResponseWs,demandeurTelephone);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //TODO: cryptage des données

    @PostMapping("/response/sms")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> responsePinSMS(@ModelAttribute PermissionResponseWs permissionResponseWs) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = resetPinService.responseSms(requestContext,partenaire, permissionResponseWs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reset")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> pinReset(@RequestParam String pin,@RequestParam String firebaseToken,@RequestParam Integer id) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs response = resetPinService.resetPin(requestContext,partenaire,pin,firebaseToken,id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

	@PostMapping("/modif")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> modifierPin(@RequestBody ModifPinWs modifPinWs) throws DataNotFoundException, AuthentificationException, OwomiException, FirebaseAuthException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = resetPinService.modifierPin(requestContext, partenaire, modifPinWs);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


	@PostMapping("/changement/{numero}/{newNumero}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> changementNumero(@PathVariable String numero, @PathVariable String newNumero) throws FirebaseAuthException, DataNotFoundException {
         partenaireService.changementNumero(numero,newNumero);
        return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
    }

    @PostMapping("/changement/{numero}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> changementMail(@PathVariable String numero, @RequestParam String mail) throws FirebaseAuthException, DataNotFoundException {
        partenaireService.changementMail(numero,mail);
        return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
    }


    @Operation(summary = "initiate pin oublier of a partenaire",
            description = "initiate pin oublier of a partenaire by giving two upline telephone in charge of confirming the partenaire as their parent ")
    @PostMapping("/init_pin_oublier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "Permission Not Exist",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "529", description = "PERMISSION AUTHORIZATION EXPIRED",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "528", description = "PERMISSION PIN AUTHORIZATION NOT FOUND",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "530", description = "PERMISION_AUTH_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "102", description = "Authenticate error",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "486", description = "Application version error",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))) })
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> initPinOublier(@RequestBody InitPinOublierWs initPinOublierWs) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        logger.debug("input of reset pin " + initPinOublierWs);
        logger.debug("input of reset pin " + initPinOublierWs.getTelephone1());
        logger.debug("input of reset pin " + initPinOublierWs.getTelephone2());
        CommonResponseWs responseWs = resetPinService.initPinOublier(new InitPinOublier(partenaire.getId(),initPinOublierWs.getTelephone1(),initPinOublierWs.getTelephone2()));
        return new ResponseEntity<>(responseWs,HttpStatus.OK);

    }

    @Operation(summary = "validate pin oublier",description = "validate pin oublier of an upline",
            security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping(value = "/response/pin_oublier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "Permission Auth not found",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "529", description = "PERMISSION AUTHORIZATION EXPIRED",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "528", description = "PERMISSION PIN AUTHORIZATION NOT FOUND",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "530", description = "PERMISION_AUTH_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "102", description = "Authenticate error",
					content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
			@ApiResponse(responseCode = "486", description = "Application version error",
					content = @Content(schema = @Schema(implementation = CommonResponseWs.class)))})
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> validatePin(@RequestBody HashDataWs hashDataWs) throws AuthentificationException, DataNotFoundException, OwomiException, PermissionException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs responseWs = resetPinService.validatePinOublier(partenaire, hashDataWs);
		return ResponseEntity.ok(responseWs);

	}

//
//    @Operation(summary = "get pending permission of a giving partenaire",
//            description = "get all pending permission of a giving partenaire",
//            security = { @SecurityRequirement(name = "bearer-key") }
//            ,tags = "permissions")
//    @GetMapping(value = "/pending/permissions")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "successful operation"),
//            @ApiResponse(responseCode = "404", description = "data not found")
//    })
//    //@ApiVersions({"1.0"})
//    public ResponseEntity<CommonResponseWs> getUserPendingPermission() throws DataNotFoundException, AuthentificationException {
//        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
//        CommonResponseWs responseWs = resetPinService.getUserPendingPermission(partenaire.getId());
//        return ResponseEntity.ok(responseWs);
//    }


}
