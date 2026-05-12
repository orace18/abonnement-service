package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.flocash.CreateFlocashRechargeWs;
import org.springframework.web.multipart.MultipartFile;

public class RechargeFlocashWs extends CreateFlocashRechargeWs {
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
