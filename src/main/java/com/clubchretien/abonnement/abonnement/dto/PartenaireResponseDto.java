package com.clubchretien.abonnement.abonnement.dto;


public class PartenaireResponseDto {
    private String status;
    private PartenaireData data;
    private String info;
    private String message;
    private Long id;
    private String reference;
    private Integer code;

    // Getters et Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public PartenaireData getData() { return data; }
    public void setData(PartenaireData data) { this.data = data; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
}
