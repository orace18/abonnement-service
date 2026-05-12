package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nFactory;
import com.clubchretien.abonnement.utils.ActivityLogUtils;
import com.clubchretien.abonnement.utils.UtilsService;
import com.owomi.commons.constants.MobileConstant;
import com.owomi.commons.constants.ResponseConstant;
import com.owomi.commons.enums.*;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.EntityUtils;
import com.owomi.commons.ws.*;
import com.owomi.database.entities.*;
import com.owomi.database.repositories.ActivityLogRepository;
import com.owomi.database.repositories.ConfigProduitRepository;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.database.repositories.ProduitPaysRepository;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.velocity.clients.VelocityClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class ActivityLogService implements MobileConstant, ResponseConstant {
	public static final String OWOMI_ACTIVITE = "activite";
	public static final SimpleDateFormat ACTIVITY_LOG_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss");
	private static Logger logger = LogManager.getLogger(ActivityLogService.class);
	private static String HTML_STARTER = "<!doctype html>";
	@Autowired(required = false)
	Mapper mapper;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	ProduitPaysRepository produitPaysRepository;
	@Autowired
	ConfigProduitRepository configProduitRepository;
	@Autowired
    UtilsService utilsService;
    @Autowired
    DataServiceClient dataServiceClient;
    @Autowired(required = false)
    VelocityClient velocityClient;
    @Autowired
    ActivityLogUtils activityLogUtils;
    @Autowired
	I18nFactory i18n;
	@Autowired
	I18nCommonsFactory i18nCommons;
	//    private NumberFormat formatter = new DecimalFormat("#0");
	@Autowired
	ActivityLogRepository activityLogRepository;


	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss");


	public void notificationPrioritaireAndAl(Partenaire partenaire, String title, String message) throws NotificationException {
		Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
		ActivityLog activityLog = new ActivityLog();
		activityLog.setActeur(acteur);
		activityLog.setDestinateur(partenaire);
		activityLog.setObjet(title);
		activityLog.setContenu(this.velocityClient.defaultTemp(message));
		activityLog.setHeure(ACTIVITY_LOG_DATE_FORMATTER.format(new Date()));
		activityLog.setStatut(Boolean.FALSE);
		activityLog.setProfil("");
		activityLog.setAction1(" ");
		activityLog.setPriority(true);
		activityLogRepository.save(activityLog);
	}

	public void notificationAlForAll(ActivityLogAllCreateWs ws, Partenaire partenaire) throws NotificationException {
		Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
		ActivityLog activityLog = mapper.map(ws, ActivityLog.class);
		activityLog.setActeur(acteur);
		activityLog.setDestinateur(partenaire);
		activityLog.setObjet(ws.getTitle());
		activityLog.setContenu(ws.getMessage());
		if (ws.getWrapped()) activityLog.setContenu(this.velocityClient.defaultTemp(ws.getMessage()));
		activityLog.setHeure(ACTIVITY_LOG_DATE_FORMATTER.format(new Date()));
		activityLog.setStatut(Boolean.FALSE);
		activityLog.setProfil(ws.getProfil());
		activityLogRepository.save(activityLog);
	}

	public void notificationAlForAll(RequestContextWs contextWs, ActivityLogAllCreateWs ws) throws NotificationException {
		List<Partenaire> partenaires = new ArrayList<>();
		List<String> emails = Arrays.asList("22965839644", "22994886168");
		if (EnvEnums.DEV.equals(contextWs.getEnv())) {
			partenaires = emails.stream().map(v -> this.partenaireRepository.findOneByEmail(v))
					.filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			partenaires = this.partenaireRepository.findAll();
		}
		for (Partenaire partenaire : partenaires) {
			this.notificationAlForAll(ws, partenaire);
		}
	}


	public ActivityLogWs create(Partenaire destinataire, ActivityLogObjetWs objet, ActivityLogContentWs contenu, List actionsRef) {
		String savableContenu = ActivityLogUtils.getSavableActivityLogContent(contenu);
		String savableObjet = ActivityLogUtils.getSavableActivityLogObjet(objet);
		String savableActionsRef = ActivityLogUtils.getSavableActivityLogActionsRef(actionsRef);

		Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
		ActivityLog activityLog = new ActivityLog();
		activityLog.setActeur(acteur);
		activityLog.setDestinateur(destinataire);
		activityLog.setObjet(savableObjet);
		activityLog.setContenu(savableContenu);
        activityLog.setHeure(ACTIVITY_LOG_DATE_FORMATTER.format(new Date()));
        activityLog.setStatut(Boolean.FALSE);
        activityLog.setProfil("");
        activityLog.setAction1(savableActionsRef);
        ActivityLog created = activityLogRepository.save(activityLog);
//        return created.getId();
        return mapper.map(created, ActivityLogWs.class);
    }

	@Transactional
	public List<ActivityLogWs> pageActivityLogWs(RequestContextWs requestContext, int page, int size, Partenaire partenaire) throws OwomiException {
		Pageable pageable = PageRequest.of(page, size);
		List<ActivityLog> activityLogList = activityLogRepository.findByDestinateurOrderByIdDesc(partenaire, pageable);
		List<ActivityLogWs> activityLogWsList = new ArrayList<>();
		for (ActivityLog activityLog : activityLogList) {
			map(requestContext, activityLogWsList, activityLog);
		}
		return activityLogWsList;
	}

	@Transactional
	public List<ActivityLogWs> pageActivityLogPriorityWs(RequestContextWs requestContext, Partenaire partenaire) throws OwomiException {
		List<ActivityLog> activityLogList = activityLogRepository.findByDestinateurAndPriorityAndStatutOrderByIdDesc(partenaire, true, false);
		List<ActivityLogWs> activityLogWsList = new ArrayList<>();
		for (ActivityLog activityLog : activityLogList) {
			map(requestContext, activityLogWsList, activityLog);
		}
		return activityLogWsList;
	}

	public List<ActivityLogWs> get(RequestContextWs requestContext, Integer id) throws OwomiException {

		List<ActivityLog> activityLogList = new ArrayList<>();
		activityLogList.add(activityLogRepository.findOneById(id));
		List<ActivityLogWs> activityLogWsList = new ArrayList<>();
		for (ActivityLog activityLog : activityLogList) {
			map(requestContext, activityLogWsList, activityLog);
		}
		return activityLogWsList;
	}

    @Transactional
    public List<ActivityLogWs> pageActivityLogNotReadWs(RequestContextWs requestContext, int page, int size, Partenaire partenaire) throws OwomiException {
		Pageable pageable = PageRequest.of(page, size);
		List<ActivityLog> activityLogList = activityLogRepository.findByDestinateurAndStatutOrderByIdDesc(partenaire, false, pageable);
        List<ActivityLogWs> activityLogWsList = new ArrayList<>();
        for (ActivityLog activityLog : activityLogList){
            map(requestContext,activityLogWsList, activityLog);
        }
        return activityLogWsList;
    }

    @Transactional
    public CommonResponseWs updateActivityLog(RequestContextWs requestContext, Integer idActivityLog) throws DataNotFoundException {
		ActivityLog activityLog = activityLogRepository.findOneById(idActivityLog);
        EntityUtils.checkIfEntityIsNull(ActivityLog.class, activityLog, idActivityLog);
		activityLog.setStatut(Boolean.TRUE);
		activityLog.setPriority(Boolean.FALSE);
		activityLogRepository.save(activityLog);
        CommonResponseWs response = new CommonResponseWs();
        response.setStatus(WsOperationResponseStatus.success);
        response.setMessage(i18nCommons.internationalize("ResponseConstant.MISE_A_JOUR_SUCCES"));
        return response;
    }

    public void createActivityLogMobileConfigMoyenMobile(RequestContextWs requestContext,String libelle , Partenaire destinataire ){
        Partenaire acteur = partenaireRepository.findOneByPrenoms(OWOMI_PARTENAIRE_PRENOM);
//        String contenu = i18nCommons.internationalize("ResponseConstant.CONFIGURATION_MOYEN_RECEPTION_ENVOYE_WITH_REPLACE").replace(i18nCommons.internationalize("MobileConstant.LIBELLE_TO_REPLACE"), libelle);
//        String objet = i18nCommons.internationalize("ResponseConstant.CONFIGURATION");

        ActivityLogContentEnums contenu = ActivityLogContentEnums.CONFIGURATION_MOYEN_RECEPTION_ENVOYE;
        ActivityLogObjetEnum objetEnum = ActivityLogObjetEnum.CONFIGURATION;
        ActivityLogStatutEnums statut = ActivityLogStatutEnums.EMPTY;

        createActivityLog(requestContext, acteur, destinataire, new ActivityLogObjetWs(objetEnum, new Object[0]), "", statut,
                    new ActivityLogContentWs(contenu, libelle), null);
    }

    public void createActivityLogMobileConfigMoyen(RequestContextWs requestContext,Partenaire destinataire ){
        Partenaire acteur = partenaireRepository.findOneByPrenoms(OWOMI_PARTENAIRE_PRENOM);
//        String objet = i18nCommons.internationalize("ResponseConstant.CONFIGURATION");
//        String contenu = i18nCommons.internationalize("ResponseConstant.CONFIGURATION_BANQUE_ACCOUNT_SEND");

        ActivityLogContentEnums contenu = ActivityLogContentEnums.CONFIGURATION_BANQUE_ACCOUNT_SEND;
        ActivityLogObjetEnum objetEnum = ActivityLogObjetEnum.CONFIGURATION;
        ActivityLogStatutEnums statut = ActivityLogStatutEnums.EMPTY;

        createActivityLog(requestContext, acteur, destinataire, new ActivityLogObjetWs(objetEnum, new Object[0]), "", statut,
                new ActivityLogContentWs(contenu, new Object[0]), null);
    }

//    public void makeActivityLog(RequestContextWs requestContext, Object object, Partenaire destinataire, String type) throws DataNotFoundException, OwomiException {
//        Partenaire acteur = partenaireRepository.findOneByPrenoms(OWOMI_PARTENAIRE_PRENOM);
//        ProduitPays produitPays ;
//        FraisCvResponseWs response;
//        switch (type){
//            case VELOCITY_DEPOT:
//                Depot depot = (Depot) object;
//                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(DEPOT, 3);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, DEPOT);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext,acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_DEPOT, String.valueOf(depot.getMontant()), String.valueOf(response.getFrais())), DEPOT, DEPOT_IMAGE, EN_COURS);
//                break;
//            case VELOCITY_VIREMENT:
//                Virement virement = (Virement) object;
//                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(VIREMENT, 3);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, VIREMENT);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext,acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_VIREMENT, String.valueOf(virement.getMontant()), String.valueOf(response.getFrais())), VIREMENT, VIREMENT_IMAGE, EN_COURS);
//                break;
//            case VELOCITY_MTN_MOBILE_MONEY:
//                MobileMoney mtnMobileMoney = (MobileMoney) object;
//                produitPays = produitPaysRepository.findOneByProduit_Nom(MTN_MOBILE_MONEY);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, MTN_MOBILE_MONEY);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext,acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_MTN_MOBILE_MONEY, String.valueOf(mtnMobileMoney.getMontant()), String.valueOf(response.getFrais())), MTN_MOBILE_MONEY, MTN_MOBILE_MONEY_IMAGE, EN_COURS);
//                break;
//            case VELOCITY_MOOV_MOBILE_MONEY:
//                MobileMoney moovMobileMoney = (MobileMoney) object;
//                produitPays = produitPaysRepository.findOneByProduit_Nom(MOOV_MOBILE_MONEY);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, MOOV_MOBILE_MONEY);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext, acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_MOOV_MOBILE_MONEY, String.valueOf(moovMobileMoney.getMontant()), String.valueOf(response.getFrais())), MOOV_MOBILE_MONEY, MOOV_MOBILE_MONEY_IMAGE, EN_COURS);
//                break;
//
//            case VELOCITY_RECHARGE_DEPOT:
//                Recharge recharge = (Recharge) object;
//                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(DEPOT, 3);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, DEPOT);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext,acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_DEPOT, String.valueOf(recharge.getMontant()), String.valueOf(response.getFrais())), DEPOT, DEPOT_IMAGE, EN_COURS);
//                break;
//
//            case VELOCITY_RECHARGE_INITIALISATION:
//                Recharge initial = (Recharge) object;
//                //creer dans base de donnée produit avec le nom de recharge_not_allow_access_info
//                produitPays = produitPaysRepository.findOneByProduit_Nom(RECHARGE_NOT_ALLOW_ACCESS_INFO);
//                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT,RECHARGE_NOT_ALLOW_ACCESS_INFO);
//                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
//                createActivityLog(requestContext,acteur, destinataire, velocityService.getActivityLogContent(requestContext,VELOCITY_RECHARGE_INITIALISATION, String.valueOf(initial.getMontant()), String.valueOf(response.getFrais())), RECHARGE_NOT_ALLOW_ACCESS_INFO, DEPOT_IMAGE, EN_COURS);
//                break;
//        }
//    }

    public void makeActivityLog(RequestContextWs requestContext, Object object, Partenaire destinataire, String type) throws DataNotFoundException, OwomiException {
        Partenaire acteur = partenaireRepository.findOneByPrenoms(OWOMI_PARTENAIRE_PRENOM);
        ProduitPays produitPays ;
        FraisCvResponseWs response;
        switch (type){
            case VELOCITY_DEPOT:
                Depot depot = (Depot) object;
                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(DEPOT, 3);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, DEPOT);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext,acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.DEPOT, new Object[0]), DEPOT_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.DEPOT, depot.getMontant(), response.getFrais()), null);
                break;
            case VELOCITY_VIREMENT:
                Virement virement = (Virement) object;
                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(VIREMENT, 3);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, VIREMENT);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext,acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.VIREMENT, new Object[0]), VIREMENT_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.VIREMENT, virement.getMontant(), response.getFrais()), null);
                break;
            case VELOCITY_MTN_MOBILE_MONEY:
                MobileMoney mtnMobileMoney = (MobileMoney) object;
                produitPays = produitPaysRepository.findOneByProduit_Nom(MTN_MOBILE_MONEY);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, MTN_MOBILE_MONEY);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext,acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.MTN_MOBILE_MONEY, new Object[0]), MTN_MOBILE_MONEY_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.MTN_MOBILEMONEY, mtnMobileMoney.getMontant(), response.getFrais()), null);
                break;
            case VELOCITY_MOOV_MOBILE_MONEY:
                MobileMoney moovMobileMoney = (MobileMoney) object;
                produitPays = produitPaysRepository.findOneByProduit_Nom(MOOV_MOBILE_MONEY);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, MOOV_MOBILE_MONEY);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext, acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.MOOV_MOBILE_MONEY, new Object[0]), MOOV_MOBILE_MONEY_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.MOOV_MOBILEMONEY, moovMobileMoney.getMontant(), response.getFrais()), null);
                break;
            case VELOCITY_RECHARGE_DEPOT:
                Recharge recharge = (Recharge) object;
                produitPays = produitPaysRepository.findOneByProduit_NomAndStatus(DEPOT, 3);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT, DEPOT);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext, acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.DEPOT, new Object[0]), DEPOT_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.DEPOT, recharge.getMontant(), response.getFrais()), null);
                break;

            case VELOCITY_RECHARGE_INITIALISATION:
                Recharge initial = (Recharge) object;
                //creer dans base de donnée produit avec le nom de recharge_not_allow_access_info
                produitPays = produitPaysRepository.findOneByProduit_Nom(RECHARGE_NOT_ALLOW_ACCESS_INFO);
                EntityUtils.checkIfEntityIsNull(ProduitPays.class, produitPays, NOM_PRODUIT,RECHARGE_NOT_ALLOW_ACCESS_INFO);
                response = dataServiceClient.getFraisCv(requestContext,new FraisCvWs(produitPays.getCode(), 1));
                createActivityLog(requestContext, acteur, destinataire, new ActivityLogObjetWs(ActivityLogObjetEnum.RECHARGE_NOT_ALLOW_ACCESS_INFO, new Object[0]), DEPOT_IMAGE, ActivityLogStatutEnums.EN_COURS,
                        new ActivityLogContentWs(ActivityLogContentEnums.DEPOT, initial.getMontant(), response.getFrais()), null);
                break;
        }
    }

