package com.preservinc.production.djr.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class MySQLDatabaseConfiguration {
    private static final Logger logger = LogManager.getLogger();
    private final Properties config;

    @Autowired
    public MySQLDatabaseConfiguration(Properties config) {
        this.config = config;
    }

    @Bean
    public DataSource getDataSource() {
        logger.info("[MySQL Database Configuration] Initializing HikariCP data source...");
        String username = config.getProperty("mysql.user");
        String password = config.getProperty("mysql.pass");
        String DB_HOST = config.getProperty("mysql.host");
        String DB_PORT = config.getProperty("mysql.port");
        String DB_NAME = config.getProperty("mysql.db");
        String DB_OPTIONS = config.getProperty("mysql.options");
        String DB_CONNECTION_URL = String.format("jdbc:mysql://%s:%s/%s%s", DB_HOST, DB_PORT, DB_NAME, DB_OPTIONS);

        HikariConfig hikariConfig = getHikariConfig(DB_CONNECTION_URL, username, password);

        return new HikariDataSource(hikariConfig);
    }

    private static HikariConfig getHikariConfig(@NonNull String DB_CONNECTION_URL, @NonNull String username, @NonNull String password) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(DB_CONNECTION_URL);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setConnectionTimeout(600000);
        hikariConfig.setKeepaliveTime(30000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        return hikariConfig;
    }
}
