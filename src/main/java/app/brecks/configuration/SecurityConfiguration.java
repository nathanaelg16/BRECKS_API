package app.brecks.configuration;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecretKey secretKey() {
        return Jwts.SIG.HS256.key().build();
    }

    @Bean
    @Autowired
    public JwtParser jwtParser(SecretKey secretKey) {
        return Jwts.parser().verifyWith(secretKey).build();
    }
}
