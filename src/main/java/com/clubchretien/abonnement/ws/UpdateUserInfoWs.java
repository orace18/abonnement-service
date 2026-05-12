package com.clubchretien.abonnement.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInfoWs implements Serializable {

    private Long id;
    private Long userId;
    private String nom;
    private String prenom;
    private String telephone;
}
