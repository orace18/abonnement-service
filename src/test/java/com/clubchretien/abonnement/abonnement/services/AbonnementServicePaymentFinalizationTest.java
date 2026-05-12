package com.clubchretien.abonnement.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.FormuleAbonnement;
import com.owomi.database.entities.TransactionsCcapp;
import com.owomi.database.entities.enums.Duree;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeAbonnement;
import com.owomi.database.entities.enums.TypeOperation;
import com.owomi.database.repositories.ArchiveParentRepository;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.rest.client.abonnementClient.AuthenticationRestClient;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.auth.UserDataClient;
import com.owomi.rest.client.notif.NotificationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementServicePaymentFinalizationTest {

    private static final Long PAYMENT_ID = 98765L;
    private static final Long ABONNEMENT_ID = 42L;

    @Mock
    private UserDataClient userDataClient;
    @Mock
    private AuthenticationRestClient authRestClient;
    @Mock
    private AbonnementRepository abonnementRepository;
    @Mock
    private TransactionCcappRepository transactionCcappRepository;
    @Mock
    private ResponseStatusUtils responseStatusUtils;
    @Mock
    private GestionNiveauxService gestionNiveauxService;
    @Mock
    private ArchiveParentRepository archiveParentRepository;
    @Mock
    private ReglementDeCompteService reglementDeCompteService;
    @Mock
    private UserInfosClient userInfosClient;
    @Mock
    private NotificationUtils notificationUtils;

    private AbonnementService abonnementService;

    @BeforeEach
    void setUp() {
        abonnementService = new AbonnementService(userDataClient, authRestClient);
        ReflectionTestUtils.setField(abonnementService, "abonnementRepository", abonnementRepository);
        ReflectionTestUtils.setField(abonnementService, "transactionCcappRepository", transactionCcappRepository);
        ReflectionTestUtils.setField(abonnementService, "responseStatusUtils", responseStatusUtils);
        ReflectionTestUtils.setField(abonnementService, "gestionNiveauxService", gestionNiveauxService);
        ReflectionTestUtils.setField(abonnementService, "archiveParentRepository", archiveParentRepository);
        ReflectionTestUtils.setField(abonnementService, "reglementDeCompteService", reglementDeCompteService);
        ReflectionTestUtils.setField(abonnementService, "userInfosClient", userInfosClient);
        ReflectionTestUtils.setField(abonnementService, "notificationUtils", notificationUtils);
    }

    @Test
    void callbackSansJwtFinaliseAbonnementParPaymentId() throws Exception {
        TransactionsCcapp transaction = transaction("PENDING");
        Abonnement abonnement = abonnement(StatutPaiement.EN_ATTENTE, false);

        when(transactionCcappRepository.findOneByJawupayId(PAYMENT_ID)).thenReturn(transaction);
        when(abonnementRepository.findByIdForUpdate(ABONNEMENT_ID)).thenReturn(Optional.of(abonnement));
        when(gestionNiveauxService.appliquerGestionNiveau(abonnement)).thenReturn(true);
        when(abonnementRepository.findFirstByUserIdAndActifTrueAndStatutPaiementAndDateFinAfterOrderByDateFinDesc(
                any(), any(), any())).thenReturn(Optional.empty());
        when(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)).thenReturn("SUCCESS");
        when(abonnementRepository.save(any(Abonnement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionCcappRepository.save(any(TransactionsCcapp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Abonnement result = abonnementService.traiterCallbackPaiement(PAYMENT_ID, true);

        assertEquals(StatutPaiement.PAYE, result.getStatutPaiement());
        assertTrue(result.getActif());
        assertEquals(PAYMENT_ID, result.getPaymentId());
        assertEquals("SUCCESS", transaction.getStatut());
        verify(gestionNiveauxService).appliquerGestionNiveau(abonnement);
    }

    @Test
    void doubleCallbackNeRejouePasLeRenouvellement() throws Exception {
        TransactionsCcapp transaction = transaction("SUCCESS");
        Abonnement abonnement = abonnement(StatutPaiement.PAYE, true);
        LocalDateTime dateDebut = abonnement.getDateDebut();
        LocalDateTime dateFin = abonnement.getDateFin();

        when(transactionCcappRepository.findOneByJawupayId(PAYMENT_ID)).thenReturn(transaction);
        when(abonnementRepository.findByIdForUpdate(ABONNEMENT_ID)).thenReturn(Optional.of(abonnement));
        when(responseStatusUtils.adapterStatus(WsOperationResponseStatus.success)).thenReturn("SUCCESS");

        Abonnement result = abonnementService.traiterCallbackPaiement(PAYMENT_ID, true);

        assertSame(abonnement, result);
        assertEquals(dateDebut, result.getDateDebut());
        assertEquals(dateFin, result.getDateFin());
        verify(gestionNiveauxService, never()).appliquerGestionNiveau(any());
        verify(abonnementRepository, never()).save(any(Abonnement.class));
    }

    @Test
    void paymentIdInconnuRetourneErreurControlee() {
        when(transactionCcappRepository.findOneByJawupayId(PAYMENT_ID)).thenReturn(null);

        OwomiException exception = assertThrows(OwomiException.class,
                () -> abonnementService.traiterCallbackPaiement(PAYMENT_ID, true));

        assertTrue(exception.getMessage().contains("Transaction introuvable"));
    }

    private TransactionsCcapp transaction(String statut) {
        TransactionsCcapp transaction = new TransactionsCcapp();
        transaction.setId(7L);
        transaction.setJawupayId(PAYMENT_ID);
        transaction.setAbonnementId(ABONNEMENT_ID);
        transaction.setType(TypeOperation.RENOUVELLEMENT.name());
        transaction.setStatut(statut);
        transaction.setDateRequete(new Date());
        return transaction;
    }

    private Abonnement abonnement(StatutPaiement statutPaiement, boolean actif) {
        FormuleAbonnement formule = new FormuleAbonnement();
        formule.setTypeAbonnement(TypeAbonnement.CLASSIC);
        formule.setDuree(Duree.UN_MOIS);
        formule.setPrix(BigDecimal.valueOf(1000));

        LocalDateTime now = LocalDateTime.now();
        Abonnement abonnement = new Abonnement();
        abonnement.setId(ABONNEMENT_ID);
        abonnement.setUserId(100L);
        abonnement.setFormuleAbonnement(formule);
        abonnement.setPrixAbonnement(BigDecimal.valueOf(1000));
        abonnement.setDateAchat(now);
        abonnement.setDateDebut(now);
        abonnement.setDateFin(now.plusMonths(1));
        abonnement.setStatutPaiement(statutPaiement);
        abonnement.setActif(actif);
        return abonnement;
    }
}
