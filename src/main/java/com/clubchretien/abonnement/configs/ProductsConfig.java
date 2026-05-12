package com.clubchretien.abonnement.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
    @PropertySource(value = "classpath:owomi-products.properties", ignoreResourceNotFound = false),
    @PropertySource(value = "file:${SPRING_CONFIG_LOCATION}/owomi-products.properties", ignoreResourceNotFound = true)
})
public class ProductsConfig {
    @Value("${fraisInscription}")
    private String fraisInscription;
    @Value("${fraisMaintenance}")
    private String fraisMaintenance;
    @Value("Kit de démarrage Agent")
    private String kitDemarrageAgent;
    @Value("Starter kit Marchand")
    private String kitDemarrageMarchand;

    @Bean
    public ProductsInfos getProductsInfos(){
        ProductsInfos productsInfos = new ProductsInfos();
        productsInfos.setFraisInscription(fraisInscription);
        productsInfos.setFraisMaintenance(fraisMaintenance);
        productsInfos.setKitDemarrageAgent(kitDemarrageAgent);
        productsInfos.setKitDemarrageMarchand(kitDemarrageMarchand);
        return productsInfos;
    }
}
