package com.clubchretien.abonnement.abonnement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    private Long commandeId;
    private Long paymentId;
    private String statutPaiement;
    private String message;
    private Long transactionId;
    private String signature;
    private String token;
}