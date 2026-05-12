package com.clubchretien.abonnement.core.partenaire.ws;

import com.owomi.commons.ws.PaysWs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MobilePartenaireWs {

    private Integer id;
    private String nom;
    private String prenoms;
    private String email;
    private Boolean emailVerified;
    private Boolean emailFaked;
    private String telephone;
    private String phoneCodeNumber;
    private String phoneNumber;
    private String phoneFullNumber;
    private Boolean phoneNumberVerified;
    private String etatActif;
    private String code;
    private String token;
    private Boolean identifier;
    private PaysWs pays;
    private String photoProfil;
    private String sexe;
    private String pined;
    private Long datenaissanceTimestamp;
    private Long dateActifTimestamp;
    private Boolean actif;
    private String modeActif;
    private String gradeLibelle;
    private Boolean kycValidator;

}
