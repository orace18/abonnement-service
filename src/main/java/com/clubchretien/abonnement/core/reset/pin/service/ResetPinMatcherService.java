package com.clubchretien.abonnement.core.reset.pin.service;

import com.clubchretien.abonnement.core.corporate.services.MobileCorporateService;
import com.clubchretien.abonnement.core.reset.pin.ws.PinUpdateWs;
import com.clubchretien.abonnement.services.AuthentificationService;
import com.clubchretien.abonnement.services.NotificationService;
import com.google.gson.Gson;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.acm.AcmSoldeWs;
import com.owomi.commons.ws.authentication.MatchingAuthWs;
import com.owomi.commons.ws.authentication.MatchingFormPaymentWs;
import com.owomi.commons.ws.authentication.MatchingFormWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PartenairePinHistory;
import com.owomi.database.entities.Payment;
import com.owomi.database.entities.ResetPinToken;
import com.owomi.database.repositories.PartenairePinHistoryRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.PaymentRepository;
import com.owomi.database.repositories.ResetPinTokenRepository;
import com.owomi.rest.client.mobileService.MobileMatchingClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ResetPinMatcherService {
    private static final Logger logger = LogManager.getLogger(ResetPinMatcherService.class);

    @Autowired
    AuthentificationService authentificationService;
    @Autowired
    MobileMatchingClient mobileMatchingClient;
    @Autowired
    MobileCorporateService mobileCorporateService;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PartenairePinHistoryRepository partenairePinHistoryRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    ResetPinTokenRepository resetPinTokenRepository;
    @Autowired
    NotificationService notificationService;

    public CommonResponseWs matchToGetResetCode(MatchingFormWs formWs) throws OwomiException {
        Partenaire partenaire = authentificationService.getPartenaire();

        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);

        boolean validDataForm = Stream.of(formWs).anyMatch(Objects::isNull);
        logger.debug("LES CHAMPS SONT-ILS TOUS NON NULS ? === {}", !validDataForm);
        if (validDataForm) return new CommonResponseWs(WsOperationResponseStatus.failed, "INVALID DATA", 401, null);

        boolean isMatch = isMatch(partenaire, formWs);
        String fullPhoneUser = formWs.getNumeroPhone();
        if (isMatch && fullPhoneUser != null)
            return this.mobileMatchingClient.createResetCode(fullPhoneUser);

        return new CommonResponseWs(WsOperationResponseStatus.failed, "Matching failed", 400, false);
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = formatter.format(partenaire.getDatenaissance());
        if( formattedDate.equals(formWs.getDateNaissance())) {
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
        logger.debug("Montant matching boolean {}", montantMatch.toString().toUpperCase());

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
        logger.debug("Page of payment found {}", paymentPage);
        int paymentSize = paymentPage.getSize();
        paymentList.add(paymentPage.getContent().get(paymentSize - 1));
        paymentList.add(paymentPage.getContent().get(paymentSize - 2));
        //paymentList.add(paymentPage.getContent().get(paymentSize - 3));
        return paymentList;
    }

    public CommonResponseWs validateResetCode(MatchingAuthWs formWs) throws OwomiException {
        if (formWs == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO DATA FOUND", 401, null);
        // User authentication
        Partenaire partenaire = authentificationService.getPartenaire();
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);

        CommonResponseWs validateRs = this.mobileMatchingClient.validateResetCode(formWs);
        logger.debug("Result of Validation {}", validateRs);
        Gson gson = new Gson();
        Boolean isValid = gson.fromJson(gson.toJson(validateRs), Boolean.class);
        logger.debug(" is valid code ? {}", isValid);
        if (!isValid) return new CommonResponseWs(WsOperationResponseStatus.failed, "Reset code validation failed", 401, null);

        // generate token for pin reset
        ResetPinToken token = resetPinTokenRepository.findOneByPartenaireId(partenaire.getId());
        if(token == null) {
            token = new ResetPinToken();
            token.setPartenaireId(partenaire.getId());
        }
        token.setPinToken(StringUtils.randomUUID(true));
        token.setValidity(10* 60 * 1000); // 10min
        token = resetPinTokenRepository.save(token);
        return new CommonResponseWs(WsOperationResponseStatus.success, " Reset Pin Token ", 401, token);
    }


    public CommonResponseWs updatePinHistory(Partenaire partenaire, PinUpdateWs ws) throws NotificationException, OwomiException {
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PARTENAIRE FOUND", 401, null);
        if (partenaire.getPin() == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "NO PIN FOUND", 401, null);
        String oldPin = partenaire.getPin();
        //check reset token
        ResetPinToken tokenSaved = resetPinTokenRepository.findOneByPartenaireId(partenaire.getId());
        if (tokenSaved == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "No reset pin token found for Partenaire id="+ partenaire.getId(), 404, null);
        // temps écoulé
        long latence = Math.abs(new Date().getTime() - tokenSaved.getUpdatedAt().getTime());

        boolean isVerified = tokenSaved.getPinToken().equals(ws.getTokenPinReset()) && latence <= tokenSaved.getValidity();
        if(!isVerified) return new CommonResponseWs(WsOperationResponseStatus.failed, "Reset token not match or time passed", 401, null);

        // pin upsate - après 3h normallement
        MatchingAuthWs updatePinWs = new MatchingAuthWs();
        updatePinWs.setPhoneFullNumber(partenaire.getPhoneFullNumber());
        updatePinWs.setSecret(ws.getPin());
        CommonResponseWs updatePinRs = this.mobileMatchingClient.updateResetCode(updatePinWs);
        if(! updatePinRs.getStatus().equals(WsOperationResponseStatus.success)) return updatePinRs;

        //pin history
        addPinHistory(partenaire, oldPin);

        //user alert
        String title = "Changement de Code Pin";
        String message = "Alerte! Votre Code Pin vient d'etre modifier. Si cette modification n'était pas vous, veuillez demarrer le processus de changement de Code Pin";
        notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, null);
    }

    private void addPinHistory(Partenaire partenaire, String pin) {
        //pinHistory(partenaire);
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
}
