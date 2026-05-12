package com.clubchretien.abonnement.controllers.v2;


import com.clubchretien.abonnement.services.PermissionService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.HashDataWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.permission.InitPinOublier;
import com.owomi.commons.ws.permission.InitPinOublierWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
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

@RestController
@RequestMapping("v2.0/permissions")
@Tag(name = "Permission", description = "Permission")
public class PermissionController {

    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;

    @Autowired
    PermissionService permissionService;

    private static Logger logger = LogManager.getLogger(PermissionController.class);



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
    //@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> initPinOublier(@RequestBody InitPinOublierWs initPinOublierWs) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        InitPinOublier pinOublierData = new InitPinOublier();
        pinOublierData.setIdDemandeur(partenaire.getId());
        pinOublierData.setUplineTelephone1(initPinOublierWs.getTelephone1());
        pinOublierData.setUplineTelephone2(initPinOublierWs.getTelephone2());
        logger.debug("input is " + initPinOublierWs);
        logger.debug("input is telephone " + initPinOublierWs.getTelephone1());
        logger.debug("input is telephone 2 " + initPinOublierWs.getTelephone2());

        CommonResponseWs responseWs = permissionService.initPinOublier(pinOublierData);
        return new ResponseEntity<>(responseWs, HttpStatus.OK);

    }

    @Operation(summary = "validate pin",description = "validate pin  of an upline",
            security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping(value = "/validate/pin")
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
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))) })
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> validatePin(@RequestBody HashDataWs hashDataWs) throws AuthentificationException, DataNotFoundException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs responseWs =  permissionService.validatePinOublier(partenaire,hashDataWs);
        return   ResponseEntity.ok(responseWs);

    }

    @Operation(summary = "get  permission of a giving partenaire",
            description = "get all  permission of a giving partenaire",
            security = { @SecurityRequirement(name = "bearer-key") }
          )
    @GetMapping()
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "404", description = "data not found")
    })
    //@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getUserPendingPermission() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs responseWs = permissionService.getUserPermission(partenaire.getId());
        return ResponseEntity.ok(responseWs);
    }


    @Operation(summary = "get permisson  by token",
            description = "get permission  by  token",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "404", description = "data not found")
    })
    //@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getPermissionByToken(@RequestParam(value = "token") String token){
        CommonResponseWs responseWs = permissionService.getPermissionByToken(token);
        return ResponseEntity.ok(responseWs);

    }

    @Operation(summary = "get permisson authorization",
            description = "get permission authorization",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/auths")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "404", description = "data not found")
    })
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getUserAuthorisation() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        CommonResponseWs responseWs = permissionService.getUserAuths(partenaire.getId());
        return ResponseEntity.ok(responseWs);

    }

    @Operation(summary = "get permisson authorization by token",
            description = "get permission authorization by auth token",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/auths/token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "404", description = "data not found")
    })
    //@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getAuthorisationbyToken(
            @Parameter(name = "token", description = "permission authorization token ", required = true)
            @RequestParam(value = "token") String token){
        CommonResponseWs responseWs = permissionService.getAuthorizationByToken(token);
        return ResponseEntity.ok(responseWs);

    }

    @Operation(summary = "Validate permission",
            description = "validate pin oublier process ",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/pin_oublier/step2")
    @ApiResponses({
			@ApiResponse(responseCode = "200", description = "successful operation"),
			@ApiResponse(responseCode = "404", description = "data not found")
	})
	//@ApiVersions({"2.0"})
	public ResponseEntity<CommonResponseWs> validatePinOublierStep2() throws DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs commonResponseWs = permissionService.initPinOublierStep2(partenaire);
		return ResponseEntity.ok(commonResponseWs);
	}

//    @Operation(summary = "validate permission",description = "validate permission",
//            security = { @SecurityRequirement(name = "bearer-key") })
//    @PostMapping(value = "/validate/permission")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful operation",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "404", description = "Permission Auth not found",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "529", description = "PERMISSION AUTHORIZATION EXPIRED",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "528", description = "PERMISSION PIN AUTHORIZATION NOT FOUND",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "530", description = "PERMISION_AUTH_NOT_FOUND",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "102", description = "Authenticate error",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
//            @ApiResponse(responseCode = "486", description = "Application version error",
//                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))) })
//    //@ApiVersions({"2.0"})
//    public ResponseEntity<CommonResponseWs> validatePinPermission(@RequestBody HashDataWs hashDataWs) throws AuthentificationException, DataNotFoundException, OwomiException {
//        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
//        CommonResponseWs responseWs =  permissionService.validatePinOublierPermission(partenaire,hashDataWs);
//        return   ResponseEntity.ok(responseWs);
//    }

}
