package com.clubchretien.abonnement.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatAbonnementResponse {
    private Long commandeId;
    private Long userId;
    private BigDecimal montant;
    private String formuleLibelle;
    private String message;
    private boolean success;

    }