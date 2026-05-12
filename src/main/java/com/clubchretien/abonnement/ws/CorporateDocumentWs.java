package com.clubchretien.abonnement.ws;

import org.springframework.web.multipart.MultipartFile;

public class   CorporateDocumentWs {
    private Integer corporate;
    private String typeDocument;
    private MultipartFile file;

    public Integer getCorporate() {
        return corporate;
    }

    public void setCorporate(Integer corporate) {
        this.corporate = corporate;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }
}
