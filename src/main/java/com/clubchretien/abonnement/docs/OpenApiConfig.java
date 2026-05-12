package com.clubchretien.abonnement.docs;

import com.clubchretien.abonnement.configs.Router;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;
import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN_TYPE;

@Configuration
@PropertySources({
        @PropertySource("classpath:properties/swagger.properties"),
})
@RequiredArgsConstructor
public class OpenApiConfig {

    private final SwaggerProperties properties;

    private Contact contact() {
        Contact contact = new Contact();
        contact.setName(this.properties.metaData.company);
        contact.setEmail(this.properties.metaData.email);
        contact.setUrl(this.properties.metaData.website);
        return contact;
    }

    private License license() {
        License license = new License();
        license.setName(this.properties.metaData.licence.name);
        license.setUrl(this.properties.metaData.licence.url);
        return license;
    }

    private Info info() {
        String version = "v1.0";
        return new Info().title(this.getName().toUpperCase())
                .description(this.getDescription())
                .version(version)
                .license(this.license())
                .contact(this.contact())
                .termsOfService(this.properties.metaData.term.service)
                ;
    }

    private ExternalDocumentation documentation() {
        return new ExternalDocumentation()
                .description(this.properties.documentation.name)
                .url(this.properties.documentation.url);
    }

    private SecurityScheme authorizationScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("BEARER")
                .in(SecurityScheme.In.HEADER)
                .bearerFormat("JWT");
    }

    private SecurityScheme accessToken() {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .name(ACCESS_TOKEN)
                .description(ACCESS_TOKEN)
                .scheme(ACCESS_TOKEN)
                .in(SecurityScheme.In.HEADER)
                ;
    }

    private Parameter authorizationType() {
        return new Parameter()
                .in(ParameterIn.HEADER.toString())
                .schema(new StringSchema())
                .name(ACCESS_TOKEN_TYPE)
                .description(ACCESS_TOKEN_TYPE)
                .required(false);
    }

    private SecurityScheme apiKey(String name, SecurityScheme.In in) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .name(name).description(name)
                .scheme(name).in(in)
                ;
    }

    private Components components() {
        Components components = new Components();
        components.addSecuritySchemes(ACCESS_TOKEN, this.apiKey(ACCESS_TOKEN, SecurityScheme.In.HEADER));
        components.addSecuritySchemes(ACCESS_TOKEN_TYPE, this.apiKey(ACCESS_TOKEN_TYPE, SecurityScheme.In.HEADER));
        return components;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(this.info())
                .externalDocs(this.documentation())
                .components(this.components())
                //.servers(this.servers())
                ;
    }

    @Bean
    public GroupedOpenApi all() {
        return GroupedOpenApi.builder()
                .group(this.getName())
                .pathsToMatch(Router.DOC_MODULES)
                .pathsToExclude(Router.DOC_MODULES_ROOT_EXCLUSION)
                .build();
    }

    private GroupedOpenApi buildGroupedOpenApi(String name, String root) {
        return GroupedOpenApi.builder()
                .group(this.getName(name))
                .pathsToMatch(root)
                .pathsToExclude(Router.ExclusionDocModules(root))
                .build();
    }

    @Bean
    public GroupedOpenApi core() {
        String name = "CORE";
        String root = "/social/v**/**";
        return this.buildGroupedOpenApi(name, root);
    }

    @Bean
    public GroupedOpenApi countries() {
        String name = "COUNTRIES";
        String root = "/countries/v**/**";
        return this.buildGroupedOpenApi(name, root);
    }


    private String getName(String suffix) {
        String name = this.properties.getMetaData().title;
        if (suffix != null) {
            name = name + " " + suffix;
        }
        return name.toUpperCase();
    }

    private String getName() {
        return this.getName(null);
    }

    private String getDescription() {
        return this.getName().concat(" ").concat(this.properties.metaData.description);
    }

}
