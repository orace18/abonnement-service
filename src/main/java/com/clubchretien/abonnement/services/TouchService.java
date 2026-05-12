package com.clubchretien.abonnement.services;

import com.clubchretien.abonnement.i18n.I18nFactory;
import com.owomi.commons.ws.CallBackTouchWs;
import com.owomi.database.entities.CallbackTouch;
import com.owomi.database.repositories.CallbackTouchRepository;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TouchService {
    @Autowired
    CallbackTouchRepository callbackTouchRepository;
    @Autowired
    Mapper mapper;
    @Autowired
    I18nFactory i18n;

    public CallBackTouchWs callback (CallBackTouchWs callBackTouchWs){
        // recuperer le statut de la reponse
        CallBackTouchWs response = callBackTouchWs;
        String status = response.getStatus();
        String message = "";
        if (status.equals("SUCCESS")){
            // L'operation s'est deroulée avec succès
            message = i18n.internationalize("callback.success.message");
        }else{
            message = i18n.internationalize("callback.not.success.message");
        }
        CallbackTouch callbackTouch = new CallbackTouch();
        // remplir CallBackTouch et enregistrer dans la base
        callbackTouch = mapper.map(response,CallbackTouch.class);
        callbackTouch = callbackTouchRepository.save(callbackTouch);
        return callBackTouchWs;
    }



}
