package com.clubchretien.abonnement.abonnement.controllers.v2;

import com.clubchretien.abonnement.abonnement.dto.PaymentCallbackRequest;
import com.clubchretien.abonnement.abonnement.services.v2.PaiementV2Service;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.v2.PartnerPaymentInitializerWs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController("Paiement")
@RequestMapping("/v2/api/paiement")
@Tag(name = "Initialisation de paiement", description = "La vesrion v2 de l'initialisation d'un paiement")
public class PaiementV2Controller {

    @Autowired
    private PaiementV2Service paiementV2Service;

    @Operation(summary = "Initialisation d'un paiement")
    @PostMapping("/init-paiement")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Événement d'initialisation de paiement", required = true, content = @Content(schema = @Schema(implementation = PartnerPaymentInitializerWs.class), examples = @ExampleObject(name = "Exemple de retour", value = """
            {
                "status": "success",
                "data": {
                    "paymentId": 123.4,
                    "transactionToken": "xxxxxxxxxxxx"
                },
                "info": null,
                "message": "Payment initialized",
                "id": null,
                "reference": null,
                "code": 200
            }
                        """)))
    public CommonResponseWs initPaiement(@RequestBody PartnerPaymentInitializerWs ws){
       return this.paiementV2Service.initPaiement(ws);
    }

    @Operation(summary = "Callback du paiement")
    @PostMapping("/callbcak")
    public CommonResponseWs callback(@RequestParam Long idPaiement) throws OwomiException {
        return this.paiementV2Service.callback(idPaiement);
    }
}
