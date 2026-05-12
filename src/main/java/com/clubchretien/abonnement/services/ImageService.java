package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.FileUploadException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Corporate;
import com.owomi.database.entities.Image;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.Validation;
import com.owomi.database.repositories.*;
import com.owomi.file.manager.services.FileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

@Component
public class ImageService {
    private static Logger logger = LogManager.getLogger(ImageService.class);
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    FileService fileService;
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    I18nFactory i18n;
    @Autowired
    ValidationService validationService;
    @Autowired
    BoutiqueShopizerRepository boutiqueShopizerRepository;


    public String storeImage(MultipartFile file) throws IOException {
        return fileService.storeFile(file);
    }

    public Resource getResourceByUrl(String url) {
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }


    @Transactional
    public CommonResponseWs updateImage(RequestContextWs requestContext, Partenaire partenaire, MultipartFile file) throws FileUploadException, IOException {
        CommonResponseWs response = new CommonResponseWs();
        String url = fileService.storeFile(file);
        Image image = partenaire.getImage();
        image.setUrl(url);
        imageRepository.save(image);
        response.setStatus(WsOperationResponseStatus.updated);
        response.setMessage(i18n.internationalize("image.update.success.message"));
        return response;
    }
    @Transactional
    public CommonResponseWs uploadImage(RequestContextWs requestContext,Partenaire partenaire, MultipartFile file) throws IOException {
        CommonResponseWs response = new CommonResponseWs();
//        String url = fileService.storeFile(file);
        //Image image = new Image();
        //image.setUrl(url);
        //image = imageRepository.save(image);
        //partenaire.setImage(image);
        partenaireRepository.save(partenaire);
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.upload.success.message"));
//        response.setData(url);
        return response;
    }
    @Transactional
    public Resource getImage(RequestContextWs requestContext,Partenaire partenaire) throws IOException, OwomiException {
        if (partenaire.getImage() == null) throw new OwomiException(i18n.internationalize("image.not.found.message"), ErrorCodeEnums.IMAGE_NOT_FOUND,"");
        String url = partenaire.getImage().getUrl();
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }

    public CommonResponseWs updateImageCorporate(RequestContextWs requestContext,Integer idCorporate, MultipartFile file) throws FileUploadException, IOException {
        CommonResponseWs response = new CommonResponseWs();
		Corporate corporate = corporateRepository.findOneById(idCorporate);
		String url = fileService.storeFile(file);
        Image image = corporate.getImage();
        image.setUrl(url);
        imageRepository.save(image);
        response.setStatus(WsOperationResponseStatus.updated);
        response.setMessage(i18n.internationalize("image.update.success.message"));
        return response;
    }
    @Transactional
    public CommonResponseWs uploadImageCorporate(RequestContextWs requestContext,Integer idCorporate, MultipartFile file) throws IOException, DataNotFoundException {
        CommonResponseWs response = new CommonResponseWs();
        Corporate corporate = corporateRepository.findOneById(idCorporate);
        EntityUtils.checkIfEntityIsNull(Corporate.class,corporate,idCorporate);
        String url = fileService.storeFile(file);
        Image image = new Image();
        image.setUrl(url);
        image = imageRepository.save(image);
        corporate.setImage(image);
        corporateRepository.save(corporate);
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.added.success.message"));
        return response;
    }

    @Transactional
    public Resource getImageCorporate(RequestContextWs requestContext,Integer idCorporate) throws IOException, OwomiException {
		Corporate corporate = corporateRepository.findOneById(idCorporate);
        if (corporate.getImage() == null) throw new OwomiException(i18n.internationalize("image.not.found.message"), ErrorCodeEnums.IMAGE_NOT_FOUND,"");
        String url = corporate.getImage().getUrl();
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }
    @Transactional
    public CommonResponseWs uploadImagePiece(RequestContextWs requestContext,Partenaire partenaire, MultipartFile file) throws IOException {
        CommonResponseWs response = new CommonResponseWs();
        String url = fileService.storeFile(file);
        Image image = new Image();
        image.setUrl(url);
        image.setEmplacement(url);
        image.setHash(url);
        image = imageRepository.save(image);
        partenaire.setPiece(image);
        partenaireRepository.save(partenaire);
        if (partenaire.getPiece() != null && partenaire.getPiecePhoto() != null && partenaire.getPiecePhoto().getHash() != null) {
            sendInValidationDemande(requestContext,partenaire);
        }
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.added.success.message"));
        return response;
    }
    @Transactional
    public Resource getImagePiece(RequestContextWs requestContext,Partenaire partenaire) throws IOException, OwomiException {
        if (partenaire.getImage() == null) throw new OwomiException(i18n.internationalize("image.not.found.message"), ErrorCodeEnums.IMAGE_NOT_FOUND," ");
        String url = partenaire.getPiece().getUrl();
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }
    @Transactional
    public CommonResponseWs uploadImagePiecePhoto(RequestContextWs requestContext,Partenaire partenaire, MultipartFile file) throws IOException {
        CommonResponseWs response = new CommonResponseWs();
        String url = fileService.storeFile(file);
        Image image = new Image();
        image.setUrl(url);
        image.setEmplacement(url);
        image.setHash(url);
        image = imageRepository.save(image);
        partenaire.setPiecePhoto(image);
        partenaireRepository.save(partenaire);
        if (partenaire.getPiece() != null && partenaire.getPiecePhoto() != null && partenaire.getPiece().getHash() != null) {
            sendInValidationDemande(requestContext, partenaire);
        }
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.added.success.message"));
        return response;
    }

