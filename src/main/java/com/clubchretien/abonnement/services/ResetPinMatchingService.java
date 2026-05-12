package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.core.corporate.services.MobileCorporateService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.acm.AcmSoldeWs;
import com.owomi.commons.ws.authentication.MatchingAuthWs;
import com.owomi.commons.ws.authentication.MatchingFormPaymentWs;
import com.owomi.commons.ws.authentication.MatchingFormWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PartenairePinHistory;
import com.owomi.database.entities.Payment;
import com.owomi.database.repositories.PartenairePinHistoryRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.PaymentRepository;
import com.owomi.rest.client.mobileService.MobileMatchingClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ResetPinMatchingService {

    private static Logger logger = LogManager.getLogger(ResetPinMatchingService.class);

    @Autowired
    MobileCorporateService mobileCorporateService;

    @Autowired
    AuthentificationService authentificationService;

    @Autowired
    MobileMatchingClient mobileMatchingClient;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    PartenaireRepository partenaireRepository;

    @Autowired
    PartenairePinHistoryRepository partenairePinHistoryRepository;



    public CommonResponseWs matchToResetPin(MatchingFormWs formWs) {

        CommonResponseWs commonResponseWs = new CommonResponseWs();
        Partenaire partenaire = authentificationService.getPartenaire();

        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);

        boolean validDataForm = Stream.of(formWs).anyMatch(Objects::isNull);
        logger.debug("LES CHAMPS SONT-ILS TOUS NON NULS ? === {}", !validDataForm);
        if (validDataForm) return new CommonResponseWs(WsOperationResponseStatus.failed, "INVALID DATA", 401, null);

        boolean isMatch = isMatch(partenaire, formWs);
        if (isMatch) {
            commonResponseWs.setStatus(WsOperationResponseStatus.success);
            commonResponseWs.setData(true);
            commonResponseWs.setMessage("Matching success");
            commonResponseWs.setCode(200);
        } else {
            commonResponseWs.setStatus(WsOperationResponseStatus.failed);
            commonResponseWs.setData(false);
            commonResponseWs.setMessage("Matching failed");
            commonResponseWs.setCode(400);
        }
        return commonResponseWs;
    }

    public CommonResponseWs getResetCode(String phoneUser) throws OwomiException {
        if (phoneUser == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);
        return this.mobileMatchingClient.createResetCode(phoneUser);
    }

    public CommonResponseWs validateResetCode(MatchingAuthWs formWs) throws OwomiException {
        if (formWs == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO DATA FOUND", 401, null);
        return this.mobileMatchingClient.validateResetCode(formWs);
    }

    public CommonResponseWs updatePinHistory(Partenaire partenaire, String pin) {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);
        if (partenaire.getPin() == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PIN FOUND", 401, null);
        //addPinHistory(partenaire, pin);
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
    }

    private void addPinHistory(Partenaire partenaire, String pin) {
        pinHistory(partenaire);
        PartenairePinHistory pinHistory = new PartenairePinHistory();
        pinHistory.setPartenaire(partenaire);
        pinHistory.setPin(pin);
        this.partenairePinHistoryRepository.save(pinHistory);
    }

    private void pinHistory(Partenaire partenaire) {
        PartenairePinHistory pinHistory = new PartenairePinHistory();
        String pin = partenaire.getPin();
        long counter = this.partenairePinHistoryRepository.findByPartenaireId(partenaire.getId()).size();

        if (counter == 0) {
            pinHistory.setPartenaire(partenaire);
            pinHistory.setPin(pin);
           // partenaire.setHaveHistoryPin(false);
            this.partenaireRepository.save(partenaire);
            this.partenairePinHistoryRepository.save(pinHistory);
        }
        // Une historyPin commence a partir de 2
        if (counter >= 2) {
            //partenaire.setHaveHistoryPin(true);
            this.partenaireRepository.save(partenaire);
        }
    }

    // TODO : Prendre en compte les reponses booleennes
    private boolean isMatch(Partenaire partenaire, MatchingFormWs formWs) {

        int weight = 0;
        Integer telephoneMatch = 0, nomMatch = 0, prenomMatch = 0, dateNaissanceMatch = 0, soldeMatch = 0, montantMatch, typeMatch;

        if (partenaire.getTelephone().equals(formWs.getNumeroPhone())) {
            telephoneMatch = 1;
            weight += 5;
        }
        if (partenaire.getNom().equals(formWs.getNom())) {
            nomMatch = 1;
            weight += 5;
        }
        if (partenaire.getPrenoms().equals(formWs.getPrenom())) {
            prenomMatch = 1;
            weight += 5;
        }
        // TODO : Fix compatibility with date format (yyyy-MM-dd) of partenaire getDatenaissance return and string of formWs getDateNaissance
        if (partenaire.getDatenaissance().toString().equals(formWs.getDateNaissance())) {
            dateNaissanceMatch = 1;
            weight += 5;
        }
        if (partenaire.getPrenoms().contentEquals(formWs.getPrenom())) {
            prenomMatch = 1;
            //weight += 7;
        }

        AcmSoldeWs acmSoldeWs = this.mobileCorporateService.acmSolde(partenaire.getPartenaireCorporates().get(0).getCorporate());
        double solde = acmSoldeWs.getTotalSolde();
        double soldeWs = formWs.getSolde();
        if ((soldeWs >= solde - 3000) && (soldeWs <= solde + 3000)) {
            soldeMatch = 1;
            weight += 40;
        }

        // TODO : Check synchronisation of matching item list
        List<Double> lastTransactionMontant = formWs.getLastTransactions().stream().map(MatchingFormPaymentWs::getAmount).collect(Collectors.toList());
        Stream<Double> montantList = this.getLastPaiement(partenaire).stream().map(Payment::getMontant);
        montantMatch =(montantList.anyMatch(lastTransactionMontant::contains)) ? 1 : 0;
        if (montantMatch == 1) weight += 15;
        logger.debug(montantMatch.toString().toUpperCase());

        List<String> lastTransactionType = this.getLastPaiement(partenaire).stream().map(Payment::getType).map(String::toUpperCase).collect(Collectors.toList());
        Stream<String> typeList = this.getLastPaiement(partenaire).stream().map(Payment::getType);
        typeMatch = (typeList.anyMatch(lastTransactionType::contains)) ? 1 : 0;
        if (typeMatch == 1) weight += 15;
        logger.debug(typeMatch.toString().toUpperCase());


        boolean isMatch = telephoneMatch == 1 && nomMatch == 1 && prenomMatch == 1 && dateNaissanceMatch == 1 && soldeMatch == 1 && montantMatch == 1 && typeMatch == 1;
        if (weight >= 60) {
            isMatch = true;
            logger.debug("Matching success");
        }
        else logger.debug("Matching failed");
        return isMatch;
    }

    private List<Payment> getLastPaiement(Partenaire partenaire) {
        Pageable pageable = Pageable.ofSize(10);
        List<Payment> paymentList = new ArrayList<>();
        Page<Payment> paymentPage = paymentRepository.findByTypeAndPartenaire(partenaire.getType(), partenaire, pageable);
        int paymentSize = paymentPage.getSize();
        paymentList.add(paymentPage.getContent().get(paymentSize - 1));
        paymentList.add(paymentPage.getContent().get(paymentSize - 2));
        //paymentList.add(paymentPage.getContent().get(paymentSize - 3));
        return paymentList;
    }
}
