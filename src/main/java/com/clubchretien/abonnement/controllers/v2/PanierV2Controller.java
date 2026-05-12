package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.PanierService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.models.ErrorResponse;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DataInWs;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

@RestController
@RequestMapping("v2.0/paniers")
@Tag(name = "Panier", description = "Panier")
public class PanierV2Controller {
	private static Logger logger = LogManager.getLogger(PanierV2Controller.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	PanierService panierService;

	@Operation(summary = "submit hash command", description = "submit operation by hashing all containing data", security = {@SecurityRequirement(name = "access-token")})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "Partenaire not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "102", description = "Authenticate error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "486", description = "Application version error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
    @PostMapping(value = "/hash", consumes = {MediaType.APPLICATION_JSON}, produces = {MediaType.APPLICATION_JSON})
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> getPanierHash(@RequestBody DataInWs hashPanier) throws DataNotFoundException, AuthentificationException, OwomiException {
        CommonResponseWs responseWs = panierService.getCommandInfo(request,hashPanier.getData());
        return new ResponseEntity<>(responseWs, HttpStatus.OK);
    }

}
