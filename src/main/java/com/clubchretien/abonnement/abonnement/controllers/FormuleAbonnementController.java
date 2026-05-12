package com.clubchretien.abonnement.abonnement.controllers;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.TypeAbonnement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.clubchretien.abonnement.abonnement.dto.FormuleAbonnementDto;

import com.clubchretien.abonnement.abonnement.services.FormuleAbonnementService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/formules")
@CrossOrigin(origins = "*")
@Tag(name = "Formules d'Abonnement", description = "API pour la gestion des formules d'abonnement")
public class FormuleAbonnementController {

    @Autowired
    private FormuleAbonnementService formuleAbonnementService;

    @Operation(summary = "Lister toutes les formules d'abonnement", 
               description = "Récupère la liste complète de toutes les formules d'abonnement disponibles")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des formules récupérée avec succès")
    })
    @GetMapping
    public ResponseEntity<CommonResponseWs> getAllFormules() {
        try {
            List<FormuleAbonnement> formules = formuleAbonnementService.getAllFormules();
            List<FormuleAbonnementDto> formulesDto = formules.stream()
                    .map(FormuleAbonnementDto::new)
                    .collect(Collectors.toList());
            
            String message = formulesDto.isEmpty() ? 
                "Aucune formule d'abonnement disponible" : 
                String.format("Liste des formules d'abonnement récupérée avec succès (%d formule(s))", formulesDto.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, formulesDto));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des formules d'abonnement: " + e.getMessage(), 500, e));
        }
    }

    @Operation(summary = "Lister les formules par pays", 
               description = "Récupère toutes les formules d'abonnement disponibles pour un pays donné")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des formules pour le pays récupérée avec succès")
    })
    @GetMapping("/pays/{pays}")
    public ResponseEntity<CommonResponseWs> getFormulesByPays(
            @Parameter(description = "Nom du pays", example = "France") @PathVariable String pays) {
        try {
            List<FormuleAbonnement> formules = formuleAbonnementService.getFormulesByPays(pays);
            List<FormuleAbonnementDto> formulesDto = formules.stream()
                    .map(FormuleAbonnementDto::new)
                    .collect(Collectors.toList());
            
            String message = formulesDto.isEmpty() ? 
                String.format("Aucune formule d'abonnement trouvée pour le pays '%s'", pays) : 
                String.format("Formules d'abonnement pour '%s' récupérées avec succès (%d formule(s))", pays, formulesDto.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, formulesDto));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des formules pour le pays '" + pays + "': " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour lister les formules par type d'abonnement
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<CommonResponseWs> getFormulesByType(@PathVariable TypeAbonnement type) {
        try {
            List<FormuleAbonnement> formules = formuleAbonnementService.getFormulesByType(type);
            List<FormuleAbonnementDto> formulesDto = formules.stream()
                    .map(FormuleAbonnementDto::new)
                    .collect(Collectors.toList());
            
            String message = formulesDto.isEmpty() ? 
                String.format("Aucune formule d'abonnement trouvée pour le type '%s'", type.name()) : 
                String.format("Formules d'abonnement de type '%s' récupérées avec succès (%d formule(s))", type.name(), formulesDto.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, formulesDto));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des formules par type '" + type.name() + "': " + e.getMessage(), 500, e));
        }
    }

    /**
     *
     * API pour lister les formules par durée
     */
    @GetMapping("/duree/{duree}")
    public ResponseEntity<CommonResponseWs> getFormulesByDuree(@PathVariable Duree duree) {
        try {
            List<FormuleAbonnement> formules = formuleAbonnementService.getFormulesByDuree(duree);
            List<FormuleAbonnementDto> formulesDto = formules.stream()
                    .map(FormuleAbonnementDto::new)
                    .collect(Collectors.toList());
            
            String message = formulesDto.isEmpty() ? 
                String.format("Aucune formule d'abonnement trouvée pour la durée '%s'", duree.name()) : 
                String.format("Formules d'abonnement de durée '%s' récupérées avec succès (%d formule(s))", duree.name(), formulesDto.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, formulesDto));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des formules par durée '" + duree.name() + "': " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour obtenir une formule spécifique par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponseWs> getFormuleById(@PathVariable Long id) {
        try {
            Optional<FormuleAbonnement> formuleOpt = formuleAbonnementService.getFormuleById(id);
            
            if (formuleOpt.isPresent()) {
                FormuleAbonnementDto formuleDto = new FormuleAbonnementDto(formuleOpt.get());
                return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, "Formule d'abonnement récupérée avec succès", 200, formuleDto));
            } else {
                return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, "Formule d'abonnement non trouvée", 404, null));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération de la formule d'abonnement: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour obtenir les pays disponibles
     */
    @GetMapping("/pays")
    public ResponseEntity<CommonResponseWs> getPaysDisponibles() {
        try {
            List<String> pays = formuleAbonnementService.getPaysDisponibles();
            
            String message = pays.isEmpty() ? 
                "Aucun pays disponible" : 
                String.format("Liste des pays disponibles récupérée avec succès (%d pays)", pays.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, pays));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des pays disponibles: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour obtenir les types d'abonnement disponibles
     */
    @GetMapping("/types")
    public ResponseEntity<CommonResponseWs> getTypesDisponibles() {
        try {
            List<TypeAbonnement> types = formuleAbonnementService.getTypesDisponibles();
            
            String message = types.isEmpty() ? 
                "Aucun type d'abonnement disponible" : 
                String.format("Liste des types d'abonnement disponibles récupérée avec succès (%d type(s))", types.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, types));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des types d'abonnement disponibles: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour obtenir les durées disponibles
     */
    @GetMapping("/durees")
    public ResponseEntity<CommonResponseWs> getDureesDisponibles() {
        try {
            List<Duree> durees = formuleAbonnementService.getDureesDisponibles();
            
            String message = durees.isEmpty() ? 
                "Aucune durée d'abonnement disponible" : 
                String.format("Liste des durées d'abonnement disponibles récupérée avec succès (%d durée(s))", durees.size());
            
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, durees));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération des durées d'abonnement disponibles: " + e.getMessage(), 500, e));
        }
    }

    /**
     * API pour rechercher des formules avec critères multiples
     */
    @GetMapping("/recherche")
    public ResponseEntity<CommonResponseWs> rechercherFormules(
            @RequestParam(required = false) String pays,
            @RequestParam(required = false) TypeAbonnement type,
            @RequestParam(required = false) Duree duree) {
        
        try {
            List<FormuleAbonnement> formules = formuleAbonnementService.rechercherFormules(pays, type, duree);
            List<FormuleAbonnementDto> formulesDto = formules.stream()
                    .map(FormuleAbonnementDto::new)
                    .collect(Collectors.toList());
            
            // Construire un message descriptif des critères de recherche
            StringBuilder criteres = new StringBuilder();
            if (pays != null) criteres.append("pays='").append(pays).append("' ");
            if (type != null) criteres.append("type='").append(type.name()).append("' ");
            if (duree != null) criteres.append("durée='").append(duree.name()).append("' ");

            String message = getString(criteres, formulesDto);

            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, formulesDto));
        } catch (Exception e) {
            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la recherche de formules d'abonnement: " + e.getMessage(), 500, e));
        }
    }

    private static String getString(StringBuilder criteres, List<FormuleAbonnementDto> formulesDto) {
        String criteresStr = criteres.toString().trim();
        return formulesDto.isEmpty() ?
            (criteresStr.isEmpty() ? "Aucune formule d'abonnement trouvée" :
             String.format("Aucune formule d'abonnement trouvée pour les critères: %s", criteresStr)) :
            (criteresStr.isEmpty() ?
             String.format("Recherche effectuée avec succès (%d formule(s))", formulesDto.size()) :
             String.format("Recherche effectuée avec succès pour %s (%d formule(s))", criteresStr, formulesDto.size()));
    }
}