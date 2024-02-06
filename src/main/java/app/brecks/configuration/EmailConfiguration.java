package app.brecks.configuration;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class EmailConfiguration {
    private static final List<String> SMTP_PROPERTIES;

    static {
        SMTP_PROPERTIES = new ArrayList<>();
        SMTP_PROPERTIES.add("mail.smtp.auth");
        SMTP_PROPERTIES.add("mail.smtp.starttls.enable");
        SMTP_PROPERTIES.add("mail.smtp.host");
        SMTP_PROPERTIES.add("mail.smtp.port");
        SMTP_PROPERTIES.add("mail.smtp.user");
        SMTP_PROPERTIES.add("mail.smtp.password");
        SMTP_PROPERTIES.add("mail.from");
    }

    @Bean
    @Autowired
    public Session getSession(Environment environment) {
        Properties emailProperties = new Properties();

        for (String property : SMTP_PROPERTIES)
            emailProperties.put(property, environment.getRequiredProperty(property, String.class));

        return Session.getInstance(emailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailProperties.getProperty("mail.smtp.user"),
                        emailProperties.getProperty("mail.smtp.password"));
            }
        });
    }
}
