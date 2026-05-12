package com.clubchretien.abonnement.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
  @PropertySource(value = "classpath:reseau-config.properties", ignoreResourceNotFound = true),
  @PropertySource(value = "file:${SPRING_CONFIG_LOCATION}/reseau-config.properties", ignoreResourceNotFound = true)
})
public class NetworkConfig {
    @Value("${argent.value}")
    private double argentValue;
    @Value("${or.value}")
    private double orValue;
    @Value("${platinium.value}")
    private double platiniumValue;
    @Value("${diamant.value}")
    private double diamantValue;
    @Value("${diamant.elite.value}")
    private double diamantEliteValue;

    @Bean
    public NetworkInfos getNetworkInfos(){
        NetworkInfos networkInfos = new NetworkInfos();
        networkInfos.setArgentValue(argentValue);
        networkInfos.setOrValue(orValue);
        networkInfos.setPlatiniumValue(platiniumValue);
        networkInfos.setDiamantValue(diamantValue);
        networkInfos.setDiamantEliteValue(diamantEliteValue);
        return networkInfos;
    }
}
