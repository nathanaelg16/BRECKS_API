package app.brecks.interceptor;

import app.brecks.auth.accesskey.AccessKey;
import app.brecks.auth.accesskey.AccessKeyManager;
import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.auth.jwt.RevokedTokens;
import app.brecks.dao.employees.IEmployeeDAO;
import app.brecks.service.authorization.IAuthorizationService;
import app.brecks.util.Constants;
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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private static final Logger logger = LogManager.getLogger();
    private static final Set<String> OPEN_ENDPOINTS;
    private final RevokedTokens revokedTokens;
    private final JwtParser jwtParser;
    private final AccessKeyManager accessKeyManager;
    private final IAuthorizationService authorizationService;
    private final IEmployeeDAO employeeDAO;

    static {
        OPEN_ENDPOINTS = new HashSet<>();
        OPEN_ENDPOINTS.add("/error");
        OPEN_ENDPOINTS.add("/login");
    }

    @Autowired
    public AuthenticationInterceptor(@Lazy JwtParser jwtParser, RevokedTokens revokedTokens,
                                     AccessKeyManager accessKeyManager, IAuthorizationService authorizationService,
                                     IEmployeeDAO employeeDAO) {
        this.jwtParser = jwtParser;
        this.revokedTokens = revokedTokens;
        this.accessKeyManager = accessKeyManager;
        this.authorizationService = authorizationService;
        this.employeeDAO = employeeDAO;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        logger.info("Checking request authentication at endpoint {}", request.getRequestURI());

        if (request.getMethod().equalsIgnoreCase("options")) return true;
        if (OPEN_ENDPOINTS.contains(request.getRequestURI())) return true;

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

        String authorizationTypeString = authorizationTypeAndToken[0].strip();
        AuthorizationType authorizationType = AuthorizationType.of(authorizationTypeString);

        if (authorizationType == null) {
            logger.info("Unsupported authorization type: {}", authorizationTypeString);
            response.sendError(401, "Unsupported authorization type");
            return false;
        }

        String authorizationToken = authorizationTypeAndToken[1].strip();

        switch (authorizationType) {
            case JWT -> {
                AuthorizationToken token = new AuthorizationToken(authorizationToken);
                if (verifyToken(token, request.getRequestURI())) {
                    request.setAttribute("token", token);
                    attemptTokenRenewal(token, response);
                    return true;
                } else response.sendError(401, "Invalid authentication token");
            }

            case ACCESS_KEY -> {
                AccessKey accessKey = this.accessKeyManager.verifyAccessKey(authorizationToken, request.getRequestURI());
                if (accessKey == null) response.sendError(401, "Invalid authentication token");
                else {
                    request.setAttribute("accessKey", accessKey);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean verifyToken(AuthorizationToken token, String endpoint) {
        logger.info("Verifying token authentication `{}` for request endpoint {}", token, endpoint);
        if (this.revokedTokens.contains(token)) return false;
        try {
            Jws<Claims> claims = this.jwtParser.parseSignedClaims(token.token());
            if (endpoint.startsWith("/admin")) {
                Integer userID = claims.getPayload().get("userID", Integer.class);
                return this.employeeDAO.findEmployeeByID(userID).isAdmin();
            }
            return true;
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | ExpiredJwtException | IllegalArgumentException e) {
            logger.info("Exception occurred parsing JWT token: {}", e.getMessage());
        } catch (SQLException e) {
            logger.info("Exception occurred checking db: {}", e.getMessage());
            logger.error(e);
        }

        return false;
    }

    private void attemptTokenRenewal(AuthorizationToken token, HttpServletResponse response) {
        logger.info("Checking if token qualifies for renewal...");
        Jws<Claims> jwsToken = this.jwtParser.parseSignedClaims(token.token());
        Claims body = jwsToken.getPayload();
        if (body.getExpiration().before(Date.from(Instant.now().plusMillis(Constants.DEFAULT_TOKEN_REFRESH_DELTA)))) {
            logger.info("Token qualifies for renewal! Renewing token...");
            AuthorizationToken renewedToken = this.authorizationService.reissueAuthorizationToken(token);
            response.setHeader("X-Token-Renewal", renewedToken.token());
        } else logger.info("Token does not qualify for renewal.");
    }

    @Getter
    private enum AuthorizationType {
        JWT ("BearerJWT"),
        ACCESS_KEY ("BearerAccessKey");

        private static final Map<String, AuthorizationType> map = new HashMap<>(values().length, 1);

        static {
            for (AuthorizationType authorizationType : values()) map.put(authorizationType.getType(), authorizationType);
        }

        private final String type;

        AuthorizationType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.getType();
        }

        @Nullable
        public static AuthorizationType of(String type) {
            return map.get(type);
        }
    }
}
