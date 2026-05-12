package com.clubchretien.abonnement.abonnement.controllers;

import com.clubchretien.abonnement.abonnement.dto.PaymentCallbackRequest;
import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.SubscriptionPaymentReconciliationService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeAbonnement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementControllerPaymentCallbackTest {

    private static final Long PAYMENT_ID = 1234L;

    @Mock
    private AbonnementService abonnementService;
    @Mock
    private SubscriptionPaymentReconciliationService subscriptionPaymentReconciliationService;
    @Mock
    private ResponseStatusUtils responseStatusUtils;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AbonnementController controller;

    @BeforeEach
    void setUp() {
        controller = new AbonnementController();
        ReflectionTestUtils.setField(controller, "abonnementService", abonnementService);
        ReflectionTestUtils.setField(controller, "subscriptionPaymentReconciliationService",
                subscriptionPaymentReconciliationService);
        ReflectionTestUtils.setField(controller, "responseStatusUtils", responseStatusUtils);
        ReflectionTestUtils.setField(controller, "httpServletRequest", httpServletRequest);
    }

    @Test
    void callbackRetourneErreurControleeQuandPaymentIdInconnu() throws Exception {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPaymentId(PAYMENT_ID);
        request.setStatutPaiement("SUCCESS");

        when(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)).thenReturn("SUCCESS");
        when(abonnementService.traiterCallbackPaiement(PAYMENT_ID, true))
                .thenThrow(new OwomiException("Transaction introuvable pour paymentId: " + PAYMENT_ID));

        CommonResponseWs response = controller.callback(request);

        assertEquals(WsOperationResponseStatus.failed, response.getStatus());
        assertEquals(400, response.getCode());
    }

    @Test
    void checkUtiliseLaMemeFinalisationQueLeCallback() throws Exception {
        Abonnement abonnement = abonnementPaye();
        when(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)).thenReturn("SUCCESS");
        when(abonnementService.check(org.mockito.ArgumentMatchers.any())).thenReturn(
                new CommonResponseWs(WsOperationResponseStatus.success, "ok", 200, "SUCCESS"));
        when(abonnementService.traiterCallbackPaiement(PAYMENT_ID, true)).thenReturn(abonnement);

        CommonResponseWs response = controller.check(null, PAYMENT_ID, "secret");

        assertEquals(WsOperationResponseStatus.success, response.getStatus());
        verify(abonnementService).traiterCallbackPaiement(eq(PAYMENT_ID), eq(true));
    }

    private Abonnement abonnementPaye() {
        FormuleAbonnement formule = new FormuleAbonnement();
        formule.setTypeAbonnement(TypeAbonnement.CLASSIC);
        formule.setDuree(Duree.UN_MOIS);
        formule.setPrix(BigDecimal.valueOf(1000));

        LocalDateTime now = LocalDateTime.now();
        Abonnement abonnement = new Abonnement();
        abonnement.setId(99L);
        abonnement.setUserId(100L);
        abonnement.setFormuleAbonnement(formule);
        abonnement.setPrixAbonnement(BigDecimal.valueOf(1000));
        abonnement.setDateAchat(now);
        abonnement.setDateDebut(now);
        abonnement.setDateFin(now.plusMonths(1));
        abonnement.setStatutPaiement(StatutPaiement.PAYE);
        abonnement.setActif(true);
        return abonnement;
    }
}
