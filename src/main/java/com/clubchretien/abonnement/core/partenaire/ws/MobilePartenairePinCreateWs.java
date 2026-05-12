package com.clubchretien.abonnement.core.partenaire.ws;

import com.owomi.commons.ws.encrypt.EncryptTerminalEncoderWs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MobilePartenairePinCreateWs {
    private Integer partenaireId;
    private EncryptTerminalEncoderWs pin;
}