    @Transactional
    public CommonResponseWs uploadImagePiecePhoto(RequestContextWs requestContext,Partenaire partenaire, String url) throws IOException {
        CommonResponseWs response = new CommonResponseWs();
        Image image = new Image();
        image.setUrl(url);
        image.setEmplacement(url);
        image.setHash(url);
        image = imageRepository.save(image);
        partenaire.setPiecePhoto(image);
        partenaireRepository.save(partenaire);
        if (partenaire.getPiece() != null && partenaire.getPiecePhoto() != null && partenaire.getPiece().getHash() != null) {
            sendInValidationDemande(requestContext, partenaire);
        }
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.added.success.message"));
        return response;
    }

    @Transactional
    public CommonResponseWs uploadImagePieceAndPiecePhoto(
            RequestContextWs requestContext, Partenaire partenaire, String pieceUrl, String piecePhotoUrl
    ) throws IOException {
        boolean haveChange = false;

        if (pieceUrl != null) {
            Image imagePiece = partenaire.getPiece() == null ? new Image() : partenaire.getPiece();
            imagePiece.setUrl(pieceUrl);
            imagePiece.setEmplacement(pieceUrl);
            imagePiece.setHash(pieceUrl);
            imagePiece = imageRepository.save(imagePiece);
            partenaire.setPiece(imagePiece);
            haveChange = true;
        }

        if (piecePhotoUrl != null) {
            Image imagePiecePhoto = partenaire.getPiecePhoto() == null ? new Image() : partenaire.getPiecePhoto();
            imagePiecePhoto.setUrl(piecePhotoUrl);
            imagePiecePhoto.setEmplacement(piecePhotoUrl);
            imagePiecePhoto.setHash(piecePhotoUrl);
            imagePiecePhoto = imageRepository.save(imagePiecePhoto);
            partenaire.setPiecePhoto(imagePiecePhoto);
            haveChange = true;
        }

        partenaireRepository.save(partenaire);

        if (haveChange && partenaire.getPiece() != null && partenaire.getPiecePhoto() != null) {
            sendInValidationDemande(partenaire);
        }

        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18n.internationalize("image.added.success.message"));
        response.setCode(200);
        return response;
    }

    @Transactional
    public CommonResponseWs uploadImagePieceAndPiecePhoto(
            RequestContextWs requestContext, Partenaire partenaire, MultipartFile piece, MultipartFile piecePhoto) throws IOException {
        boolean haveChange = false;
        String pieceUrl = null;
        if (piece != null) pieceUrl = fileService.storeFile(piece);
        String piecePhotoUrl = null;
        if (piecePhoto != null) piecePhotoUrl = fileService.storeFile(piecePhoto);
        return this.uploadImagePieceAndPiecePhoto(requestContext, partenaire, pieceUrl, piecePhotoUrl);
    }

	@Transactional
	public Resource getImagePiecePhoto(RequestContextWs requestContext, Partenaire partenaire) throws IOException, OwomiException {
		if (partenaire.getImage() == null)
			throw new OwomiException(i18n.internationalize("image.not.found.message"), ErrorCodeEnums.IMAGE_NOT_FOUND, " ");
		String url = partenaire.getPiecePhoto().getUrl();
		Resource resource = fileService.loadFileAsResource(url);
		return resource;
	}

    private void sendInValidationDemande(RequestContextWs requestContext, Partenaire partenaire) {
        this.sendInValidationDemande(partenaire);
    }

    private void sendInValidationDemande(Partenaire partenaire) {
        if(partenaire.isIdentifier()) return;
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


}

