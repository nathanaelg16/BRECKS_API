package com.preservinc.production.djr.configuration;

import com.preservinc.production.djr.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class ServerConfiguration implements WebMvcConfigurer {
    private final AuthenticationInterceptor authenticationInterceptor;
    private final Properties config;

    public ServerConfiguration(@Autowired AuthenticationInterceptor authenticationInterceptor, @Value("${spring.profiles.active}") String activeProfile) throws IOException {
        this.authenticationInterceptor = authenticationInterceptor;
        this.config = new Properties();
        this.config.load(new ClassPathResource("config-%s.properties".formatted(activeProfile)).getInputStream());
    }


    @Bean("config")
    public Properties loadConfig() {
        return this.config;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.authenticationInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(this.config.getProperty("webapp.host"));
    }
}
