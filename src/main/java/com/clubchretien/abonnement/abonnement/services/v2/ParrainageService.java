package com.clubchretien.abonnement.abonnement.services.v2;

import com.clubchretien.abonnement.abonnement.core.BaseConstant;
import com.clubchretien.abonnement.abonnement.services.AbonnementService;
import com.clubchretien.abonnement.abonnement.services.LogisticService;
import com.clubchretien.abonnement.abonnement.services.ReglementDeCompteService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.UserUpdateRequestWs;
import com.owomi.database.entities.Abonnement;
import com.owomi.database.entities.ArchiveParent;
import com.owomi.database.entities.TransactionsCcapp;
import com.owomi.database.repositories.ArchiveParentRepository;
import com.owomi.database.repositories.TransactionCcappRepository;
import com.owomi.database.repositories.repositories.AbonnementRepository;
import com.owomi.jwt_lib.utils.CustomJwtUtils;
import com.owomi.rest.client.abonnementClient.UserInfosClient;
import com.owomi.rest.client.auth.UserDataClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static com.clubchretien.abonnement.abonnement.core.BaseConstant.*;

@Service
public class ParrainageService {
    private static Logger logger = LogManager.getLogger(ParrainageService.class);
    @Autowired
    UserInfosClient userInfosClient;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    PaiementV2Service paiementV2Service;
    @Autowired
    ArchiveParentRepository archiveParentRepository;
    @Autowired
    ReglementDeCompteService reglementDeCompteService;
    @Autowired
    AbonnementRepository abonnementRepository;
    @Autowired
    TransactionCcappRepository transactionCcappRepository;

    /**
     * Cette fonction permet de sauvegarder les information parent pour
     * l'utilisateur
     * 
     * @param parentPhoneNumber
     * @param idPaiement
     * @param idAbonnement
     * @return
     * @throws OwomiException
     */
    public CommonResponseWs saveParrainageData(Optional<String> parentPhoneNumber, Long idPaiement, Long idAbonnement)
            throws OwomiException {
        if (parentPhoneNumber.isPresent()) {
            String phoneNumberParent = parentPhoneNumber.get();
            logger.info("Le numero fourni:{}", parentPhoneNumber.get());
            try {
                CommonResponseWs commonResponseWs = this.userInfosClient.getParentByPhone(phoneNumberParent,
                        CustomJwtUtils.getAccessToken(httpServletRequest));
                logger.info("La reponse apres la recuperaction du parent:{}", commonResponseWs);
                if (commonResponseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
                    return new CommonResponseWs(WsOperationResponseStatus.failed, "Le parent n'est pas dans le systeme",
                            400, false);
                }
                Map<String, Object> parentData = this.paiementV2Service.mapper(commonResponseWs);
                PARENT_ID = (String) parentData.get(CONST_PARENT_ID);
                PARENT_DISPLAYNAME = (String) parentData.get(CONST_PARENT_DISPLAYNAME);
                PARENT_CC_NUMBER = (String) parentData.get(CONST_PARENT_CC_NUMBER);
                ArchiveParent archiveParent = new ArchiveParent();
                archiveParent.setPaymentId(idPaiement);
                archiveParent.setAbonnementId(idAbonnement);
                archiveParent.setNumeroCCParent(PARENT_CC_NUMBER);
                archiveParent.setParentDisplayname(PARENT_DISPLAYNAME);
                archiveParent.setParentPhoneNumber(phoneNumberParent);
                archiveParentRepository.save(archiveParent);
                UserUpdateRequestWs updateRequestWs = new UserUpdateRequestWs();
                updateRequestWs.setParentId(Long.valueOf(PARENT_ID));
                updateRequestWs.setParentCCNumber(PARENT_CC_NUMBER);
                CommonResponseWs rs = this.userInfosClient.updateUserAndProfile(updateRequestWs,
                        CustomJwtUtils.getAccessToken(httpServletRequest));
                logger.info("La reponse de la mise a jour des donnes utilisateur: {}", rs);
                if (rs.getStatus().equals(WsOperationResponseStatus.failed)) {
                    return new CommonResponseWs(WsOperationResponseStatus.failed,
                            "Echec de mise a jour des donnees user", 400, false);
                }
                logger.info("Les donnee parent enregistree avec succes");
                return new CommonResponseWs(WsOperationResponseStatus.success,
                        "Les donnee parent enregistree avec succes", 200, true);
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
        return new CommonResponseWs(WsOperationResponseStatus.failed, "Donnees non sauvegardee", 500, false);
    }

    /**
     * Cette fonction permet de gerer les mise en reseau et le calcule des
     * commissions
     * 
     * @param idPaiement
     * @return
     * @throws OwomiException
     */
    public CommonResponseWs gererReseauAndCommission(Long idPaiement) throws OwomiException {
        logger.debug("======= Nous sommes dans la founction de reseau =======");
        Optional<Abonnement> ab = abonnementRepository.findByPaymentId(idPaiement);
        if (ab.isEmpty()) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Abonnement non trouve", 404, ab);
        }
        Abonnement abonnement = ab.get();
        TransactionsCcapp transactionsCcapp = transactionCcappRepository.findOneByJawupayId(idPaiement);
        if (transactionsCcapp == null) {
            return new CommonResponseWs(WsOperationResponseStatus.failed, "Transaction non trouve", 404,
                    transactionsCcapp);
        }
        ArchiveParent archiveParent = archiveParentRepository.findByAbonnementId(abonnement.getId());
        if (archiveParent == null) {
            logger.info("ℹ️ Pas d'archive parent pour abonnementId: {} - Création membre sans parrain",
                    abonnement.getId());
            boolean isCreate = this.reglementDeCompteService.creeMembre(abonnement,
                    CustomJwtUtils.getAccessToken(httpServletRequest));
            if (isCreate) {
                return new CommonResponseWs(WsOperationResponseStatus.success, "Membre cree avec success sans parrain",
                        200, true);
            } else {
                return new CommonResponseWs(WsOperationResponseStatus.failed, "Membre non cree sans parrain", 500,
                        false);
            }
        }
        String parentPhoneNumber = archiveParent.getParentPhoneNumber();
        logger.info("Le numeros parent:{}", parentPhoneNumber);
        if (parentPhoneNumber == null) {
            logger.debug("Pas de numeros de telephone parent retrouvee, surement non renseignee");
            boolean isCreate = this.reglementDeCompteService.creeMembre(abonnement,
                    CustomJwtUtils.getAccessToken(httpServletRequest));
            if (isCreate) {
                return new CommonResponseWs(WsOperationResponseStatus.success, "Membre cree avec success sans parrain",
                        200, true);
            } else {
                return new CommonResponseWs(WsOperationResponseStatus.failed, "Membre non cree sans parrain", 500,
                        false);
            }
        }
        CommonResponseWs responseWs = this.reglementDeCompteService.traiterCommissionsAbonnementNew(abonnement,
                Long.valueOf(PARENT_ID), CustomJwtUtils.getAccessToken(httpServletRequest));
        logger.debug("La repose du calcule de commission et de mise en reseau:{}", responseWs);
        if (responseWs.getStatus().equals(WsOperationResponseStatus.failed)) {
            return new CommonResponseWs(responseWs.getStatus(), responseWs.getMessage(), responseWs.getCode(), false);
        }
        return new CommonResponseWs(WsOperationResponseStatus.success, "Membre cree avec success sans parrain", 200,
                true);
    }


}
