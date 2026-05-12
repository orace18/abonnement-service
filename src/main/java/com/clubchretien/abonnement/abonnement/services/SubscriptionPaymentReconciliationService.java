package com.clubchretien.abonnement.abonnement.services;

import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.ResponseStatusUtils;
import com.owomi.commons.ws.CallbackCreditRequestWs;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.TransactionsCcapp;
import com.owomi.database.entities.enums.StatutPaiement;
import com.owomi.database.entities.enums.TypeOperation;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SubscriptionPaymentReconciliationService {

    @Autowired
    private TransactionCcappRepository transactionCcappRepository;

    @Autowired
    private AbonnementRepository abonnementRepository;

    @Autowired
    private AbonnementService abonnementService;

    @Autowired
    private ResponseStatusUtils responseStatusUtils;

    public Map<String, Object> reconcilierRenouvellementsPayesNonFinalises(String jawupayAccess,
            String partnerToken, int limit) {
        int effectiveLimit = limit > 0 ? limit : 200;
        List<TransactionsCcapp> transactions = transactionCcappRepository
                .findByTypeOrderByDateRequeteDesc(TypeOperation.RENOUVELLEMENT.name());

        int scannes = 0;
        int reconcilies = 0;
        int ignores = 0;
        int aTraiterManuellement = 0;
        int erreurs = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (TransactionsCcapp transaction : transactions) {
            if (scannes >= effectiveLimit) {
                break;
            }
            scannes++;

            Map<String, Object> detail = nouveauDetail(transaction);
            try {
                String raisonIrreconciliable = raisonIrreconciliable(transaction);
                if (raisonIrreconciliable != null) {
                    aTraiterManuellement++;
                    detail.put("statut", "manuel");
                    detail.put("raison", raisonIrreconciliable);
                    details.add(detail);
                    log.warn("Reconciliation abonnement impossible: {}", detail);
                    continue;
                }

                Optional<Abonnement> abonnementOpt = abonnementRepository.findById(transaction.getAbonnementId());
                if (abonnementOpt.isEmpty()) {
                    aTraiterManuellement++;
                    detail.put("statut", "manuel");
                    detail.put("raison", "abonnement_introuvable");
                    details.add(detail);
                    log.warn("Reconciliation abonnement impossible: {}", detail);
                    continue;
                }

                Abonnement abonnement = abonnementOpt.get();
                if (estDejaFinalise(abonnement)) {
                    ignores++;
                    detail.put("statut", "ignore");
                    detail.put("raison", "abonnement_deja_finalise");
                    details.add(detail);
                    continue;
                }

                if (!paiementConfirme(transaction, jawupayAccess, partnerToken)) {
                    ignores++;
                    detail.put("statut", "ignore");
                    detail.put("raison", "paiement_non_confirme");
                    details.add(detail);
                    continue;
                }

                Abonnement abonnementFinalise = abonnementService.finaliserPaiementAbonnement(
                        transaction.getJawupayId(), true);
                reconcilies++;
                detail.put("statut", "reconcilie");
                detail.put("abonnementStatut", abonnementFinalise.getStatutPaiement());
                detail.put("abonnementActif", abonnementFinalise.getActif());
                details.add(detail);
                log.info("Reconciliation renouvellement OK: {}", detail);
            } catch (Exception e) {
                erreurs++;
                detail.put("statut", "erreur");
                detail.put("raison", e.getMessage());
                details.add(detail);
                log.warn("Reconciliation renouvellement echouee sans arret batch: {}", detail, e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scannes", scannes);
        result.put("reconcilies", reconcilies);
        result.put("ignores", ignores);
        result.put("aTraiterManuellement", aTraiterManuellement);
        result.put("erreurs", erreurs);
        result.put("details", details);
        return result;
    }

    private Map<String, Object> nouveauDetail(TransactionsCcapp transaction) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("transactionId", transaction != null ? transaction.getId() : null);
        detail.put("paymentId", transaction != null ? transaction.getJawupayId() : null);
        detail.put("abonnementId", transaction != null ? transaction.getAbonnementId() : null);
        detail.put("transactionStatut", transaction != null ? transaction.getStatut() : null);
        return detail;
    }

    private String raisonIrreconciliable(TransactionsCcapp transaction) {
        if (transaction == null) {
            return "transaction_nulle";
        }
        if (transaction.getJawupayId() == null) {
            return "paymentId_absent";
        }
        if (transaction.getAbonnementId() == null) {
            return "abonnement_lie_absent";
        }
        return null;
    }

    private boolean estDejaFinalise(Abonnement abonnement) {
        return StatutPaiement.PAYE.equals(abonnement.getStatutPaiement())
                && Boolean.TRUE.equals(abonnement.getActif());
    }

    private boolean paiementConfirme(TransactionsCcapp transaction, String jawupayAccess, String partnerToken)
            throws OwomiException {
        String statutSucces = responseStatusUtils.adapterStatus(WsOperationResponseStatus.success);
        if (statutSucces.equals(transaction.getStatut())) {
            return true;
        }
        if (!hasText(jawupayAccess)) {
            return false;
        }

        CallbackCreditRequestWs ws = new CallbackCreditRequestWs();
        ws.setIdPaiement(transaction.getJawupayId());
        ws.setSecretClient(jawupayAccess);
        ws.setPartnerClient(partnerToken);
        CommonResponseWs response = abonnementService.check(ws);
        return response != null && statutSucces.equals(response.getData());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