//    private void createActivityLog(RequestContextWs requestContext, Partenaire acteur, Partenaire destinataire, String contenu, String objet, String profil, String statusString){
//        ActivityLog activityLog = new ActivityLog();
//        activityLog.setActeur(acteur);
//        activityLog.setDestinateur(destinataire);
//        activityLog.setContenu(contenu);
//        activityLog.setObjet(objet);
//        activityLog.setProfil(profil);
//        activityLog.setHeure(dateFormat.format(new Date()));
//        activityLog.setStatutstring(statusString);
//        activityLog.setStatut(Boolean.FALSE);
//        activitylogRepository.save(activityLog);
//    }

//    public void createActivityLog(RequestContextWs requestContext, Partenaire acteur, Partenaire destinataire, ActivityLogObjetEnum objet, String profil, ActivityLogStatutEnums status, ActivityLogContentWs contenu) {
//        String savableContenu = ActivityLogUtils.getSavableActivityLogContent(contenu);
//        String savableObjet = objet.toString();
//        String savableStatus = status.toString();
//
//        ActivityLog activityLog = new ActivityLog();
//        activityLog.setActeur(acteur);
//        activityLog.setDestinateur(destinataire);
//        activityLog.setObjet(savableObjet);
//        activityLog.setContenu(savableContenu);
//        activityLog.setProfil(profil);
//        activityLog.setHeure(dateFormat.format(new Date()));
//        activityLog.setStatutstring(savableStatus);
//        activityLog.setStatut(Boolean.FALSE);
//        activitylogRepository.save(activityLog);
//
//    }

    public int createActivityLog(RequestContextWs requestContext, Partenaire acteur, Partenaire destinataire, ActivityLogObjetWs objet, String profil, ActivityLogStatutEnums status, ActivityLogContentWs contenu, List actionsRef) {
        String savableContenu = ActivityLogUtils.getSavableActivityLogContent(contenu);
        String savableObjet = ActivityLogUtils.getSavableActivityLogObjet(objet);
        String savableActionsRef = ActivityLogUtils.getSavableActivityLogActionsRef(actionsRef);
        String savableStatus = status.toString();

        ActivityLog activityLog = new ActivityLog();
        activityLog.setActeur(acteur);
        activityLog.setDestinateur(destinataire);
        activityLog.setObjet(savableObjet);
        activityLog.setContenu(savableContenu);
        activityLog.setProfil(profil);
        activityLog.setHeure(dateFormat.format(new Date()));
        activityLog.setStatutstring(savableStatus);
        activityLog.setStatut(Boolean.FALSE);
        activityLog.setAction1(savableActionsRef);
        ActivityLog created = activityLogRepository.save(activityLog);
        return created.getId();
    }

    private void map(RequestContextWs requestContext,List<ActivityLogWs> activityLogWsList, ActivityLog activityLog) throws OwomiException {
		try {
			ActivityLogWs activityLogWs = mapper.map(activityLog, ActivityLogWs.class);
			activityLogWs.setTime(dateFormat.parse(activityLog.getHeure()).getTime());
			activityLogWs.setTitre(activityLog.getObjet());
			activityLogUtils.readableActivityLog(activityLogWs);
			activityLogWsList.add(activityLogWs);
		} catch (Exception exception) {
			logger.debug("requestContext : " + requestContext + " " + exception.getMessage());
			// logger.error("requestContext : " + requestContext, exception);
			// exception.printStackTrace();
//            throw new OwomiException(requestContext, exception.getMessage(), ErrorCodeEnums.NONE, exception.getMessage());
		}

    }

    public CommonResponseWs updateActivityLogByList(RequestContextWs requestContext, List<IdActivityLogWs> activityLogWsList) throws DataNotFoundException {

		List<Integer> idList = activityLogWsList.stream().map(IdActivityLogWs::getId).collect(Collectors.toList());
		List<ActivityLog> alList = (List<ActivityLog>) activityLogRepository.findAllById(idList);
		alList = alList.stream().filter(Objects::nonNull)
				.peek(al -> al.setStatut(Boolean.TRUE))
				.peek(al -> al.setPriority(Boolean.FALSE))
				.collect(Collectors.toList());
		activityLogRepository.saveAll(alList);

		System.out.println("Mise à jour effective");
		CommonResponseWs response = new CommonResponseWs();
		response.setStatus(WsOperationResponseStatus.success);
		response.setMessage(i18nCommons.internationalize("ResponseConstant.MISE_A_JOUR_SUCCES"));
		return response;
	}


}

