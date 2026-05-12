package com.clubchretien.abonnement.services;

import com.owomi.commons.utils.ActivityLogUtils;
import com.owomi.commons.ws.ActivityLogContentWs;
import com.owomi.commons.ws.ActivityLogObjetWs;
import com.owomi.database.entities.ActivityLog;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.ActivityLogRepository;
import com.owomi.database.repositories.PartenaireRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.clubchretien.abonnement.constants.MobileConstant.OWOMI_ACTIVITE;

@Component
public class ActivityLogCreator {
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    ActivityLogRepository activityLogRepository;

//    public void create(Partenaire destinataire, ActivityLogObjetEnum objet, ActivityLogContentWs contenu){
//        String savableContenu = ActivityLogUtils.getSavableActivityLogContent(contenu);
//        String savableObjet = objet.toString();
//
//        Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
//        ActivityLog activityLog = new ActivityLog();
//        activityLog.setActeur(acteur);
//        activityLog.setDestinateur(destinataire);
//        activityLog.setObjet(savableObjet);
//        activityLog.setContenu(savableContenu);
//        activityLog.setHeure(ACTIVITY_LOG_DATE_FORMATTER.format(new Date()));
//        activityLog.setStatut(Boolean.FALSE);
//        activityLog.setProfil("");
//        activityLogRepository.save(activityLog);
//    }

    public int create(Partenaire destinataire, ActivityLogObjetWs objet, ActivityLogContentWs contenu, List idActions){
        String savableContenu = ActivityLogUtils.getSavableActivityLogContent(contenu);
        String savableObjet = ActivityLogUtils.getSavableActivityLogObjet(objet);
        String savableActionsRef = ActivityLogUtils.getSavableActivityLogActionsRef(idActions);

        Partenaire acteur = partenaireRepository.findByPrenoms(OWOMI_ACTIVITE);
        ActivityLog activityLog = new ActivityLog();
        activityLog.setActeur(acteur);
        activityLog.setDestinateur(destinataire);
        activityLog.setObjet(savableObjet);
        activityLog.setContenu(savableContenu);
        activityLog.setHeure(new SimpleDateFormat("dd/MM/yyyy | HH:mm:ss").format(new Date()));
        activityLog.setStatut(Boolean.FALSE);
        activityLog.setProfil("");
        activityLog.setAction1(savableActionsRef);
        ActivityLog created = activityLogRepository.save(activityLog);
        return created.getId();
    }


}
