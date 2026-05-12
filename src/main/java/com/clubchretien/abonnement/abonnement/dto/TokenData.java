package com.clubchretien.abonnement.abonnement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenData {
    private String accessToken;
    private String refreshToken;
    private long expirationTime;

    public TokenData(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = System.currentTimeMillis();
    }
}