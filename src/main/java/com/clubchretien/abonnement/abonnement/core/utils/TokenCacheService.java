package com.clubchretien.abonnement.abonnement.core.utils;

import com.clubchretien.abonnement.abonnement.dto.TokenData;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class TokenCacheService {
    private final Cache<String, TokenData> tokenCache;

    public TokenCacheService() {
        this.tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    public void saveToken(String userId, String accessToken, String refreshToken) {
        tokenCache.put(userId, new TokenData(accessToken, refreshToken));
    }

    public TokenData getToken(String userId) {
        return tokenCache.getIfPresent(userId);
    }
}