package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.exception.SessionExpireException;
import com.clubchretien.abonnement.services.OperationService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.models.ErrorResponse;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DataInWs;
import com.owomi.commons.ws.OperationInfoWs;
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

@RestController("operations")
@RequestMapping("v2.0/operations")
@Tag(name = "operations", description = " v2 Operation API")
public class OperationController {
    private static Logger logger = LogManager.getLogger(OperationController.class);
    @Autowired
    OperationService operationService;

    @Autowired
    HttpServletRequest request;

    @Autowired
    UserService userService;


    @Operation(summary = "validate commande operation",
            description = "Hash command operation data and send it for validation",security = { @SecurityRequirement(name = "access-token") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
            @ApiResponse(responseCode = "404", description = "Partenaire not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "102", description = "Authenticate error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "486", description = "Application version error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "516", description = "EXPIRED SESSION",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/validate")
	//@ApiVersions({"2.0"})
    public ResponseEntity<CommonResponseWs> stepTwoToBuy(@RequestBody DataInWs hashedOperation) throws DataNotFoundException, AuthentificationException,OwomiException, SessionExpireException {
        CommonResponseWs responseWs = operationService.stepTwoToBuyProcess(request,hashedOperation.getData());
        return new ResponseEntity<>(responseWs,HttpStatus.OK);
    }

	@Operation(summary = "get public key for encryptage",
			description = "Api to use when you need a new key for encryptage ", security = {@SecurityRequirement(name = "access-token")})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successful operation",
					content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
			@ApiResponse(responseCode = "404", description = "Partenaire not found",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	})
	@GetMapping("/key")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CommonResponseWs> getKey() throws OwomiException {
		CommonResponseWs responseWs = operationService.getKey(request);
		return new ResponseEntity<>(responseWs, HttpStatus.OK);
	}


    @Operation(summary = "validate commande operation",
            description = "Hash command operation data and send it for validation",security = { @SecurityRequirement(name = "access-token") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
					content = @Content(schema = @Schema(implementation = CommonResponseWs.class))),
			@ApiResponse(responseCode = "404", description = "Partenaire not found",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "102", description = "Authenticate error",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "486", description = "Application version error",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "516", description = "EXPIRED SESSION",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/validate_operation")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CommonResponseWs> stepTwoToBuyOperation(@RequestBody OperationInfoWs operationInfo, @RequestParam Integer idPartenaire) throws DataNotFoundException, AuthentificationException,OwomiException, SessionExpireException {
        CommonResponseWs responseWs = operationService.stepTwoToBuyProcessOperation(operationInfo,idPartenaire);
        return new ResponseEntity<>(responseWs,HttpStatus.OK);
    }
}
