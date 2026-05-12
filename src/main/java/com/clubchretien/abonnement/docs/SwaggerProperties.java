package com.clubchretien.abonnement.docs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("swagger")
@Data
@JsonIgnoreProperties(value = "$$beanFactory")
public class SwaggerProperties {

    public Group group;
    public Path path;
    public MetaData metaData;
    public Documentation documentation;
//    public Map<String, ServerData> servers;
//
//    @Data
//    public static class ServerData {
//        public String description;
//        public String url;
//    }

    @Data
    public static class MetaData {
        public String title;
        public String description;
        public String company;
        public String website;
        public String email;
        public Licence licence;
        public Term term;
    }

    @Data
    public static class Group {
        public String name;
    }

    @Data
    public static class Documentation {
        public String name;
        public String url;
    }

    @Data
    public static class Path {
        public String mapping;
    }

    @Data
    public static class Licence {
        public String name;
        public String url;
    }

    @Data
    public static class Term {
        public String service;
    }

}
