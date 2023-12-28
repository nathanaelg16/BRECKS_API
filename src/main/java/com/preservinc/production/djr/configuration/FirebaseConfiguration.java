package com.preservinc.production.djr.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class FirebaseConfiguration {
    private final FirebaseApp firebaseApp;

    @Value("${spring.profiles.active}")
    private String activeProfile;
    private Properties config;

    @Autowired
    public FirebaseConfiguration(Properties config) throws IOException {
        InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        this.firebaseApp = FirebaseApp.initializeApp(options);
        this.config = config;
    }

    @Bean
    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Autowired
    @Bean("firebaseTestToken")
    public String getFirebaseTestToken(FirebaseAuth firebaseAuth) throws FirebaseAuthException {
        if (activeProfile.equalsIgnoreCase("test") || activeProfile.equalsIgnoreCase("local"))
            return firebaseAuth.createCustomToken(config.getProperty("firebase.uid"));
        else return null;
    }
}
