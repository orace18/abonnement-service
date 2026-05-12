package com.clubchretien.abonnement.services;


import com.google.gson.Gson;
import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PartenaireWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.network.NetworkToolClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class NotificationTunnelSalesOnlineService {
    private static final Logger logger = LogManager.getLogger(NotificationTunnelSalesOnlineService.class);
    @Autowired
    DataServiceClient dataServiceClient;

    @Autowired
    NotificationService notificationService;

    @Autowired
    NetworkToolClient networkToolClient;

    @Autowired
    AuthentificationUtils authentificationUtils;
    @Autowired
    PartenaireRepository partenaireRepository;

    public void notificationTunnelInstallationAppFuture(Partenaire partenaire) {
        try {
            this.notificationTunnelInstallationApp(partenaire);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notificationTunnelInstallationApp(Partenaire partenaire) throws NotificationException, AuthentificationException, OwomiException {
        boolean subscribed = partenaire.getSubscribed() != null && partenaire.getSubscribed();
        if (subscribed) return;
        String token = authentificationUtils.getToken(authentificationUtils.getCode());
        CommonResponseWs commonResponseWs = networkToolClient.getChefCommunity(partenaire.getEmail(), token);
        if (!commonResponseWs.getStatus().equals(WsOperationResponseStatus.success)) return;
        PartenaireWs parent = new Gson().fromJson(new Gson().toJson(commonResponseWs.getData()), PartenaireWs.class);
        if (parent == null) return;
        Partenaire parrain = this.partenaireRepository.findOneById(parent.getId());
        String title = "Prospect : Installation Application ";
        String message = "Votre propect avec l'email : " + partenaire.getEmail();
        message += " " + partenaire.getNom() + " " + partenaire.getPrenoms();
        message += " vient d'installer l'application owomi. ";
        this.notificationService.notificationAndAl(FirebaseAppEnums.MOBILE, parrain, title, message);
    }

}
