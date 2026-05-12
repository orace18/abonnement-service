package com.clubchretien.abonnement.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkInfos {
    private double argentValue;
    private double orValue;
    private double platiniumValue;
    private double diamantValue;
    private double diamantEliteValue;
}
