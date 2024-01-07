package com.preservinc.production.djr.interceptor;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private static final Logger logger = LogManager.getLogger();

    private final FirebaseAuth firebaseAuth;

    @Autowired
    public AuthenticationInterceptor(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        logger.info("[Auth Interceptor -- Pre-Handle] Checking request authentication at endpoint {}", request.getRequestURI());

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
            try {
                FirebaseToken firebaseAuthenticationToken = firebaseAuth.verifyIdToken(authorizationToken, true);
                if (firebaseAuthenticationToken == null) throw new IllegalArgumentException();
                logger.info("[Auth Interceptor -- Pre-Handle] Successfully verified authentication token");
                request.setAttribute("FirebaseToken", firebaseAuthenticationToken);
                return true;
            } catch (IllegalArgumentException | FirebaseAuthException e) {
                logger.info("[Auth Interceptor -- Pre-Handle] Verification of authentication token failed. Reason: '{}'", e.getMessage());
                response.sendError(401, "Invalid authentication token");
                return false;
            }
        } else {
            logger.info("Unsupported authentication type: {}", authorizationType);
            response.sendError(401, "Unsupported authentication type");
            return false;
        }
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
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
