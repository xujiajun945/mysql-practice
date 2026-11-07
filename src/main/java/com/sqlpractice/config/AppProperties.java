package com.sqlpractice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private InitialAdmin initialAdmin = new InitialAdmin();
    private Datasource datasource = new Datasource();
    private List<String> ddlSchemas = new ArrayList<>();
    private int queryTimeoutSeconds = 5;
    private int maxResultRows = 1000;
    private int gradingRowsLimit = 2000;

    @Data
    public static class Jwt {
        private String secret;
        private long accessTtlMinutes = 30;
        private long refreshTtlDays = 7;
    }

    @Data
    public static class InitialAdmin {
        private String username = "admin";
        private String password = "Admin@123";
        private String nickname = "管理员";
    }

    @Data
    public static class Datasource {
        private Pool app = new Pool();
        private Pool ro = new Pool();
        private Pool ddl = new Pool();
    }

    @Data
    public static class Pool {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private String poolName = "pool";
    }
}
