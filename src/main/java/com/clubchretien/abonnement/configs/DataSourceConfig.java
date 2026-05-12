//package com.clubchretien.abonnement.configs;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.core.env.Environment;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import javax.persistence.EntityManagerFactory;
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableTransactionManagement
//public class DataSourceConfig {
//
//    private final Environment env;
//
//    public DataSourceConfig(Environment env) {
//        this.env = env;
//    }
//
//    // ========================================
//    // BASE PRINCIPALE : abonnement_db
//    // ========================================
//
//    @Primary
//    @Bean(name = "abonnementDataSource")
//    public DataSource abonnementDataSource() {
//        String dbHost = env.getProperty("DB_HOST", "localhost");
//        String dbPort = env.getProperty("DB_PORT", "5432");
//        String dbUser = env.getProperty("DB_USER");
//        String dbPassword = env.getProperty("DB_PASSWORD");
//
//        System.out.println("🔍 DB_HOST: " + dbHost);
//        System.out.println("🔍 DB_PORT: " + dbPort);
//        System.out.println("🔍 DB_USER: " + dbUser);
//
//        // JDBC URL avec timeouts étendus
//        String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort
//                + "/abonnement_db"
//                + "?connectTimeout=120"           // 2 minutes pour se connecter
//                + "&socketTimeout=900"            // 15 minutes pour les opérations
//                + "&tcpKeepAlive=true"
//                + "&loginTimeout=120";
//        System.out.println("🔗 JDBC URL: " + jdbcUrl);
//
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(jdbcUrl);
//        config.setUsername(dbUser);
//        config.setPassword(dbPassword);
//        config.setDriverClassName("org.postgresql.Driver");
//
//        // Configuration du pool pour opérations longues
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(120000);      // 2 minutes
//        config.setIdleTimeout(600000);            // 10 minutes
//        config.setMaxLifetime(1800000);           // 30 minutes
//        config.setValidationTimeout(10000);       // 10 secondes
//        config.setLeakDetectionThreshold(300000); // 5 minutes
//        config.setConnectionTestQuery("SELECT 1");
//        config.setPoolName("AbonnementHikariPool");
//
//        // Paramètres PostgreSQL avancés
//        config.addDataSourceProperty("ApplicationName", "abonnement-service");
//
//        return new HikariDataSource(config);
//    }
//
//    @Primary
//    @Bean(name = "abonnementEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean abonnementEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("abonnementDataSource") DataSource dataSource) {
//
//        Map<String, Object> properties = new HashMap<>();
//
//        // Vous pouvez garder "update" pour la base principale (peu d'entités)
//        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "update");
//        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
//        System.out.println("⚙️ Abonnement DDL mode: " + ddlAuto);
//
//        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
//        properties.put("hibernate.jdbc.lob.non_contextual_creation", "true");
//        properties.put("hibernate.jdbc.batch_size", "20");
//        properties.put("hibernate.order_inserts", "true");
//        properties.put("hibernate.order_updates", "true");
//
//        // Améliorer les performances
//        properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
//
//        return builder
//                .dataSource(dataSource)
//                .packages("com.clubchretien.abonnement.abonnement.entities")
//                .persistenceUnit("abonnement")
//                .properties(properties)
//                .build();
//    }
//
//    @Primary
//    @Bean(name = "abonnementTransactionManager")
//    public PlatformTransactionManager abonnementTransactionManager(
//            @Qualifier("abonnementEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//
//    // ========================================
//    // BASE SECONDAIRE : authentication
//    // ========================================
//
//    @Bean(name = "authenticationDataSource")
//    public DataSource authenticationDataSource() {
//        String dbHost = env.getProperty("DB_HOST", "localhost");
//        String dbPort = env.getProperty("DB_PORT", "5432");
//        String dbUser = env.getProperty("DB_USER");
//        String dbPassword = env.getProperty("DB_PASSWORD");
//
//        // JDBC URL avec timeouts TRÈS LONGS (282 repositories !)
//        String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort
//                + "/authentication_db"
//                + "?connectTimeout=120"           // 2 minutes
//                + "&socketTimeout=1800"           // 30 MINUTES pour DDL massif
//                + "&tcpKeepAlive=true"
//                + "&loginTimeout=120";
//        System.out.println("🔗 Auth JDBC URL: " + jdbcUrl);
//
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(jdbcUrl);
//        config.setUsername(dbUser);
//        config.setPassword(dbPassword);
//        config.setDriverClassName("org.postgresql.Driver");
//
//        // Pool TRÈS patient pour 282 entités
//        config.setMaximumPoolSize(15);
//        config.setMinimumIdle(5);
//        config.setConnectionTimeout(120000);      // 2 minutes
//        config.setIdleTimeout(1800000);           // 30 minutes
//        config.setMaxLifetime(3600000);           // 1 heure
//        config.setValidationTimeout(10000);       // 10 secondes
//        config.setLeakDetectionThreshold(600000); // 10 minutes
//        config.setConnectionTestQuery("SELECT 1");
//        config.setPoolName("AuthenticationHikariPool");
//        config.setInitializationFailTimeout(-1);  // Attend indéfiniment
//
//        // Keep-alive pour connexions longues
//        config.addDataSourceProperty("ApplicationName", "abonnement-auth-service");
//        config.setKeepaliveTime(300000);          // Keep-alive toutes les 5 min
//
//        return new HikariDataSource(config);
//    }
//
//    @Bean(name = "authenticationEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean authenticationEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("authenticationDataSource") DataSource dataSource) {
//
//        Map<String, Object> properties = new HashMap<>();
//
//        // ✅ SOLUTION : Désactiver complètement le DDL auto
//        properties.put("hibernate.hbm2ddl.auto", "none");
//        System.out.println("⚙️ Authentication DDL mode: none (forced for 282 entities)");
//
//        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
//        properties.put("hibernate.jdbc.lob.non_contextual_creation", "true");
//
//        // 🔥 CRITIQUE : Désactiver l'extraction des métadonnées JDBC
//        properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
//        properties.put("hibernate.boot.allow_jdbc_metadata_access", "false");
//
//        // Ne pas valider le schéma au démarrage
//        properties.put("hibernate.validator.apply_to_ddl", "false");
//        properties.put("hibernate.check_nullability", "false");
//
//        // Optimisations runtime
//        properties.put("hibernate.jdbc.batch_size", "30");
//        properties.put("hibernate.order_inserts", "true");
//        properties.put("hibernate.order_updates", "true");
//        properties.put("hibernate.connection.provider_disables_autocommit", "true");
//
//        // Désactiver les statistiques (performance)
//        properties.put("hibernate.generate_statistics", "false");
//
//        return builder
//                .dataSource(dataSource)
//                .packages("com.owomi.database.entities")
//                .persistenceUnit("authentication")
//                .properties(properties)
//                .build();
//    }
//
//    @Bean(name = "authenticationTransactionManager")
//    public PlatformTransactionManager authenticationTransactionManager(
//            @Qualifier("authenticationEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//}