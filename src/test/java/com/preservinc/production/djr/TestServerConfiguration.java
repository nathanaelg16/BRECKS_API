package com.preservinc.production.djr;

import com.preservinc.production.djr.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

@TestConfiguration
public class TestServerConfiguration {
    @Bean("activeProfile")
    public String getActiveProfile() {
        return "test";
    }

    @Bean("config")
    @Autowired
    public Properties loadConfig(String activeProfile) throws IOException {
        Properties config = new Properties();
        config.load(new ClassPathResource("config-%s.properties".formatted(activeProfile)).getInputStream());
        return config;
    }
}
