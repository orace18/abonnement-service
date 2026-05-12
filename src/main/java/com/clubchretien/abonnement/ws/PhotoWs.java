package com.clubchretien.abonnement.ws;

import org.springframework.web.multipart.MultipartFile;

public class PhotoWs {
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
