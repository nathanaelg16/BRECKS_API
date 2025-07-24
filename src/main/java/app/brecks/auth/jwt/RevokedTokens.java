package app.brecks.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RevokedTokens {
    private final Set<AuthorizationToken> revokedTokens = new HashSet<>();
    private final JwtParser jwtParser;

    @Autowired
    public RevokedTokens(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public boolean contains(AuthorizationToken token) {
        return this.revokedTokens.contains(token);
    }

    public boolean add(AuthorizationToken token) {
        return this.revokedTokens.add(token);
    }

    public List<AuthorizationToken> get() {
        return this.revokedTokens.stream().toList();
    }

    @Scheduled(fixedDelay = 1_800_000)
    private void cleanup() {
        this.revokedTokens.stream().filter((token) -> {
            try {
                jwtParser.parse(token.token());
                return false;
            } catch (ExpiredJwtException e) {
                return true;
            }
        }).forEach(this.revokedTokens::remove);
    }
}
