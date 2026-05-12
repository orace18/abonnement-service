package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.utils.NameUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.kyc.ClientKycWs;
import com.owomi.commons.ws.kyc.KycExtractedInfoWs;
import com.owomi.commons.ws.kyc.KycValidationWs;
import com.owomi.commons.ws.kyc.OciResponseWs;
import com.owomi.database.entities.HumanKycValidation;
import com.owomi.database.entities.Image;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.HumanKycValidationRepository;
import com.owomi.database.repositories.ImageRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.rest.client.OCR.OcrServiceClient;
import com.owomi.rest.client.apps.AppsInfos;
import com.owomi.rest.client.storage.FileStorageServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class KycService {

    @Autowired
    OcrServiceClient ocrServiceClient;
    @Autowired
    NotificationService notificationService;
    @Autowired
    FileStorageServiceClient fileStorageServiceClient;
    @Autowired
    HumanKycValidationRepository humanKycValidationRepository;
    @Autowired
    AppsInfos appsInfos;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    ImageRepository imageRepository;

    private static Logger logger = LogManager.getLogger(KycService.class);

    public CommonResponseWs validationKyc(Partenaire partenaire, KycValidationWs ws) throws OwomiException,
            NotificationException, IOException, NoSuchAlgorithmException, KeyManagementException, ParseException {

        if (partenaire.isIdentifier())
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Vous avez déjà identifier votre compte", 205,
                    null);

        byte[] photoCard = this.downloadImageFromUrl(buildFullFileUrl(ws.getPhotoCardUrl()));
        logger.info("Image photoCard téléchargée, taille: " + photoCard.length);
        byte[] photoSelfie = this.downloadImageFromUrl(buildFullFileUrl(ws.getPhotoSelfieUrl()));
        logger.info("Image photoSelfie téléchargée, taille: " + photoSelfie.length);

        ClientKycWs kycWs = new ClientKycWs();
        kycWs.setPhotoCard(photoCard);
        kycWs.setPhotoSelfie(photoSelfie);

        OciResponseWs ociResponseWs = ocrServiceClient.extractPiecesInfos(kycWs);

        logger.debug("OciResponseWs is ############# {}", ociResponseWs);

        // KycExtractedInfoWs extractedInfoWs =
        // objectMapper.convertValue(commonResponseWs.getData(),
        // KycExtractedInfoWs.class);
        KycExtractedInfoWs extractedInfoWs = ociResponseWs.getData();
        logger.debug("Extracted infos are ============> {} ", extractedInfoWs);
        logger.debug("Ocr response status ============> {} ", ociResponseWs.getSuccess());
        logger.debug("Partenaire is {}", partenaire);

        String dateStr = extractedInfoWs.getDateOfBirth();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Date dateFromString = sdf.parse(dateStr); // Conversion

        if (ociResponseWs.getSuccess()) {
            logger.info("OCR response is success");
            if (ociResponseWs.getCode().equalsIgnoreCase("success"))
                logger.info("OCR code is success");
            if (NameUtils.areNamesEqual(partenaire.getNom(), extractedInfoWs.getFirstNames()))
                logger.info("Partenaire nom is equal to extracted info firstname");
            if (NameUtils.areNamesEqual(partenaire.getPrenoms(), extractedInfoWs.getLastName()))
                logger.info("Partenaire prenom is equal to extracted info lastname");
            if (partenaire.getDatenaissance().equals(dateFromString))
                logger.info("Partenaire date of birth is equal to extracted info date of birth");

            if (ociResponseWs.getCode().equalsIgnoreCase("success")
                    && NameUtils.areNamesEqual(partenaire.getNom(), extractedInfoWs.getFirstNames())
                    && NameUtils.areNamesEqual(partenaire.getPrenoms(), extractedInfoWs.getLastName())
                    && partenaire.getDatenaissance().equals(dateFromString)) {
                partenaire.setIdentifier(true);
                Image piece = new Image();
                piece.setEmplacement(ws.getPhotoCardUrl());
                piece.setHash(ws.getPhotoCardUrl());
                piece.setUrl(ws.getPhotoCardUrl());
                imageRepository.save(piece);
                partenaire.setPiecePhoto(piece);
                partenaireRepository.save(partenaire);
                String title = "Validation d'identité";
                String message = "Félicitation Mr/Mme" + partenaire.getNom() + "\t" + partenaire.getPrenoms()
                        + " votre identité à été verifier avec succès";
                this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
                return new CommonResponseWs(WsOperationResponseStatus.success,
                        "Votre identité à été verifier avec succès", 200, extractedInfoWs);
            } else if (ociResponseWs.getCode().equalsIgnoreCase("verification_failed")
                    && NameUtils.areNamesEqual(partenaire.getNom(), extractedInfoWs.getFirstNames())
                    && NameUtils.areNamesEqual(partenaire.getPrenoms(), extractedInfoWs.getLastName())
                    && partenaire.getDatenaissance().equals(dateFromString)) {
                HumanKycValidation existant = humanKycValidationRepository
                        .findOneByIdPartenaireAndStatus(partenaire.getId(), "PENDING");
                if (existant == null) {
                    HumanKycValidation humanValidation = new HumanKycValidation();
                    humanValidation.setIdPartenaire(partenaire.getId());
                    humanValidation.setNom(partenaire.getPrenoms());
                    humanValidation.setPrenoms(partenaire.getNom());
                    humanValidation.setUrlPiece(ws.getPhotoCardUrl());
                    humanValidation.setUrlPhoto(ws.getPhotoSelfieUrl());
                    humanValidation.setStatus("PENDING");
                    humanKycValidationRepository.save(humanValidation);
                    String title = "Validation en attente";
                    String message = "Mr/Mme" + partenaire.getNom() + "\t" + partenaire.getPrenoms()
                            + " Vos informations sont en cours de verification et seront validées dans les plus brefs délais";
                    this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
                    return new CommonResponseWs(WsOperationResponseStatus.failed,
                            "La ressemblance entre vos deux photos est inferieur au taux de ressemblance recommander vous passerai à la validation humaine",
                            201, null);
                }

                return new CommonResponseWs(WsOperationResponseStatus.failed,
                        "Vous avez déjà une validation manuelle en cours", 201, null);

            } else if (ociResponseWs.getCode().equalsIgnoreCase("verification_failed")
                    && !NameUtils.areNamesEqual(partenaire.getNom(), extractedInfoWs.getFirstNames())
                    || !NameUtils.areNamesEqual(partenaire.getPrenoms(), extractedInfoWs.getLastName())
                    || !partenaire.getDatenaissance().equals(dateFromString)) {

            }

        } else {
            if (ociResponseWs.getCode().equalsIgnoreCase("processing_error")) {
                String title = "Rejet d'identité";
                String message = "Mr/Mme" + partenaire.getNom() + "\t" + partenaire.getPrenoms()
                        + " Vos informations ne correspondent pas à votre identité";
                this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, partenaire, title, message);
                return new CommonResponseWs(WsOperationResponseStatus.rejected,
                        "Vos informations ne correspondent pas à votre identitée", 202, null);
            }
        }
        return new CommonResponseWs(WsOperationResponseStatus.rejected,
                "Échec votre profil jawupay ne correspond pas aux information de votre carte.Veillez mettre votre profil à jour avec les inormation de votre carte.",
                203, null);
    }

    public byte[] downloadImageFromUrl(String imageUrl)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        trustAllSslCertificates(); // Bypasser le SSL

        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    private void trustAllSslCertificates() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    public String buildFullFileUrl(String partialPath) throws UnsupportedEncodingException {
        String baseUrl = "https://api-gateway-prod.owomi.mylocal/file-storage-service/v1.0/storage/read";
        String encodedPath = URLEncoder.encode(partialPath, StandardCharsets.UTF_8);
        return baseUrl + "?url=" + encodedPath + "&download=true";
    }

}
