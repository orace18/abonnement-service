package com.clubchretien.abonnement.abonnement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackV2Request {
    private Long paymentId;
    private boolean success;
    private Long userId;
    private Long parentId;
    private String token;
    private String tokenCcApp;
}
