package com.preservinc.production.djr.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
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

// todo change config-*.properties files to be all in the application-*.properties so that they can be injected

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

    @Bean("spaces")
    public AmazonS3 buildSpaces() {
        final String endpoint = config.getProperty("spaces.endpoint");
        final String secret = config.getProperty("spaces.secret");
        final String key = config.getProperty("spaces.key");
        return AmazonS3Client.builder()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, Regions.US_EAST_1.getName()))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret)))
                .build();
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
