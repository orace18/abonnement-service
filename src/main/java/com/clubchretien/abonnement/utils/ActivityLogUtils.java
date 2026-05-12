package com.clubchretien.abonnement.utils;

import com.clubchretien.abonnement.i18n.I18nCommonsFactory;
import com.clubchretien.abonnement.i18n.I18nVelocityFactory;
import com.owomi.commons.ws.ActivityLogWs;
import com.owomi.database.entities.ActivityLog;
import com.owomi.rest.client.apps.AppsInfos;
import com.owomi.velocity.clients.VelocityClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ActivityLogUtils extends com.owomi.commons.utils.ActivityLogUtils<ActivityLog, VelocityClient> {

    @Autowired
    private VelocityClient velocityClient;
    @Autowired
    private I18nVelocityFactory i18nVelocity;
    @Autowired
    private I18nCommonsFactory i18nCommons;
    @Autowired
    private AppsInfos appsInfos;


    public ActivityLogUtils() {
        super();
    }

    @Override
    public String getReadableObjet(String savable) {
        return getReadableObjetPriv(savable, i18nCommons);
    }

    @Override
    public String getReadableStatut(String savable) {
        return getReadableStatutPriv(savable, i18nCommons);
    }


    @Override
    public void readableActivityLog(ActivityLog savableActivityLog) {
        //Objet
        String readableObjet = this.getReadableObjet(savableActivityLog.getObjet());
        savableActivityLog.setObjet(readableObjet);
        //Statut
        String readableStatus = this.getReadableStatut(savableActivityLog.getStatutstring());
        savableActivityLog.setStatutstring(readableStatus);
        //Contenu
        String readableContenu = this.getReadableContent(savableActivityLog.getContenu(), this.velocityClient);
        savableActivityLog.setContenu(readableContenu);
    }


    public void readableActivityLog(ActivityLogWs savableActivityLog) {
        //Objet
        String readableObjet = this.getReadableObjet(savableActivityLog.getObjet());
        savableActivityLog.setObjet(readableObjet);
        //Statut
        String readableStatus = this.getReadableStatut(savableActivityLog.getStatutstring());
        savableActivityLog.setStatutstring(readableStatus);
        //Contenu
        String readableContenu = this.getReadableContent(savableActivityLog.getContenu(), this.velocityClient);
        savableActivityLog.setContenu(readableContenu);
    }

    @Override
    protected String executeVelocityClient(VelocityClient velocityClient, String velocityClientName, Object[] params) {
        Object[] paramsWithI18nObject = Arrays.copyOf(params, params.length + 1);
        paramsWithI18nObject[params.length] = i18nVelocity;
        return excecuteVelocityClientWithI18nObject(velocityClient, velocityClientName, paramsWithI18nObject);
    }


}
