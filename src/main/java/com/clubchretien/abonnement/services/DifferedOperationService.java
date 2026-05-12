package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.configs.NetworkInfos;
import com.clubchretien.abonnement.configs.ProductsInfos;
import com.clubchretien.abonnement.constants.MobileConstant;
import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.UtilsService;
import com.owomi.commons.constants.OperationConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.utils.NetworkUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.*;
import com.owomi.message.publisher.publishers.CommandeActiveMqPublisher;
import com.owomi.message.publisher.publishers.CorporateProcessorActiveMqPublisher;
import com.owomi.rest.client.accountService.AccountServiceClient;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.mobileService.MobileServiceClient;
import com.owomi.rest.client.neo4j.PartenaireServiceClient;
import com.owomi.rest.client.shopizerService.ShopizerServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class DifferedOperationService implements OperationConstant {
    private static Logger logger = LogManager.getLogger(DifferedOperationService.class);
    @Autowired
    CorporateRepository corporateRepository;
    @Autowired
    PanierProduitRepository panierProduitRepository;
    @Autowired
    ConfigProduitRepository configProduitRepository;
    @Autowired
    TransactionFinanciereService transactionFinanciereService;
    @Autowired
    PanierRepository panierRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    CommandespRepository commandespRepository;
    @Autowired
    ProduitPaysRepository produitPaysRepository;
    @Autowired
    OperationStringRepository operationStringRepository;
    @Autowired
    AutorisationsRepository autorisationsRepository;
    @Autowired
    ValidationRepository validationRepository;
    @Autowired
    PlanComptableRepository planComptableRepository;
    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired
    GradesRepository gradesRepository;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    UtilsService utilsService;
    @Autowired
    RetraitRepository retraitRepository;
    @Autowired
    CorporateProcessorActiveMqPublisher corporateProcessorActiveMqPublisher;
    @Autowired
    AccountSoldeRepository accountSoldeRepository;
    @Autowired
    AccountServiceClient accountServiceClient;
    @Autowired
    ShopizerServiceClient shopizerServiceClient;
    @Autowired
    I18nFactory i18n;
    @Autowired
    I18nCommonsFactory i18nCommons;
    @Autowired
    Mapper mapper;
    @Autowired
    CommandeActiveMqPublisher commandeActiveMqPublisher;
    @Autowired
    PartenaireCorporateRepository partenaireCorporateRepository;
    @Autowired
    ProductsInfos productsInfos;
    @Autowired
    PanierService panierService;
    @Autowired
    OperationService operationService;
    @Autowired
    GradePartenaireRepository gradePartenaireRepository;
    @Autowired
    PromoRepository promoRepository;
    @Autowired
    NetworkInfos networkInfos;
    @Autowired
    PartenaireServiceClient partenaireServiceClient;
    @Autowired
    ActivityLogCreator activityLogCreator;
    @Autowired
    PeriodeRepository periodeRepository;
    @Autowired
    HistoriqueActiviteRepository historiqueActiviteRepository;

    @Autowired
    MobileServiceClient mobileServiceClient;


    public CommonResponseWs initOperation(RequestContextWs requestContext, Partenaire partenaire, String produitName, Integer quantite) throws OwomiException, DataNotFoundException {
        Calendar lastWeekDay = Calendar.getInstance();
        Calendar firstWeekDay = Calendar.getInstance();
        firstWeekDay.set(Calendar.HOUR_OF_DAY, 0);
        firstWeekDay.set(Calendar.MINUTE, 0);
        firstWeekDay.set(Calendar.SECOND, 0);
        firstWeekDay.set(Calendar.HOUR, -168);
        String sdk ="sdk";
        if(produitName.equals(productsInfos.getFraisInscription()))sdk="FRAIS_ADHESION";
        if(produitName.equals(productsInfos.getFraisMaintenance()))sdk="FRAIS_MAINTENANCE";

        if (!produitName.equals(productsInfos.getFraisMaintenance())) {
            Double weekCv = commandespRepository.getCommissionsVolumeByPeriode(partenaire, firstWeekDay.getTime(), lastWeekDay.getTime());
            weekCv = (weekCv == null) ? 0.0 : weekCv;
            if (weekCv >= 0.0 && weekCv < 20.0) throw new OwomiException(i18n.internationalize("pay.process.grade.error.message1"), ErrorCodeEnums.CV_INITIAL_NOT_FOUND, i18n.internationalize("pay.process.grade.error.message2"));
        }
        ProduitPays produitPays = produitPaysRepository.findOneByProduit_NomAndPays_Devise(produitName,partenaire.getPays().getDevise());
        EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, "Nom", produitName);
        PanierProduit panierProduits1 = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.DESIR_PAYER_SUCCES_CORPORATE.name()).stream().reduce((first, last)->last).orElse(null);
        if(panierProduits1 != null ) {dataServiceClient.changeStatutOfPanierProduit(requestContext,panierProduits1.getReference(), PannierProduitStatusEnums.DESIR_PAYER_ECHEC_CORPORATE.name()); }
        PanierProduit panierProduit = panierProduitRepository.findByPanier_PartenaireAndProduitPaysAndStatus(partenaire, produitPays, PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.name()).stream().reduce((first, last)->last).orElse(null);
        if (panierProduit != null && produitName.equalsIgnoreCase(productsInfos.getFraisInscription())) throw new OwomiException(i18n.internationalize("pay.process.adhesion.already.paid.message1"), ErrorCodeEnums.FRAIS_ADHESION_ALREADY_BUY_ERROR, i18n.internationalize("pay.process.adhesion.already.paid.message2"));
        PanierWs panierWs = new PanierWs();
        panierWs.setReference(sdk);
        panierWs.setQuantite(quantite);
        panierWs.setCode(produitPays.getCode());
        panierProduit = panierService.panierProcess(requestContext,partenaire, panierWs);
        Integer[] ids = new Integer[]{panierProduit.getId()};
        return operationService.stepOneToBuyDiffered(requestContext,partenaire, ids,partenaire.getPays().getDevise().getCode(),0);
    }


    public CommonResponseWs payement(RequestContextWs requestContext, Partenaire partenaire, PaimentDifferedWs differedWs ) throws OwomiException {
        //TODO check operation on apple, if ok so....
        logger.info("Call service apple prod");
        logger.info("Input service apple :"+differedWs);
        Integer statusLive = mobileServiceClient.checkApple(differedWs);
        logger.info("status live : "+statusLive);

        Integer status = new Integer(-1);

        if(statusLive.equals(21007) || statusLive.equals(21002)){
            logger.info("Call service apple sandbox");
            logger.info("Input service apple :"+differedWs);
            status = mobileServiceClient.checkAppleSandBox(differedWs);
            logger.info("status sandbox : "+status);
        }

       if(differedWs.isTest()==true){
           logger.info("Mode test status : "+status);
            switch (status){
                case 0:{
                    //receipt is valid
                    processingAfterPayment(differedWs.getReferenceCommandeOwomi(),partenaire);
                    return new CommonResponseWs(WsOperationResponseStatus.success,null,null,"VALID_RECEIPT",null,null,null);
                }
                case 21000:{
                    //The request to the App Store was not made using the HTTP POST request method.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"REQUEST_FAILED",null,null,null);
                }
                case 21001:{
                    //This status code is no longer sent by the App Store.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_NOT_SEND",null,null,null);
                }
                case 21002:{
                    //The data in the receipt-data property was malformed or the service experienced a temporary issue. Try again.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"BAD_REQUEST",null,null,null);
                }
                case 21003:{
                    //The receipt could not be authenticated.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_NOT_AUTH",null,null,null);
                }
                case 21004:{
                    //The shared secret you provided does not match the shared secret on file for your account.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SECRET_NOT_FOUND",null,null,null);
                }
                case 21005:{
                    //The receipt server was temporarily unable to provide the receipt. Try again.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SERVER_UNABLE ",null,null,null);
                }
                case 21006:{
                    //This receipt is valid but the subscription has expired.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SUBSCRIPTION_EXPIRED",null,null,null);
                }
                case 21007:{
                    //This receipt is from the test environment.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_TEST_ENV",null,null,null);
                }
                case 21008:{
                    //This receipt is from the production environment.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_PROD_ENV",null,null,null);
                }
                case 21009:{
                    //Internal data access error. Try again later.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"INTERNAL_ERROR",null,null,null);
                }
                case 21010:{
                    //The user account cannot be found or has been deleted.
                    return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"USER_NOT_FOUND",null,null,null);
                }
                default:return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RETRY",null,null,null);
            }
        }else{
           logger.info("Mode non test status : "+status);
           switch (status){
               case 0:{
                   //receipt is valid
                   processingAfterPayment(differedWs.getReferenceCommandeOwomi(),partenaire);
                   return new CommonResponseWs(WsOperationResponseStatus.success,null,null,"VALID_RECEIPT",null,null,null);
               }
               case 21000:{
                   //The request to the App Store was not made using the HTTP POST request method.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"REQUEST_FAILED",null,null,null);
               }
               case 21001:{
                   //This status code is no longer sent by the App Store.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_NOT_SEND",null,null,null);
               }
               case 21002:{
                   //The data in the receipt-data property was malformed or the service experienced a temporary issue. Try again.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"BAD_REQUEST",null,null,null);
               }
               case 21003:{
                   //The receipt could not be authenticated.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_NOT_AUTH",null,null,null);
               }
               case 21004:{
                   //The shared secret you provided does not match the shared secret on file for your account.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SECRET_NOT_FOUND",null,null,null);
               }
               case 21005:{
                   //The receipt server was temporarily unable to provide the receipt. Try again.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SERVER_UNABLE ",null,null,null);
               }
               case 21006:{
                   //This receipt is valid but the subscription has expired.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"SUBSCRIPTION_EXPIRED",null,null,null);
               }
               case 21007:{
                   //This receipt is from the test environment.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_TEST_ENV",null,null,null);
               }
               case 21008:{
                   //This receipt is from the production environment.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RECEIPT_PROD_ENV",null,null,null);
               }
               case 21009:{
                   //Internal data access error. Try again later.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"INTERNAL_ERROR",null,null,null);
               }
               case 21010:{
                   //The user account cannot be found or has been deleted.
                   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"USER_NOT_FOUND",null,null,null);
               }
               default:return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"RETRY",null,null,null);
           }

             //   return new CommonResponseWs(WsOperationResponseStatus.failed,null,null,"BAD_REQUEST",null,null,null);
        }

    }



    private void processingAfterPayment(String referenceCommande, Partenaire partenaire) throws OwomiException {
        List<PanierProduit> panierProduitList = panierProduitRepository.findByReferenceCommande(referenceCommande);
        panierProduitList.forEach(panierProduit -> {
                    panierProduit.setStatus(PannierProduitStatusEnums.PRODUIT_PAYER_SUCCES.name());
                    panierProduitRepository.save(panierProduit);
                });
        String sdk = panierProduitList.get(0).getReference();

        if(sdk.equals("FRAIS_ADHESION")){
            double cv = getCvForWeek(partenaire);
            String grade = getGrade(cv);
            if (grade != null){
                Grades grades = gradesRepository.findOneByLibelle(grade);
                partenaire.setGrade(grades);
                partenaire.setVolumeCommissionEntree(cv);
                partenaire.setDebutActif(new Date());
                partenaire.setActif(Boolean.TRUE);
                partenaire.setEtatActif(NiveauActifEnums.getNiveauActif(cv).getLibelle());
                if(partenaire.getEtatActif().equals("Niveau 1")) partenaire.setEtatActif("Niveau 2");
                partenaire.setDateActif(NetworkUtils.getDureeActif());
                partenaire.setEtapeInscriptionReseau(1);
                partenaireRepository.save(partenaire);
                GradePartenaire gradePartenaire = new GradePartenaire();
                gradePartenaire.setGrade(grades);
                gradePartenaire.setPartenaire(partenaire);
                gradePartenaire.setRecordDate(new Date());
                gradePartenaireRepository.save(gradePartenaire);
            }
        }else if(sdk.equals("FRAIS_MAINTENANCE")){

            ProduitPays produitPays = panierProduitList.get(0).getProduitPays();
            Integer quantite = panierProduitList.get(0).getQuantite();
            ProfilWs profil = partenaireServiceClient.getProfil(partenaire.getId());
            Double cv = profil == null ? 0.0 : profil.getCvAchatMoisPasse();
            if(partenaire.getDateActif().before(new Date())){
                Calendar calendar  = Calendar.getInstance();
                calendar.setTime(NetworkUtils.getFinActifMaintenance());
                calendar.add(Calendar.MONTH, (int) quantite -1);
                String niveauActivite = NiveauActifEnums.getNiveauActif(cv).getLibelle();
                Integer indexNewActivite = Integer.valueOf(niveauActivite.charAt(niveauActivite.length() -1));
                Integer indexPartenaire = Integer.valueOf(partenaire.getEtatActif().charAt(niveauActivite.length() -1));
                if(indexPartenaire < indexNewActivite) partenaire.setEtatActif(NiveauActifEnums.getNiveauActif(cv).getLibelle());
                partenaire.setActif(Boolean.TRUE);
                partenaire.setDebutActif(NetworkUtils.getDebutActifMaintenance());
                partenaire.setDateActif(calendar.getTime());
                partenaire = partenaireRepository.save(partenaire);
                updateHistoriqueActivite(partenaire);
//                activityLogCreator.create(partenaire, velocityClient.paiementFraisMaintenance(partenaire.getDebutActif(),partenaire.getDateActif()),OBJET_ACTIVITY_LOG_REGLEMENT_MAINTENANCE);
                ActivityLogContentWs activityLogContentWs = new ActivityLogContentWs(ActivityLogContentEnums.REGLEMENT_FRAIS_MAINTENANCE, partenaire.getDebutActif(),partenaire.getDateActif());
                activityLogCreator.create(partenaire, new ActivityLogObjetWs(ActivityLogObjetEnum.REGLEMENT_FRAIS_MAINTENANCE, new Object[0]), activityLogContentWs, null);

            }else{
                Calendar calendar  = Calendar.getInstance();
                calendar.setTime(partenaire.getDateActif());
                calendar.add(Calendar.MONTH, (int) quantite);

                Calendar dat = Calendar.getInstance();
                dat.setTime(partenaire.getDateActif());
                dat.add(Calendar.DATE,1);
                Date dateDebut = dat.getTime();

                partenaire.setEtatActif(NiveauActifEnums.getNiveauActif(cv).getLibelle());
                partenaire.setActif(Boolean.TRUE);
                partenaire.setDateActif(calendar.getTime());
                partenaireRepository.save(partenaire);
                updateHistoriqueActivite(partenaire);
//                activityLogCreator.create(partenaire, velocityClient.paiementFraisMaintenance(dateDebut,partenaire.getDateActif()),OBJET_ACTIVITY_LOG_REGLEMENT_MAINTENANCE);
                ActivityLogContentWs activityLogContentWs = new ActivityLogContentWs(ActivityLogContentEnums.REGLEMENT_FRAIS_MAINTENANCE, dateDebut,partenaire.getDateActif());
                activityLogCreator.create(partenaire, new ActivityLogObjetWs(ActivityLogObjetEnum.REGLEMENT_FRAIS_MAINTENANCE, new Object[0]), activityLogContentWs, null);
            }
        }

    }

    private double getCvForWeek(Partenaire partenaire){
        Calendar firstWeekDay = Calendar.getInstance();
        firstWeekDay.set(Calendar.HOUR_OF_DAY, 0);
        firstWeekDay.set(Calendar.MINUTE, 0);
        firstWeekDay.set(Calendar.SECOND, 0);
        firstWeekDay.set(Calendar.HOUR, -168);
        Double cv = commandespRepository.getCommissionsVolumeByPeriode(partenaire, firstWeekDay.getTime(), Calendar.getInstance().getTime());
        return cv != null ? cv : 0.0;
    }

    public String getGrade(double cv){

        Double argentValue = networkInfos.getArgentValue();
        Double orValue = networkInfos.getOrValue();
        Double platiniumValue = networkInfos.getPlatiniumValue();
        Double diamantValue = networkInfos.getDiamantValue();
        Double diamantEliteValue = networkInfos.getDiamantEliteValue();
        List<Promo> promoList = promoRepository.findByDateFinPromoBeforeAndTypePromo(new Date(),TypePromoEnums.PROMO_GRADE.toString());
        if(!promoList.isEmpty()){
            Double cvArgent = promoList.stream().filter(promo -> promo.getPromoGrade().getGrade().equals(GradeEnums.ARGENT.getGrade())).mapToDouble(promo -> promo.getPromoGrade().getCv()).sum();
            Double cvOr = promoList.stream().filter(promo -> promo.getPromoGrade().getGrade().equals(GradeEnums.OR.getGrade())).mapToDouble(promo -> promo.getPromoGrade().getCv()).sum();
            Double cvPlatinium = promoList.stream().filter(promo -> promo.getPromoGrade().getGrade().equals(GradeEnums.PLATINIUM.getGrade())).mapToDouble(promo -> promo.getPromoGrade().getCv()).sum();
            Double cvDiamant = promoList.stream().filter(promo -> promo.getPromoGrade().getGrade().equals(GradeEnums.DIAMANT.getGrade())).mapToDouble(promo -> promo.getPromoGrade().getCv()).sum();
            Double cvDiamantElite = promoList.stream().filter(promo -> promo.getPromoGrade().getGrade().equals(GradeEnums.DIAMANT_ELIT.getGrade())).mapToDouble(promo -> promo.getPromoGrade().getCv()).sum();
            if(cvArgent!=0.0 ) argentValue = cvArgent;
            if(cvOr!=0.0 ) orValue = cvOr;
            if(cvPlatinium!=0.0 ) platiniumValue = cvPlatinium;
            if(cvDiamant!=0.0 ) diamantValue = cvDiamant;
            if(cvDiamantElite!=0.0 ) diamantEliteValue = cvDiamantElite;
        }
        if(cv >= argentValue && cv < orValue){
            return "Argent";
        }if(cv >= orValue && cv < platiniumValue){
            return "Or";
        }if(cv >= platiniumValue && cv < diamantValue){
            return "Platinium";
        }if(cv >= diamantValue && cv < diamantEliteValue){
            return "Diamant";
        }if(cv >= diamantEliteValue){
            return "Diamant Elite";
        }else{
            return null;
        }
    }

    private void updateHistoriqueActivite(Partenaire partenaire){
        Periode lastPeriodeMensuel = periodeRepository.findByTypePeriodeOrderByIdDesc(MobileConstant.MENSUEL).get(0);
        HistoriqueActivite historiqueActiviteFound = historiqueActiviteRepository.findOneByPartenaireAndPeriode(partenaire,lastPeriodeMensuel);
        if(historiqueActiviteFound!=null){
            HistoriqueActivite historiqueActivite = new HistoriqueActivite();
            historiqueActivite.setEtatActif(partenaire.getEtatActif());
            historiqueActivite.setPartenaire(partenaire);
            historiqueActivite.setPeriode(lastPeriodeMensuel);
            historiqueActivite.setDebutActivite(new Date());
            historiqueActiviteRepository.save(historiqueActivite);
        }
    }
}
