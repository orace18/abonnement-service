package com.clubchretien.abonnement.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.TransactionsCcapp;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeOperation;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentReconciliationServiceTest {

    private static final Long PAYMENT_ID = 8765L;
    private static final Long ABONNEMENT_ID = 33L;

    @Mock
    private TransactionCcappRepository transactionCcappRepository;
    @Mock
    private AbonnementRepository abonnementRepository;
    @Mock
    private AbonnementService abonnementService;
    @Mock
    private ResponseStatusUtils responseStatusUtils;

    private SubscriptionPaymentReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new SubscriptionPaymentReconciliationService();
        ReflectionTestUtils.setField(reconciliationService, "transactionCcappRepository", transactionCcappRepository);
        ReflectionTestUtils.setField(reconciliationService, "abonnementRepository", abonnementRepository);
        ReflectionTestUtils.setField(reconciliationService, "abonnementService", abonnementService);
        ReflectionTestUtils.setField(reconciliationService, "responseStatusUtils", responseStatusUtils);
    }

    @Test
    void transactionSuccesEtAbonnementNonFinaliseSontReconcilies() throws Exception {
        TransactionsCcapp transaction = transaction(ABONNEMENT_ID, "SUCCESS");
        Abonnement abonnement = abonnement(StatutPaiement.EN_ATTENTE, false);
        Abonnement abonnementFinalise = abonnement(StatutPaiement.PAYE, true);

        when(transactionCcappRepository.findByTypeOrderByDateRequeteDesc(TypeOperation.RENOUVELLEMENT.name()))
                .thenReturn(List.of(transaction));
        when(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)).thenReturn("SUCCESS");
        when(abonnementRepository.findById(ABONNEMENT_ID)).thenReturn(Optional.of(abonnement));
        when(abonnementService.finaliserPaiementAbonnement(PAYMENT_ID, true)).thenReturn(abonnementFinalise);

        Map<String, Object> result = reconciliationService
                .reconcilierRenouvellementsPayesNonFinalises(null, null, 200);

        assertEquals(1, result.get("reconcilies"));
        assertEquals(0, result.get("erreurs"));
        verify(abonnementService).finaliserPaiementAbonnement(PAYMENT_ID, true);
    }

    @Test
    void transactionSuccesSansAbonnementLieNeCassePasLeBatch() throws Exception {
        TransactionsCcapp transaction = transaction(null, "SUCCESS");

        when(transactionCcappRepository.findByTypeOrderByDateRequeteDesc(TypeOperation.RENOUVELLEMENT.name()))
                .thenReturn(List.of(transaction));

        Map<String, Object> result = reconciliationService
                .reconcilierRenouvellementsPayesNonFinalises(null, null, 200);

        assertEquals(1, result.get("aTraiterManuellement"));
        assertEquals(0, result.get("erreurs"));
        verify(abonnementService, never()).finaliserPaiementAbonnement(PAYMENT_ID, true);
    }

    private TransactionsCcapp transaction(Long abonnementId, String statut) {
        TransactionsCcapp transaction = new TransactionsCcapp();
        transaction.setId(1L);
        transaction.setJawupayId(PAYMENT_ID);
        transaction.setAbonnementId(abonnementId);
        transaction.setType(TypeOperation.RENOUVELLEMENT.name());
        transaction.setStatut(statut);
        return transaction;
    }

    private Abonnement abonnement(StatutPaiement statutPaiement, boolean actif) {
        Abonnement abonnement = new Abonnement();
        abonnement.setId(ABONNEMENT_ID);
        abonnement.setStatutPaiement(statutPaiement);
        abonnement.setActif(actif);
        return abonnement;
    }
}
