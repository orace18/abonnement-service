package com.clubchretien.abonnement.abonnement.controllers.v2;

import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.v2.AbonnementServiceV2;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UpdateUserInfoWs;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Slf4j
@RestController("abonnementControllerV2")
@RequestMapping("/v2/api/abonnements")
@CrossOrigin(origins = "*")
@Tag(name = "Abonnements", description = "API pour la gestion des abonnements")
public class AbonnementV2Controller {

    @Autowired
    private AbonnementServiceV2 abonnementServiceV2;

    @Autowired
    private AbonnementService abonnementService;

    @Operation(summary = "Création de compte device_email et token", description = "Création de compte device_email chez jawupay et genereration de token pour la creation de pin")
    @GetMapping("/generate/token/forPin")
    public CommonResponseWs jawupayTokenGeneration() {
        try {
            log.info("=== Début de la fonction pour la création de PIN ===");
            CommonResponseWs commonResponseWs = abonnementServiceV2.makeCompletLoginRegisterJawuPay();
            return new CommonResponseWs(WsOperationResponseStatus.success,
                    "Token jawupay generer avec succès pour la création de PIN",
                    200, commonResponseWs.getData());
        } catch (Exception e) {
            log.error("Erreur durant la génération du token jawupay: ", e);
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur: " + e.getMessage(), 500,
                    null);
        }
    }


            @Operation(
                    summary = "Complément d'information utilisateur (Onboarding V2)",
                    description = """
                Cette API permet de :
                1. Mettre à jour les informations du profil CCAPP
                2. Créer le compte utilisateur chez JawuPay
                3. Mettre à jour le StepInSystem = COMPLEMENT_INFO
                4. Retourner le token JawuPay nécessaire à la création du PIN
                
                Header obligatoire : access-token
                
                ⚠️ Si une étape échoue, le processus est rollback (Transactional).
                """
            )
            @ApiResponses(value = {

                    @ApiResponse(
                            responseCode = "200",
                            description = "Complément info effectué avec succès - PIN requis",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommonResponseWs.class),
                                    examples = @ExampleObject(value = """
                                {
                                  "status": "success",
                                  "message": "PIN requis",
                                  "statusCode": 200,
                                  "data": {
                                    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
                                    "expireAt": "2026-02-16T12:45:00Z"
                                  }
                                }
                                """)
                            )
                    ),

                    @ApiResponse(
                            responseCode = "500",
                            description = "Erreur interne (Update user / JawuPay / Step)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(name = "Erreur JawuPay", value = """
                                        {
                                          "status": "failed",
                                          "message": "Erreur lors de la création du user chez jawupay",
                                          "statusCode": 500,
                                          "data": 12345
                                        }
                                        """),
                                            @ExampleObject(name = "Erreur Step", value = """
                                        {
                                          "status": "failed",
                                          "message": "Erreur lors de la mise à jour du stepInSystem du user",
                                          "statusCode": 500,
                                          "data": null
                                        }
                                        """)
                                    }
                            )
                    )
            })
            @PostMapping("/complete-info")
            public CommonResponseWs completeInformation(
                    @Parameter(
                            description = "Token d'authentification utilisateur CCAPP",
                            required = true,
                            in = ParameterIn.HEADER,
                            name = "access-token"
                    )
                    @RequestHeader("access-token") String accessToken,

                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "Informations utilisateur nécessaires à la création du wallet JawuPay",
                            content = @Content(
                                    schema = @Schema(implementation = UpdateUserInfoWs.class)
                            )
                    )
                    @RequestBody UpdateUserInfoWs request) {

                return abonnementServiceV2.complementInfo(request);
            }
//re

    @Operation(summary = "Vérification du step onboarding V2", description = "Retourne le statut actuel du parcours onboarding V2 de l'utilisateur. "
            +
            "Au step 2 (COMPLEMENT_INFO), vérifie automatiquement le PIN chez JawuPay et finalise l'onboarding si OK.")
    @GetMapping("/user-step")
    public CommonResponseWs checkUserStep() {
        try {
            log.info("=== Appel de l'API checkUserStepV2 ===");
            return abonnementServiceV2.checkUserStepV2();
        } catch (Exception e) {
            log.error("Erreur durant la vérification du step V2: ", e);
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Erreur: " + e.getMessage(), 500,
                    null);
        }
    }

    @GetMapping("/initialiser")
    public CommonResponseWs initializer(
            @RequestParam Long idFormuleAbonnement,
            @RequestParam(required = false) String numeroCCParent) {
        CommonResponseWs userInfo = abonnementService.initializerV2(
                Optional.ofNullable(numeroCCParent),
                idFormuleAbonnement);

        return new CommonResponseWs(
                userInfo.getStatus(),
                userInfo.getMessage(),
                userInfo.getCode(),
                userInfo.getData());
    }

    @GetMapping("/reinitialiser")
    public CommonResponseWs reinitialiser(
            @RequestParam Long idFormuleAbonnement) {

        CommonResponseWs response = abonnementServiceV2.reinitialiser(
                idFormuleAbonnement);

        return new CommonResponseWs(
                response.getStatus(),
                response.getMessage(),
                response.getCode(),
                response.getData());
    }
    
}
