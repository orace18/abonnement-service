package com.clubchretien.abonnement.services;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CreateRecipientWs;
import com.owomi.commons.ws.RecipientWs;
import com.owomi.database.entities.Banque;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.Recipient;
import com.owomi.database.repositories.BanqueRepository;
import com.owomi.database.repositories.PaysRepository;
import com.owomi.database.repositories.RecipientRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecipientService {

    private static Logger logger = LogManager.getLogger(RecipientService.class);

    @Autowired
    RecipientRepository recipientRepository;
    @Autowired
    BanqueRepository banqueRepository;
    @Autowired
    PaysRepository paysRepository;


    public CreateRecipientWs createRecipent ( CreateRecipientWs createRecipientWs, Partenaire partenaire) throws OwomiException, DataNotFoundException {
        Recipient recipient = new Recipient();
        if (createRecipientWs.getBanque_id() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de banque_id");
        Banque banque = banqueRepository.findOneById(createRecipientWs.getBanque_id());
        EntityUtils.checkIfEntityIsNull(Banque.class, banque, createRecipientWs.getBanque_id());
		//if (createRecipientWs.getPays() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de pays");
		//Pays pays = paysRepository.findOneById(createRecipientWs.getPays());
		//EntityUtils.checkIfEntityIsNull(Pays.class, pays, createRecipientWs.getPays());
        recipient.setPartenaire(partenaire);
        recipient.setBanque(banque);
        recipient.setPays(banque.getPays());
        if (createRecipientWs.getTel() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de tel");
        if (!checkphoneNumber(createRecipientWs.getTel(), banque.getPays().getCode() )) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Numero invalide");
        switch (banque.getNom()) {

            case MobileConstant.BANQUE_MOOV_BENIN:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_BOA_BENIN:
                recipient = banque_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MTN_BENIN:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MOOV_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MTN_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_ORANGE_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
        }
        recipient = recipientRepository.save(recipient);
        createRecipientWs.setId(recipient.getId());
        return createRecipientWs;
    }


    public CreateRecipientWs updateRecipent (CreateRecipientWs createRecipientWs, Partenaire partenaire) throws OwomiException, DataNotFoundException {
        if (createRecipientWs.getId() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de id");
		Recipient recipient = recipientRepository.findOneById(createRecipientWs.getId());
        EntityUtils.checkIfEntityIsNull(Recipient.class, recipient, createRecipientWs.getId());
        if (!recipient.getPartenaire().getTelephone().equals(partenaire.getTelephone())) throw new OwomiException("Partenaire error", ErrorCodeEnums.RECIPIENT_ERROR, "Ce recipient ne vous appartient pas");
        if (createRecipientWs.getBanque_id() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de banque_id");
		Banque banque = banqueRepository.findOneById(createRecipientWs.getBanque_id());
        EntityUtils.checkIfEntityIsNull(Banque.class, banque, createRecipientWs.getBanque_id());
		//if (createRecipientWs.getPays() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de pays");
		//Pays pays = paysRepository.findOneById(createRecipientWs.getPays());
		//EntityUtils.checkIfEntityIsNull(Pays.class, pays, createRecipientWs.getPays());
        recipient.setPartenaire(partenaire);
        recipient.setBanque(banque);
        recipient.setPays(banque.getPays());
        if (createRecipientWs.getTel() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de tel");
        //if (!checkphoneNumber(createRecipientWs.getTel(), banque.getPays().getCode() )) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Numero invalide");
        switch (banque.getNom()) {

            case MobileConstant.BANQUE_MOOV_BENIN:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_BOA_BENIN:
                recipient = banque_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MTN_BENIN:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MOOV_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_MTN_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
            case MobileConstant.BANQUE_ORANGE_CIV:
                recipient = mobile_banque_methode(recipient, createRecipientWs);
                break;
        }
        recipientRepository.save(recipient);
        return createRecipientWs;
    }

    public List<RecipientWs> getRecipientList (Partenaire partenaire) {
        List<Recipient> recipientList = recipientRepository.findAllByPartenaire(partenaire);
        return recipientList.stream().map(recipient -> new RecipientWs(recipient.getId(), recipient.getBanque().getNom(), recipient.getIban(),
                recipient.getNumero_du_compte(), recipient.getIntitule_du_compte(), recipient.getAdresse(),
                recipient.getVille(), recipient.getPays().getNompays(), recipient.getTel(),
                recipient.getSwift(), recipient.getNom_banque(), recipient.getNom_utlisateur())).collect(Collectors.toList());
    }

    public RecipientWs getRecipient (Partenaire partenaire, Integer id) {
            Recipient recipient = recipientRepository.findByPartenaireAndId(partenaire, id);
            return new RecipientWs(recipient.getId(), recipient.getBanque().getNom(), recipient.getIban(),
                    recipient.getNumero_du_compte(), recipient.getIntitule_du_compte(), recipient.getAdresse(),
                    recipient.getVille(), recipient.getPays().getNompays(), recipient.getTel(),
                    recipient.getSwift(), recipient.getNom_banque(), recipient.getNom_utlisateur());
    }
    public CommonResponseWs deleteRecipient (Partenaire partenaire, Integer id) throws DataNotFoundException, OwomiException {
        Recipient recipient = recipientRepository.findByPartenaireAndId(partenaire, id);
        EntityUtils.checkIfEntityIsNull(Recipient.class, recipient, id);
        //if (!recipient.getPartenaire().getTelephone().equals(partenaire.getTelephone())) throw new OwomiException("Partenaire error", ErrorCodeEnums.RECIPIENT_ERROR, "Ce recipient ne vous appartient pas");
        recipientRepository.delete(recipient);
        return new CommonResponseWs(WsOperationResponseStatus.success,null,null,null,null,null,null);
    }

    private Recipient mobile_banque_methode (Recipient recipient1, CreateRecipientWs createRecipientWs) throws OwomiException {
        Recipient recipient2 = recipient1;
        recipient2.setTel(createRecipientWs.getTel());
        if (createRecipientWs.getNom_utlisateur() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de nom_utlisateur");
        recipient2.setNom_utlisateur(createRecipientWs.getNom_utlisateur());
        return recipient2;
    }

    private Recipient banque_banque_methode (Recipient recipient4, CreateRecipientWs createRecipientWs) throws OwomiException {
        Recipient recipient3 = recipient4;
        if (createRecipientWs.getIban() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de iban");
        recipient3.setIban(createRecipientWs.getIban());
        if (createRecipientWs.getNumero_du_compte() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de numero_du_compte");
        recipient3.setNumero_du_compte(createRecipientWs.getNumero_du_compte());
        if (createRecipientWs.getIntitule_du_compte() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de intitule_du_compte");
        recipient3.setIntitule_du_compte(createRecipientWs.getIntitule_du_compte());
        if (createRecipientWs.getAdresse() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de adresse");
        recipient3.setAdresse(createRecipientWs.getAdresse());
        if (createRecipientWs.getVille() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de ville");
        recipient3.setVille(createRecipientWs.getVille());
        if (createRecipientWs.getTel() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de tel");
        recipient3.setTel(createRecipientWs.getTel());
        if (createRecipientWs.getSwift() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de swift");
        recipient3.setSwift(createRecipientWs.getSwift());
        /*if (createRecipientWs.getNom_banque() == null) throw new OwomiException("Attribut manquant", ErrorCodeEnums.RECIPIENT_ERROR, "Absence de nom_banque");
        recipient3.setNom_banque(createRecipientWs.getNom_banque());*/
        return recipient3;
    }

    private Boolean checkphoneNumber (String number , String pays) {
        logger.debug("Code: " + pays);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
        try {
            phoneNumber = phoneUtil.parse(number, pays);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }
        return phoneUtil.isValidNumberForRegion(phoneNumber, pays);
    }

}
