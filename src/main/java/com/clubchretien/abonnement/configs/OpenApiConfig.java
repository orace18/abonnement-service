package com.clubchretien.abonnement.configs;//package com.owomi.mobile.service.configs;
//
//import io.swagger.v3.oas.annotations.enums.ParameterIn;
//import io.swagger.v3.oas.models.Components;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.Operation;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.media.StringSchema;
//import io.swagger.v3.oas.models.parameters.Parameter;
//import org.springdoc.core.customizers.OperationCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.method.HandlerMethod;
//
//@Configuration
//public class OpenApiConfig {
//    @Bean
//    public OpenAPI customOpenAPI() {
//        return new OpenAPI()
//                .components(new Components()
//                        /*.addSecuritySchemes("access-token",
//                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))*/)
//                .info(new Info().title("Mobile service  API").description(
//                        "This service is in charge of mobile transaction and operation").version("3.0"));
//    }
//
//
//    @Bean
//    public OperationCustomizer customGlobalHeaders(){
//        return (Operation operation, HandlerMethod handlerMethod) -> {
//            Parameter headerParam = new Parameter()
//                    .in(ParameterIn.HEADER.toString())
//                    .schema(new StringSchema())
//                    .name("access-token")
//                    .description("access-token")
//                    .required(false);
//
//            operation.addParametersItem(headerParam);
//            return operation;
//        };
//
//    }
//}
