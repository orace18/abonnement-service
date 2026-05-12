package com.clubchretien.abonnement.system;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.ArchiveParent;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.repositories.ArchiveParentRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/parrainages")
@Slf4j
@Tag(name = "System Internal", description = "Endpoints inter-services sécurisés par X-System-Key")
public class SystemParrainageController {

    private static final String SYSTEM_KEY_HEADER = "X-System-Key";

    @Value("${clubchretien.appidentifier}")
    private String systemKey;

    private final ArchiveParentRepository archiveParentRepository;
    private final AbonnementRepository abonnementRepository;

    public SystemParrainageController(ArchiveParentRepository archiveParentRepository,
                                      AbonnementRepository abonnementRepository) {
        this.archiveParentRepository = archiveParentRepository;
        this.abonnementRepository = abonnementRepository;
    }

    @Operation(summary = "Archive parrainage (inter-services)",
            description = "Retourne tous les liens parent-enfant confirmés depuis archive_parent. " +
                    "Inclut les abonnements expirés — la relation PARRAINE est permanente.")
    @GetMapping("/archive")
    public ResponseEntity<CommonResponseWs> getArchiveParrainages(
            @RequestHeader(SYSTEM_KEY_HEADER) String key) {

        if (!systemKey.equals(key)) {
            log.warn("getArchiveParrainages: clé invalide");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new CommonResponseWs(WsOperationResponseStatus.failed,
                            "Clé système invalide", HttpStatus.FORBIDDEN.value(), null));
        }

        List<ArchiveParent> archives = archiveParentRepository.findAll();
        log.info("getArchiveParrainages: {} archives au total", archives.size());

        List<Map<String, Object>> result = new ArrayList<>();
        for (ArchiveParent ap : archives) {
            if (ap.getAbonnementId() == null) continue;

            Abonnement abonnement = abonnementRepository.findById(ap.getAbonnementId()).orElse(null);
            if (abonnement == null) continue;

            // Inclure uniquement les paiements confirmés (abonnements expirés inclus — PARRAINE est permanent)
            if (!StatutPaiement.PAYE.equals(abonnement.getStatutPaiement())) continue;

            // v1 : téléphone dans parentPhoneNumber / v2 : téléphone dans numeroCCParent
            String parentPhone = ap.getParentPhoneNumber();
            if (parentPhone == null || parentPhone.isBlank()) {
                parentPhone = ap.getNumeroCCParent();
            }
            if (parentPhone == null || parentPhone.isBlank()) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("parentPhoneNumber", parentPhone);
            item.put("filleulUserId", abonnement.getUserId());
            result.add(item);
        }

        log.info("getArchiveParrainages: {} liens parent-enfant retournés", result.size());
        return ResponseEntity.ok(new CommonResponseWs(
                WsOperationResponseStatus.success, "OK", HttpStatus.OK.value(), result));
    }
}
