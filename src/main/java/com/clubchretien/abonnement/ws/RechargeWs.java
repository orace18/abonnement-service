package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.CreateRechargeWs;
import org.springframework.web.multipart.MultipartFile;

public class RechargeWs extends CreateRechargeWs {
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
