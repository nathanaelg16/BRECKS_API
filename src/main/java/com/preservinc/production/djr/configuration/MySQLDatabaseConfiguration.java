package com.preservinc.production.djr.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:database-${spring.profiles.active}.properties")
public class MySQLDatabaseConfiguration {
    private static final Logger logger = LogManager.getLogger();

    @Bean
    public DataSource getDataSource(@Value("${mysql.username}") String username,
                                    @Value("${mysql.password}") String password,
                                    @Value("${mysql.host}") String host,
                                    @Value("${mysql.port}") String port,
                                    @Value("${mysql.db}") String database,
                                    @Value("${mysql.options}") String options)
    {
        logger.info("[MySQL Database Configuration] Initializing HikariCP data source...");

        String DB_CONNECTION_URL = String.format("jdbc:mysql://%s:%s/%s%s", host, port, database, options);

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
