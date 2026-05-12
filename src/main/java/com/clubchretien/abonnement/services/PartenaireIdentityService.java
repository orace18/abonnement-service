package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.owomi.commons.constants.MobilePartenaireConstant;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.TypeDocumentEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.*;
import com.owomi.commons.ws.kyc.KycDemandeWs;
import com.owomi.commons.ws.kyc.PartenaireKycWs;
import com.owomi.database.entities.Image;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PartenaireKyc;
import com.owomi.database.entities.Validation;
import com.owomi.database.repositories.ImageRepository;
import com.owomi.database.repositories.PartenaireKycRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.ValidationRepository;
import com.owomi.rest.client.payerfactureService.PayerFactureEcoFamAbonneGroupeServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class PartenaireIdentityService implements MobilePartenaireConstant {
    private static Logger logger = LogManager.getLogger(PartenaireIdentityService.class);
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    ImageService imageService;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    ValidationService validationService;


    @Autowired
    PartenaireKycRepository partenaireKycRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    I18nFactory i18n;

    @Autowired
    Mapper mapper;

    @Autowired
    PayerFactureEcoFamAbonneGroupeServiceClient payerFactureEcoFamAbonneGroupeServiceClient;


    public PartenaireKyc findOne(Partenaire partenaire) {
        if (partenaire == null) return null;
        return this.partenaireKycRepository.findOneByPartenaire(partenaire);
    }

    public PartenaireKyc updateOneKyc(Partenaire partenaire) {
        PartenaireKyc entity = this.findOne(partenaire);
        if (entity == null) return null;
        Date birthday = Optional.ofNullable(partenaire.getDatenaissance()).orElse(entity.getBirthday());
        String cardPictureUrl = Optional.ofNullable(partenaire.getPiece()).map(Image::getUrl).orElse(entity.getCardPictureUrl());
        String cardFacePictureUrl = Optional.ofNullable(partenaire.getPiecePhoto()).map(Image::getUrl).orElse(entity.getCardFacePictureUrl());
        if (cardPictureUrl != null) entity.setCardPictureUrl(cardPictureUrl);
        if (cardFacePictureUrl != null) entity.setCardFacePictureUrl(cardFacePictureUrl);
        if (birthday != null) entity.setBirthday(birthday);
        entity = this.partenaireKycRepository.save(entity);
        return entity;
    }

    public Partenaire updateOnePartenaire(PartenaireKyc entity) {
        if (entity == null) return null;
        Partenaire partenaire = entity.getPartenaire();
        if (partenaire == null) return null;
        Date birthday = Optional.ofNullable(entity.getBirthday()).orElse(partenaire.getDatenaissance());
        partenaire.setDatenaissance(birthday);
        partenaire = this.partenaireRepository.save(partenaire);
        Image image = this.saveImage(partenaire.getPiece(), entity.getCardPictureUrl());
        partenaire.setPiece(image);
        image = this.saveImage(partenaire.getPiecePhoto(), entity.getCardFacePictureUrl());
        partenaire.setPiecePhoto(image);
        partenaire = this.partenaireRepository.save(partenaire);
        return partenaire;
    }

    private Date convertDateFromString(String str) {
        try {
            return StringUtils.fromISODate(str);
        } catch (OwomiException e) {
            e.printStackTrace();
        }
        return null;
    }


    public PartenaireKyc submit(Partenaire partenaire, KycDemandeWs ws) {
        PartenaireKyc entity = this.findOne(partenaire);
        if (entity == null) {
            entity = mapper.map(ws, PartenaireKyc.class);
            entity.setRecordDate(new Date());
            entity.setValidated(false);
        }
        entity.setPartenaire(partenaire);
        Date birthday = Optional.ofNullable(ws.getBirthdayStr()).map(this::convertDateFromString).orElse(partenaire.getDatenaissance());
        String cardType = Optional.ofNullable(ws.getCardType()).orElse(entity.getCardType());
        String cardId = Optional.ofNullable(ws.getCardId()).orElse(entity.getCardId());
        String cardPictureUrl = Optional.ofNullable(ws.getCardPictureUrl()).orElse(entity.getCardPictureUrl());
        String cardPictureBUrl = Optional.ofNullable(ws.getCardPictureBUrl()).orElse(entity.getCardPictureBUrl());
        String cardFacePictureUrl = Optional.ofNullable(ws.getCardFacePictureUrl()).orElse(entity.getCardFacePictureUrl());
        String cardIssuer = Optional.ofNullable(ws.getCardIssuer()).orElse(entity.getCardIssuer());
        Date cardExpirationDate = Optional.ofNullable(ws.getCardExpirationDateStr()).map(this::convertDateFromString).orElse(entity.getCardExpirationDate());
        String contactTelephone = Optional.ofNullable(ws.getContactTelephone()).orElse(entity.getContactTelephone());
        String additionalInformation = Optional.ofNullable(ws.getAdditionalInformation()).orElse(entity.getAdditionalInformation());
        if (birthday != null) entity.setBirthday(birthday);
        if (cardType != null) entity.setCardType(cardType);
        if (cardId != null) entity.setCardId(cardId);
        if (cardPictureUrl != null) entity.setCardPictureUrl(cardPictureUrl);
        if (cardPictureBUrl != null) entity.setCardPictureBUrl(cardPictureBUrl);
        if (cardFacePictureUrl != null) entity.setCardFacePictureUrl(cardFacePictureUrl);
        if (cardIssuer != null) entity.setCardIssuer(cardIssuer);
        if (cardExpirationDate != null) entity.setCardExpirationDate(cardExpirationDate);
        if (contactTelephone != null) entity.setContactTelephone(contactTelephone);
        if (additionalInformation != null) entity.setAdditionalInformation(additionalInformation);
        entity = this.partenaireKycRepository.save(entity);
        return entity;
    }


    public CommonResponseWs submission(Partenaire partenaire, KycDemandeWs ws) {
        logger.debug("submission ws =  {}", ws);
        if (partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        if(partenaire.isIdentifier()) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire deja identifier", 409, null);;
        PartenaireKyc entity = this.findOne(partenaire);
        if(entity != null && entity.getValidated() != null && entity.getValidated()) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire deja identifier", 409, null);
        entity = this.submit(partenaire, ws);
        this.updateOnePartenaire(entity);
        this.sendInValidationDemande(entity);
        PartenaireKycWs result = this.convertPartenaireKycToWs(entity);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }


    public CommonResponseWs authorizedPreValidationKyc(Partenaire guarantor) throws OwomiException {
        boolean validator = guarantor != null && guarantor.getKycValidator() != null && guarantor.getKycValidator();
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, validator);
    }

    public CommonResponseWs detailPreValidationKyc(Partenaire guarantor, String telephone) throws OwomiException {
        if(guarantor == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        boolean validator = guarantor.getKycValidator() != null && guarantor.getKycValidator();
        if(!validator) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire not validator", 402, null);
        Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
        if(partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "non partenaire", 410, null);
        if(partenaire.isIdentifier()) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire deja verifier", 411, null);
        Validation validation = this.findOneValidationIdentificationPartenaire(telephone);
        if (validation == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "aucune validation en attente", 420, null);
        boolean guarantorValidated = validation.getGuarantorValidated() != null && validation.getGuarantorValidated();
        if (guarantorValidated) return new CommonResponseWs(WsOperationResponseStatus.failed, "validation deja prevalider", 420, null);
        if(!partenaire.getId().equals(validation.getIdref())) return new CommonResponseWs(WsOperationResponseStatus.failed, "validation ne correspond pas au partenaire", 421, null);
        ValidationWs result = this.validationService.convertValidationToWs(validation, partenaire);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

    private Validation findOneValidationIdentificationPartenaire(String telephone) {
        List<String> references = Arrays.asList(Partenaire.class.getSimpleName());
        List<String> types = Arrays.asList("identification");
        List<Validation> validations = validationRepository.findByMotifassistantAndTablerefInAndTicketvalidationInAndStatutBetweenOrderByIdDesc(
                telephone, references, types, 0, 2
        );
        return validations.stream().findFirst().orElse(null);
    }

    public CommonResponseWs preValidateKyc(Partenaire guarantor, ValidationKycAgentWs ws) throws OwomiException {
        logger.debug("preValidateKyc ws = {}", ws);
        if(guarantor == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire", 401, null);
        boolean validator = guarantor.getKycValidator() != null && guarantor.getKycValidator();
        if(!validator) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire not validator", 402, null);
        Partenaire partenaire = partenaireRepository.findOneByTelephone(ws.getTelephone());
        if(partenaire == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "non partenaire", 410, null);
        if(partenaire.isIdentifier()) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire deja verifier", 411, null);
        PartenaireKyc entity = this.findOne(partenaire);
        if(entity != null && entity.getValidated() != null && entity.getValidated()) return new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire deja identifier", 411, null);
        Validation validation = this.findOneValidationIdentificationPartenaire(ws.getTelephone());
        if (validation == null) return new CommonResponseWs(WsOperationResponseStatus.failed, "aucune validation en attente", 420, null);
        boolean guarantorValidated = validation.getGuarantorValidated() != null && validation.getGuarantorValidated();
        if (guarantorValidated) return new CommonResponseWs(WsOperationResponseStatus.failed, "validation deja prevalider", 420, null);
        if(!partenaire.getId().equals(validation.getIdref())) return new CommonResponseWs(WsOperationResponseStatus.failed, "validation ne correspond pas au partenaire", 421, null);
        Date expiration = Optional.ofNullable(ws.getDateExpirationStr()).map(this::convertDateFromString).orElse(null);
        boolean verification = expiration == null || expiration.before(new Date());
        if (verification) return new CommonResponseWs(WsOperationResponseStatus.failed, "La carte est expirée.", 424, null);
        String birthdayInput = Optional.ofNullable(ws.getDateNaissanceStr()).map(this::convertDateFromString)
                .map(v -> dateFormat.format(v)).orElse(null);
        String birthdayCurrent = Optional.ofNullable(partenaire.getDatenaissance())
                .map(v -> dateFormat.format(v)).orElse(null);
        verification = birthdayInput == null || !birthdayInput.equalsIgnoreCase(birthdayCurrent);
        logger.debug("preValidateKyc birthdayInput {} birthdayCurrent {} verification {}", birthdayInput, birthdayCurrent, verification);
        if(verification) return new CommonResponseWs(WsOperationResponseStatus.failed, "La date de naissance n'est pas conforme.", 426, null);
        verification = ws.getPicture() == null || ws.getPicture().trim().isEmpty();
        if (verification) return new CommonResponseWs(WsOperationResponseStatus.failed, "Image requis.", 425, null);
        validation = this.preValidateKyc(validation, guarantor, ws);
        entity = this.submitGuarantor(partenaire, guarantor, ws);
        boolean validate = validation.getGuarantorValidated() != null && validation.getGuarantorValidated();
        this.validateKyc(partenaire, validation, entity, validate);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
    }

    public PartenaireKyc submitGuarantor(Partenaire partenaire, Partenaire guarantor, ValidationKycAgentWs ws) {
        PartenaireKyc entity = this.findOne(partenaire);
        if (entity == null) return null;
        entity.setPartenaire(partenaire);
        entity.setGuarantor(guarantor);
        String guarantorFacePictureUrl = Optional.ofNullable(ws.getPicture()).orElse(entity.getGuarantorFacePictureUrl());
        entity.setGuarantorFacePictureUrl(guarantorFacePictureUrl);
        Date expiration = Optional.ofNullable(ws.getDateExpirationStr()).map(this::convertDateFromString).orElse(entity.getCardExpirationDate());
        entity.setCardExpirationDate(expiration);
        entity = this.partenaireKycRepository.save(entity);
        return entity;
    }

    public Validation preValidateKyc(Validation validation, Partenaire guarantor, ValidationKycAgentWs ws) {
        validation.setGuarantorPartenaireId(guarantor.getId());
        validation.setGuarantorPartenaireTelephone(guarantor.getTelephone());
        Image image = this.saveImage(validation.getGuarantorPartenaireImage(), ws.getPicture());
        validation.setGuarantorPartenaireImage(image);
        validation.setGuarantorValidated(image != null);
        validation = this.validationRepository.save(validation);
        return validation;
    }

    public void validateKyc(Partenaire partenaire, Validation validation, PartenaireKyc entity, Boolean status) {
        if (!status) return;
        if (validation != null) {
            validation.setStatut(3);
            validation.setStatus(WsOperationResponseStatus.success.name());
            validation = this.validationRepository.save(validation);
        }
        if (partenaire != null) {
            partenaire.setIdentifier(true);
            partenaire = this.partenaireRepository.save(partenaire);
        }
        if (entity != null) {
            entity.setValidated(true);
            entity = this.partenaireKycRepository.save(entity);
        }
        this.notificationSuccessValidationKyc(partenaire, true);
    }

    public void notificationSuccessValidationKyc(Partenaire partenaire, Boolean status) {
        if (!status) return;
        try {
            Map<String, String> data = new HashMap<>();
            NotifWs notifWs = new NotifWs();
            data.put("id", "4546");
            notifWs.setData(data);
            notifWs.setTitle("Succès de la vérification de votre identité");
            String message = "<p>Chers Clients!</p>\n" +
                    "\n" +
                    "<p>Votre compte est valid&eacute; </p>\n";
            notifWs.setBody(message);
            notifWs.setApp(FirebaseAppEnums.MOBILE);
            this.createMessageValidation(partenaire, notifWs, false);
            EmailWs mail = EmailWs.builder().to(partenaire.getEmail()).subject(notifWs.getTitle()).content(notifWs.getBody()).build();
            this.notificationService.sendEmail(mail);
            this.payerFactureEcoFamAbonneGroupeServiceClient.updateKycAbonne(partenaire.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CommonResponseWs createMessageValidation(
            Partenaire partenaire, NotifWs ws, Boolean prioritaire
    ) throws NotificationException {
        if(partenaire == null) return  new CommonResponseWs(WsOperationResponseStatus.failed, "partenaire not found", 404, null);
        if (ws.getData() == null) ws.setData(new HashMap<>());
        ws.getData().putIfAbsent("id", "7550");
        this.notificationService.notificationAndAl(
                ws.getApp(), partenaire, ws.getTitle(), ws.getBody(), prioritaire, ws.getData()
        );
        PartenaireInfosWs partenaireInfosWs = mapper.map(partenaire, PartenaireInfosWs.class);
        return new CommonResponseWs(WsOperationResponseStatus.success, "success", 200, partenaireInfosWs);
    }



    public PartenaireKycWs convertPartenaireKycToWs( PartenaireKyc entity) {
        if (entity == null) return null;
        PartenaireKycWs result = mapper.map(entity, PartenaireKycWs.class);
        if (entity.getBirthday() != null) {
            result.setBirthdayTimestamp(entity.getBirthday().getTime());
            result.setBirthdayStr(entity.getBirthday().toString());
        }
        if (entity.getCardExpirationDate() != null) {
            result.setCardExpirationDateTimestamp(entity.getCardExpirationDate().getTime());
            result.setBirthdayStr(entity.getCardExpirationDate().toString());
        }
        return result;
    }

    private void sendInValidationDemande(PartenaireKyc entity) {
        if (entity == null) return;
        if(entity.getValidated() != null && entity.getValidated()) return ;
        Partenaire partenaire = entity.getPartenaire();
        this.sendInValidationDemande(partenaire);
    }

    private void sendInValidationDemande(Partenaire partenaire) {
        if(partenaire == null || partenaire.isIdentifier()) return ;
        List<Validation> list = validationRepository.findByTablerefAndIdrefAndTicketvalidationAndStatutBetween(Partenaire.class.getSimpleName(), partenaire.getId(), "identification", 0, 2);
        if(!list.isEmpty()) return;
        Validation validation = new Validation();
        validation.setReference(Partenaire.class.getSimpleName());
        validation.setMotifassistant(partenaire.getTelephone());
        validation.setTableref(Partenaire.class.getSimpleName());
        validation.setStatut(0);
        validation.setIdref(partenaire.getId());
        validation.setNature(i18n.internationalize("send.demand.for.validation.message", Partenaire.class.getSimpleName()));
        validation.setTicketvalidation("identification");
        validation.setDateEnvoi(new Timestamp(System.currentTimeMillis()));
        validation.setMotifreceveur(partenaire.getNom() + " " + partenaire.getPrenoms());
        validation = validationRepository.save(validation);
        this.validationService.inSendValidation(partenaire, validation);
    }


    private Image saveImage(Image image, String url) {
        if (url == null || url.isEmpty()) return image;
        image = image == null ? new Image() : image;
        image.setUrl(url);
        image.setEmplacement(url);
        image.setHash(url);
        image = imageRepository.save(image);
        return image;
    }

    @Transactional
    public CommonResponseWs uploadKycDocs(
            Partenaire partenaire, Map<TypeDocumentEnums, String> documents
    ) throws IOException {
        String cardIdentityUrl = documents.get(TypeDocumentEnums.CARD_IDENTITY);
        String faceIdentityUrl = documents.get(TypeDocumentEnums.FACE_IDENTITY);
        boolean hasChange = false;
        if (cardIdentityUrl != null && !cardIdentityUrl.isEmpty()) {
            Image image = this.saveImage(partenaire.getPiece(), cardIdentityUrl);
            hasChange = partenaire.getPiece() == null || !cardIdentityUrl.equalsIgnoreCase(partenaire.getPiece().getUrl());
            partenaire.setPiece(image);
            partenaire = partenaireRepository.save(partenaire);
        }
        if (faceIdentityUrl != null && !faceIdentityUrl.isEmpty()) {
            Image image = this.saveImage(partenaire.getPiecePhoto(), faceIdentityUrl);
            hasChange = hasChange || (partenaire.getPiecePhoto() == null || !faceIdentityUrl.equalsIgnoreCase(partenaire.getPiecePhoto().getUrl()));
            partenaire.setPiecePhoto(image);
            partenaire = partenaireRepository.save(partenaire);
        }

        if (hasChange && partenaire.getPiece() != null && partenaire.getPiecePhoto() != null)
            this.sendInValidationDemande(partenaire);

        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, null);
    }

    public CommonResponseWs readKycDocs(Partenaire partenaire) {
        String cardIdentityUrl = Optional.ofNullable(partenaire).map(Partenaire::getPiece).map(Image::getUrl).orElse(null);
        String faceIdentityUrl = Optional.ofNullable(partenaire).map(Partenaire::getPiecePhoto).map(Image::getUrl).orElse(null);
        Map<TypeDocumentEnums, String> result = new HashMap<>();
        result.put(TypeDocumentEnums.CARD_IDENTITY, cardIdentityUrl);
        result.put(TypeDocumentEnums.FACE_IDENTITY, faceIdentityUrl);
        return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, result);
    }

}
