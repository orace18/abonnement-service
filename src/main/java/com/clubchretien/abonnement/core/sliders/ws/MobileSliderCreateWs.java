package com.clubchretien.abonnement.core.sliders.ws;

import com.owomi.commons.ws.storage.UploadWs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MobileSliderCreateWs { private Integer id;
    private UploadWs image;
    private String countryPhoneCode;
//    private String statut;
//    private String typeAction;
//    private Integer idAction;
//    private Integer store;
}
