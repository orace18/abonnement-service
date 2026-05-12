package com.clubchretien.abonnement.abonnement.core.utils;

import com.owomi.commons.ws.CommonResponseWs;

import com.clubchretien.abonnement.abonnement.dto.*;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionStateMapper {

   private final Gson gson;

    public SubscriptionStateMapper(Gson gson) {
        this.gson = gson;
    }

    /**
     * Convertit un JSON String en PartenaireResponseDto
     */
    public PartenaireResponseDto jsonToDto(Object jsonResponse) {
        return gson.fromJson(jsonResponse.toString(), PartenaireResponseDto.class);
    }

    /**
     * Convertit un CommonResponseWs en PartenaireResponseDto
     */
    public PartenaireResponseDto commonResponseToDto(CommonResponseWs commonResponseWs) {
        String json = gson.toJson(commonResponseWs);
        return gson.fromJson(json, PartenaireResponseDto.class);
    }

    /**
     * Extrait directement l'objet Partenaire
     */
    public Partenaire extractPartenaire(String jsonResponse) {
        PartenaireResponseDto response = jsonToDto(jsonResponse);
        if (response != null && response.getData() != null) {
            return response.getData().getPartenaire();
        }
        return null;
    }

}
