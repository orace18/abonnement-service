package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.DepotWs;
import org.springframework.web.multipart.MultipartFile;

public class DepotWss extends DepotWs {
    private MultipartFile fichier;
    public MultipartFile getFichier() {
        return fichier;
    }
    public void setFichier(MultipartFile fichier) {
        this.fichier = fichier;
    }
}
