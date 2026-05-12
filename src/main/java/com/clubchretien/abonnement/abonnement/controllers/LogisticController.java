package com.clubchretien.abonnement.abonnement.controllers;

import com.clubchretien.abonnement.abonnement.services.LogisticService;
import com.owomi.commons.ws.CommonResponseWs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Tag(name = "Utilisateur & Logistique", description = "API pour les informations utilisateur et logistique")
public class LogisticController {

    @Autowired
    private LogisticService logisticService;

    @Operation(
            summary = "Obtenir les informations complètes de l'utilisateur connecté",
            description = "Récupère toutes les informations de l'utilisateur connecté ainsi que son statut d'abonnement"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations récupérées avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @GetMapping("/infos-complete")
    public CommonResponseWs getUserInfosWithAbonnement() {
        log.info("=== Endpoint: /api/user/infos-complete appelé ===");
        return logisticService.getUserInfosWithAbonnement();
    }

    @Operation(
            summary = "Obtenir les informations complètes d'un utilisateur spécifique",
            description = "Récupère toutes les informations d'un utilisateur spécifique ainsi que son statut d'abonnement (Admin uniquement)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations récupérées avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @GetMapping("/infos-complete/{userId}")
    public CommonResponseWs getUserInfosWithAbonnementByUserId(
            @Parameter(description = "ID de l'utilisateur", example = "123")
            @PathVariable Long userId
    ) {
        log.info("=== Endpoint: /api/user/infos-complete/{} appelé ===", userId);
        return logisticService.getUserInfosWithAbonnementByUserId(userId);
    }

    @Operation(
            summary = "Obtenir les informations de l'utilisateur connecté",
            description = "Récupère uniquement les informations de base de l'utilisateur connecté (sans abonnement)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations récupérées avec succès")
    })
    @GetMapping("/infos")
    public CommonResponseWs getUserInfos() {
        log.info("=== Endpoint: /api/user/infos appelé ===");
        return logisticService.getUserInfos();
    }

    @Operation(
            summary = "Obtenir le terminal de l'utilisateur",
            description = "Récupère les informations du terminal d'un utilisateur"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Terminal récupéré avec succès")
    })
    @GetMapping("/terminal/{userId}")
    public CommonResponseWs getUserTerminal(
            @Parameter(description = "ID de l'utilisateur", example = "123")
            @PathVariable Long userId
    ) {
        log.info("=== Endpoint: /api/user/terminal/{} appelé ===", userId);
        return logisticService.getUserTerminal(userId);
    }

    @Operation(
            summary = "Obtenir la session de l'utilisateur",
            description = "Récupère les informations de session d'un utilisateur"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session récupérée avec succès")
    })
    @GetMapping("/session/{userId}")
    public CommonResponseWs getUserSession(
            @Parameter(description = "ID de l'utilisateur", example = "123")
            @PathVariable Long userId
    ) {
        log.info("=== Endpoint: /api/user/session/{} appelé ===", userId);
        return logisticService.getUserSession(userId);
    }

}