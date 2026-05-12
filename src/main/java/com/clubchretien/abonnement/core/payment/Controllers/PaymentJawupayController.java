package com.clubchretien.abonnement.core.payment.Controllers;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.core.partenaire.services.MobilePartenaireAuthenticationService;
import com.clubchretien.abonnement.core.payment.Services.PaymentJawupayService;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DemandePaiementWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/abonnement/v1.0/payment")
@SecurityRequirements({@SecurityRequirement(name = ACCESS_TOKEN)})

public class PaymentJawupayController {

    private static Logger logger = LogManager.getLogger(PaymentJawupayController.class);

    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    PaymentJawupayService service;
    @Autowired
    SecurityUtils securityUtils;

    /*@GetMapping("/init")
    public CommonResponseWs initPayment(@RequestBody InitPaymentWs ws) throws Exception {
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        Partenaire partenaire = this.securityUtils.getPartenaireFromHeader(httpServletRequest);
        return this.service.initialisation(partenaire, ws);
    }*/

}
