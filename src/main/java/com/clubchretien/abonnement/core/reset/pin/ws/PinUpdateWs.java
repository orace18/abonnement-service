package com.clubchretien.abonnement.core.reset.pin.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinUpdateWs {
    private String pin;
    private String tokenPinReset;
}
