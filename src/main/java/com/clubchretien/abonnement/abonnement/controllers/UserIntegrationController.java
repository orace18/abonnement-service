//package com.clubchretien.abonnement.abonnement.controllers;
//
//import com.clubchretien.abonnement.abonnement.services.LogisticService;
//import com.clubchretien.abonnement.abonnement.services.UserSubscriptionService;
//import com.owomi.commons.enums.WsOperationResponseStatus;
//import com.owomi.commons.ws.CommonResponseWs;
//import com.owomi.database.entities.User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/users")
//@CrossOrigin(origins = "*")
//public class UserIntegrationController {
//
//    @Autowired
//    private UserSubscriptionService userSubscriptionService;
//
//    @Autowired
//    private LogisticService  logisticService;
//
//    /**
//     * Récupère un utilisateur par son ID
//     */
//    @GetMapping("/{userId}")
////    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
//    public ResponseEntity<CommonResponseWs> getUserById(@PathVariable Long userId) {
//        try {
//            User user = userSubscriptionService.getUserById(userId);
//            if (user != null) {
//                return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, "Utilisateur trouvé avec succès", 200, user));
//            } else {
//                return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, "Utilisateur non trouvé", 404, null));
//            }
//        } catch (Exception e) {
//            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la récupération de l'utilisateur: " + e.getMessage(), 500, e));
//        }
//    }
//
//    /**
//     * Récupère un utilisateur par son email
//     */
//    @GetMapping("/email/{email}")
////    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
//    public ResponseEntity<CommonResponseWs> getUserByEmail(@PathVariable String email) {
//        try {
//            User user = userSubscriptionService.getUserByEmail(email);
//            String message = user != null ?
//                String.format("Utilisateur trouvé pour l'email '%s'", email) :
//                String.format("Aucun utilisateur trouvé pour l'email '%s'", email);
//            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, user));
//        } catch (Exception e) {
//            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la recherche par email: " + e.getMessage(), 500, e));
//        }
//    }
//
//    /**
//     * Récupère un utilisateur par son numéro de téléphone
//     */
//    @GetMapping("/phone/{phoneNumber}")
////    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
//    public ResponseEntity<CommonResponseWs> getUserByPhone(@PathVariable String phoneNumber) {
//        try {
//            User user = userSubscriptionService.getUserByTelephone(phoneNumber);
//            String message = user != null ?
//                String.format("Utilisateur trouvé pour le numéro '%s'", phoneNumber) :
//                String.format("Aucun utilisateur trouvé pour le numéro '%s'", phoneNumber);
//            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.success, message, 200, user));
//        } catch (Exception e) {
//            return ResponseEntity.ok(new CommonResponseWs(WsOperationResponseStatus.error, "Erreur lors de la recherche par téléphone: " + e.getMessage(), 500, e));
//        }
//    }
//
//  /*  @GetMapping("get-user-infos")
//    public CommonResponseWs getUserInfos() {
//     CommonResponseWs responseWs =  logisticService.getUserInfos();
//     if (responseWs != null) {
//
//     }
//
//    }
//
//   */
//}