package com.preservinc.production.djr;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestServerConfiguration {
    @Bean("activeProfile")
    public String getActiveProfile() {
        return "test";
    }
}
