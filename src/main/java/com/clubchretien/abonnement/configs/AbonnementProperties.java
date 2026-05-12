package com.clubchretien.abonnement.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "abonnement")
public class AbonnementProperties {

    private String pays;
    private String devise;

    private FormuleConfig classic;
    private FormuleConfig gold;
    private FormuleConfig diamond;

    @Data
    public static class FormuleConfig {
        private DureeConfig unmois;
        private DureeConfig troismois;
        private DureeConfig sixmois;
        private DureeConfig douzemois;
    }

    @Data
    public static class DureeConfig {
        private BigDecimal prix;
        private BigDecimal maxLeveeFond;
    }
}