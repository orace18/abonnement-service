package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.MoyenDePaiementWs;
import org.springframework.web.multipart.MultipartFile;

public class MoyenPaiementWs extends MoyenDePaiementWs {
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
