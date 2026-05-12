package com.clubchretien.abonnement.abonnement.services;

import com.clubchretien.abonnement.FirebaseUidGenerator;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.ws.CommonResponseWs;

import com.owomi.database.entities.UserFirebaseProvider;
import com.owomi.database.repositories.repositories.UserFirebaseProviderRepository;
import com.owomi.rest.client.auth.UserDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class FirebaseProviderService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseProviderService.class);

    @Autowired
    private UserFirebaseProviderRepository repository;

    @Autowired
    private UserDataClient userDataClient;

    /**
     * Récupère ou génère un firebaseProviderId pour un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @return firebaseProviderId (existant ou nouvellement généré)
     */
    public String getOrCreateFirebaseProviderId(Long userId) {
        if (userId == null) {
            logger.error("userId est null");
            throw new IllegalArgumentException("userId ne peut pas être null");
        }

        // Vérifier si l'utilisateur a déjà un firebaseProviderId
        Optional<UserFirebaseProvider> existingProvider = repository.findByUserId(userId);

        if (existingProvider.isPresent()) {
            String existingId = existingProvider.get().getFirebaseProviderId();
            logger.info("FirebaseProviderId existant trouvé pour userId {}: {}", userId, existingId);
            return existingId;
        }

        // Sinon, générer un nouveau firebaseProviderId unique
        String newFirebaseProviderId = generateUniqueFirebaseProviderId();

        // Sauvegarder en base (sans tokens à la création)
        UserFirebaseProvider newProvider = new UserFirebaseProvider(userId, newFirebaseProviderId);
        repository.save(newProvider);

        logger.info("Nouveau firebaseProviderId généré et sauvegardé pour userId {}: {}",
                userId, newFirebaseProviderId);

        return newFirebaseProviderId;
    }

    /**
     * Met à jour le refresh token pour un utilisateur
     */
    public void updateRefreshToken(Long userId,String idToken ,String refreshToken) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être null");
        }

        UserFirebaseProvider provider = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Aucun provider trouvé pour userId: " + userId));

        provider.setFirebaseRefreshToken(refreshToken);
        provider.setIdFirebaseToken(idToken);
        repository.save(provider);

        logger.info("Refresh token mis à jour pour userId {}", userId);
    }

    /**
     * Met à jour le context token pour un utilisateur
     */
    public void updateContextToken(Long userId, String contextToken) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être null");
        }

        UserFirebaseProvider provider = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Aucun provider trouvé pour userId: " + userId));

        provider.setContextFirebaseToken(contextToken);
        repository.save(provider);

        logger.info("Context token mis à jour pour userId {}", userId);
    }

    /**
     * Met à jour les deux tokens en une seule opération
     */
    public void updateTokens(Long userId, String refreshToken, String contextToken) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être null");
        }

        UserFirebaseProvider provider = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Aucun provider trouvé pour userId: " + userId));

        if (refreshToken != null) {
            provider.setFirebaseRefreshToken(refreshToken);
        }
        if (contextToken != null) {
            provider.setContextFirebaseToken(contextToken);
        }
        repository.save(provider);

        logger.info("Tokens mis à jour pour userId {}", userId);
    }

    /**
     * Génère un firebaseProviderId unique (vérifie qu'il n'existe pas déjà)
     */
    private String generateUniqueFirebaseProviderId() {
        String firebaseProviderId;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            firebaseProviderId = FirebaseUidGenerator.generate();
            attempts++;

            if (attempts > maxAttempts) {
                logger.error("Impossible de générer un firebaseProviderId unique après {} tentatives", maxAttempts);
                throw new RuntimeException("Échec de génération d'un firebaseProviderId unique");
            }

        } while (repository.existsByFirebaseProviderId(firebaseProviderId));

        logger.debug("FirebaseProviderId unique généré en {} tentative(s): {}", attempts, firebaseProviderId);
        return firebaseProviderId;
    }

    /**
     * Récupère le firebaseProviderId d'un utilisateur (sans en créer un nouveau)
     */
    public Optional<String> getFirebaseProviderId(Long userId) {
        return repository.findByUserId(userId)
                .map(UserFirebaseProvider::getFirebaseProviderId);
    }

    /**
     * Récupère l'userId à partir d'un firebaseProviderId
     */
    public Optional<Long> getUserIdByFirebaseProviderId(String firebaseProviderId) {
        return repository.findByFirebaseProviderId(firebaseProviderId)
                .map(UserFirebaseProvider::getUserId);
    }

    /**
     * Récupère l'entité complète UserFirebaseProvider
     */
    public Optional<UserFirebaseProvider> getProvider(Long userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Recuperer le refresh token
     */
    public String getRefreshToken(Long userId) {
        return repository.findByUserId(userId).map(UserFirebaseProvider::getFirebaseRefreshToken).get();
    }

    /**
     * Récupère l'idToken stocké pour un utilisateur
     */
    public String getIdToken(Long userId) {
        return repository.findByUserId(userId)
                .map(UserFirebaseProvider::getIdFirebaseToken)
                .orElseThrow(() -> new RuntimeException("Aucun idToken trouvé pour userId: " + userId));
    }

    /**
     * Rafraîchit le token Jawupay pour un utilisateur
     * Si le refresh Jawupay échoue, utilise le fallback Firebase
     *
     * @param userId ID de l'utilisateur
     * @return CommonResponseWs contenant le nouveau token Jawupay
     */
    public CommonResponseWs refreshJawupayToken(Long userId) {
        logger.info("=== Début refreshJawupayToken pour userId: {} ===", userId);

        try {
            // Récupérer le refresh token Jawupay depuis la BDD
            Optional<UserFirebaseProvider> providerOpt = repository.findByUserId(userId);
            if (providerOpt.isEmpty()) {
                logger.error("Aucun provider trouvé pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Aucun provider trouvé pour cet utilisateur",
                        404,
                        null
                );
            }

            UserFirebaseProvider provider = providerOpt.get();
            String refreshToken = provider.getFirebaseRefreshToken();

            if (refreshToken == null || refreshToken.isEmpty()) {
                logger.warn("Aucun refresh token trouvé pour userId: {}, passage au fallback Firebase", userId);
                return refreshJawupayTokenWithFirebaseFallback(userId);
            }

            logger.info("Tentative de refresh du token Jawupay avec le refreshToken");

            // Appeler la méthode de refresh Jawupay (à implémenter dans UserDataClient)
            CommonResponseWs refreshResponse = userDataClient.refreshJawupayToken(refreshToken);

            if (refreshResponse == null || !refreshResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.warn("Échec du refresh Jawupay, passage au fallback Firebase. Statut: {}",
                        refreshResponse != null ? refreshResponse.getStatus() : "null");
                return refreshJawupayTokenWithFirebaseFallback(userId);
            }

            // Extraire les nouveaux tokens de la réponse
            Map<String, Object> data = (Map<String, Object>) refreshResponse.getData();
            String newIdToken = (String) data.get("idToken");
            String newRefreshToken = (String) data.get("refreshToken");

            // Mettre à jour les tokens en BDD
            if (newIdToken != null && newRefreshToken != null) {
                updateRefreshToken(userId, newIdToken, newRefreshToken);
                logger.info("Tokens Jawupay rafraîchis avec succès pour userId: {}", userId);
            }

            logger.info("=== Fin refreshJawupayToken avec succès ===");
            return refreshResponse;

        } catch (Exception e) {
            logger.error("Erreur lors du refresh du token Jawupay pour userId: {}", userId, e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors du refresh: " + e.getMessage(),
                    500,
                    null
            );
        }
    }

    /**
     * Fallback: Rafraîchit le token Jawupay en utilisant le refresh token Firebase
     * 1. Rafraîchit le token Firebase pour obtenir un nouvel idToken
     * 2. Utilise ce nouvel idToken pour se connecter à Jawupay
     * 3. Met à jour tous les tokens en BDD
     *
     * @param userId ID de l'utilisateur
     * @return CommonResponseWs contenant le nouveau token Jawupay
     */
    public CommonResponseWs refreshJawupayTokenWithFirebaseFallback(Long userId) {
        logger.info("=== Début refreshJawupayTokenWithFirebaseFallback pour userId: {} ===", userId);

        try {
            // Récupérer le provider
            Optional<UserFirebaseProvider> providerOpt = repository.findByUserId(userId);
            if (providerOpt.isEmpty()) {
                logger.error("Aucun provider trouvé pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Aucun provider trouvé pour cet utilisateur",
                        404,
                        null
                );
            }

            UserFirebaseProvider provider = providerOpt.get();
            String firebaseRefreshToken = provider.getFirebaseRefreshToken();

            if (firebaseRefreshToken == null || firebaseRefreshToken.isEmpty()) {
                logger.error("Aucun Firebase refresh token disponible pour userId: {}", userId);
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Aucun Firebase refresh token disponible",
                        404,
                        null
                );
            }

            logger.info("Étape 1: Rafraîchissement du token Firebase");

            // Appeler la méthode de refresh Firebase (à implémenter dans UserDataClient)
            CommonResponseWs firebaseRefreshResponse = userDataClient.refreshFirebaseToken(firebaseRefreshToken);

            if (firebaseRefreshResponse == null || !firebaseRefreshResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.error("Échec du refresh Firebase. Statut: {}",
                        firebaseRefreshResponse != null ? firebaseRefreshResponse.getStatus() : "null");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Échec du refresh Firebase",
                        500,
                        null
                );
            }

            // Extraire le nouvel idToken Firebase
            Map<String, Object> firebaseData = (Map<String, Object>) firebaseRefreshResponse.getData();
            String newFirebaseIdToken = (String) firebaseData.get("idToken");
            String newFirebaseRefreshToken = (String) firebaseData.get("refreshToken");

            if (newFirebaseIdToken == null) {
                logger.error("Aucun idToken retourné par le refresh Firebase");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Aucun idToken retourné par Firebase",
                        500,
                        null
                );
            }

            logger.info("Étape 2: Connexion à Jawupay avec le nouvel idToken Firebase");

            // Se connecter à Jawupay avec le nouvel idToken
            CommonResponseWs loginResponse = userDataClient.loginJawuPay(newFirebaseIdToken);

            if (loginResponse == null || !loginResponse.getStatus().equals(WsOperationResponseStatus.success)) {
                logger.error("Échec de la connexion Jawupay. Statut: {}",
                        loginResponse != null ? loginResponse.getStatus() : "null");
                return new CommonResponseWs(
                        WsOperationResponseStatus.failed,
                        "Échec de la connexion Jawupay",
                        500,
                        null
                );
            }

            // Extraire les tokens Jawupay de la réponse
            Map<String, Object> jawupayData = (Map<String, Object>) loginResponse.getData();
            String jawupayIdToken = (String) jawupayData.get("idToken");
            String jawupayRefreshToken = (String) jawupayData.get("refreshToken");

            logger.info("Étape 3: Mise à jour des tokens en BDD");

            // Mettre à jour tous les tokens en BDD
            if (jawupayIdToken != null && jawupayRefreshToken != null) {
                // Mettre à jour avec les nouveaux tokens Jawupay
                updateRefreshToken(userId, jawupayIdToken, jawupayRefreshToken);
                logger.info("Tokens Jawupay mis à jour avec succès via fallback Firebase pour userId: {}", userId);
            }

            logger.info("=== Fin refreshJawupayTokenWithFirebaseFallback avec succès ===");
            return loginResponse;

        } catch (Exception e) {
            logger.error("Erreur lors du fallback Firebase pour userId: {}", userId, e);
            return new CommonResponseWs(
                    WsOperationResponseStatus.error,
                    "Erreur lors du fallback Firebase: " + e.getMessage(),
                    500,
                    null
            );
        }
    }
}