//package com.clubchretien.abonnement.abonnement.controllers;
//
//import com.clubchretien.abonnement.abonnement.dto.AbonnementDto;
//import com.clubchretien.abonnement.abonnement.services.AbonnementService;
//import com.clubchretien.abonnement.abonnement.services.DebugAbonnementService;
//import com.owomi.commons.enums.WsOperationResponseStatus;
//import com.owomi.commons.ws.CommonResponseWs;
//import com.owomi.commons.ws.UserProfileWs;
//import com.owomi.database.entities.Abonnement;
//import com.owomi.database.entities.enums.StatutPaiement;
//import com.owomi.database.repositories.repositories.AbonnementRepository;
//import com.owomi.jwt_lib.utils.CustomJwtUtils;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/abonnements")
//public class AbonnementDebugController {
//    private static Logger log = LogManager.getLogger(AbonnementService.class);
//
//
//    @Autowired
//    private DebugAbonnementService debugService;
//    @Autowired AbonnementService abonnementService;
//    @Autowired
//    AbonnementRepository abonnementRepository;
//
//    // Ajoutez cet endpoint temporaire
//    @GetMapping("/debug/mon-abonnement")
//    public CommonResponseWs debugMonAbonnement() {
//        return debugService.debugAbonnementUtilisateur();
//    }
//
//    @GetMapping("/debug/abonnement/{userId}")
//    public CommonResponseWs debugAbonnementUser(@PathVariable Long userId) {
//        return debugService.debugAbonnementUtilisateurById(userId);
//    }
//
//    @PutMapping("/debug/reactiver-dernier-abonnement")
//    public CommonResponseWs reactiverDernierAbonnement() {
//        try {
//            UserProfileWs userProfile = CustomJwtUtils.getCurrentUser();
//            Long userId = userProfile.getId();
//
//            log.info("🔧 Réactivation dernier abonnement pour userId: {}", userId);
//
//            // 1. Désactiver TOUS les abonnements
//            List<Abonnement> tous = abonnementService.getHistoriqueAbonnements(userId);
//            for (Abonnement abo : tous) {
//                abo.setActif(false);
//                abonnementRepository.save(abo);
//                log.info("⚠️ Désactivation abonnement ID: {}", abo.getId());
//            }
//
//            // 2. Trouver le dernier abonnement PAYÉ
//            Optional<Abonnement> dernierPaye = tous.stream()
//                    .filter(a -> a.getStatutPaiement() == StatutPaiement.PAYE)
//                    .max(Comparator.comparing(Abonnement::getDateAchat));
//
//            if (dernierPaye.isEmpty()) {
//                return new CommonResponseWs(WsOperationResponseStatus.failed,
//                        "Aucun abonnement payé trouvé", 404, null);
//            }
//
//            // 3. Activer le dernier payé
//            Abonnement abo = dernierPaye.get();
//            abo.setActif(true);
//            abonnementRepository.save(abo);
//
//            log.info("✅ Abonnement ID {} réactivé avec succès", abo.getId());
//
//            return new CommonResponseWs(WsOperationResponseStatus.success,
//                    "Abonnement ID " + abo.getId() + " réactivé avec succès",
//                    200,
//                    new AbonnementDto(abo));
//
//        } catch (Exception e) {
//            log.error("❌ Erreur réactivation:", e);
//            return new CommonResponseWs(WsOperationResponseStatus.error,
//                    "Erreur: " + e.getMessage(), 500, null);
//        }
//    }
//}