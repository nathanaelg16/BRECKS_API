package com.preservinc.production.djr.interceptor;

import com.preservinc.production.djr.auth.RevokedTokens;
import com.preservinc.production.djr.util.function.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Instant;
import java.util.Objects;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private static final Logger logger = LogManager.getLogger();
    private final RevokedTokens revokedTokens;
    private final JwtParser jwtParser;
    private final SecretKey secretKey;

    @Autowired
    public AuthenticationInterceptor(@Lazy SecretKey secretKey, @Lazy JwtParser jwtParser, RevokedTokens revokedTokens) {
        this.secretKey = secretKey;
        this.jwtParser = jwtParser;
        this.revokedTokens = revokedTokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        logger.info("[Auth Interceptor -- Pre-Handle] Checking request authentication at endpoint {}", request.getRequestURI());

        if (request.getMethod().equalsIgnoreCase("options")) return true;

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            logger.error("Invalid authorization header: {}", authorizationHeader);
            response.sendError(401, "Invalid authentication token");
            return false;
        }

        String[] authorizationTypeAndToken = authorizationHeader.split(" ");
        if (authorizationTypeAndToken.length != 2) {
            logger.error("Invalid authorization header: {}", authorizationHeader);
            response.sendError(401, "Invalid authentication token");
            return false;
        }

        String authorizationType = authorizationTypeAndToken[0].strip();
        String authorizationToken = authorizationTypeAndToken[1].strip();
        if (authorizationType.equals(AuthorizationType.BEARER.toString())) {
            Jws<Claims> claims = verifyToken(authorizationToken);
            if (claims != null) {
                request.setAttribute("claims", claims);
                return true;
            } else response.sendError(401, "Invalid authentication token");
        }
        else {
            logger.info("Unsupported authorization type: {}", authorizationType);
            response.sendError(401, "Unsupported authorization type");
        }

        return false;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) throws Exception {
        if (request.getRequestURI().equals("/validate")) attemptTokenRenewal(request, response);
    }

    private Jws<Claims> verifyToken(String token) {
        logger.info("[Auth Interceptor] Verifying token authentication: {}", token);
        if (this.revokedTokens.contains(token)) return null;
        try {
            return this.jwtParser.parseSignedClaims(token);
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | ExpiredJwtException | IllegalArgumentException e) {
            logger.info("Exception occurred parsing JWT token: {}", e.getMessage());
            return null;
        }
    }

    private void attemptTokenRenewal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("[Auth Interceptor] Checking if token qualifies for renewal...");
        Jws<Claims> claims = (Jws<Claims>) Objects.requireNonNull(request.getAttribute("claims"));
        Claims body = claims.getPayload();
        if (body.getExpiration().before(Date.from(Instant.now().plusMillis(Constants.DEFAULT_TOKEN_REFRESH_DELTA)))) {
            logger.info("[Auth Interceptor] Token qualifies for renewal! Renewing token...");
            String renewedToken = Jwts.builder()
                    .subject(body.getSubject())
                    .issuer("BRECKS Authentication Service")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusMillis(Constants.DEFAULT_TIMEOUT)))
                    .signWith(this.secretKey)
                    .compact();
            response.setHeader("X-Token-Renewal", renewedToken);
        } else logger.info("[Auth Interceptor] Token does not qualify for renewal.");
    }

    @Getter
    private enum AuthorizationType {
        BEARER ("Bearer");

        private final String name;

        AuthorizationType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.getName();
        }
    }
}
