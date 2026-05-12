package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.CreatePartenaireStepTwoWs;
import org.springframework.web.multipart.MultipartFile;

public class CreatePartenaireStepTwoWss extends CreatePartenaireStepTwoWs {
    private String photoProfil;
    private MultipartFile photo;


    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }


    public String getPhotoProfil() {
        return photoProfil;
    }

    public void setPhotoProfil(String photoProfil) {
        this.photoProfil = photoProfil;
    }
}
