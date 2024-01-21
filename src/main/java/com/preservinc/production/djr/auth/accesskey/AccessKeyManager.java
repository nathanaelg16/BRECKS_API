package com.preservinc.production.djr.auth.accesskey;

import com.preservinc.production.djr.exception.auth.AccessKeyException;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AccessKeyManager {
    private static final Logger logger = LogManager.getLogger();
    private final Map<String, AccessKey> accessKeys;

    public AccessKeyManager() {
        this.accessKeys = new HashMap<>();
    }

    // todo define enum of allowed scopes

    public String createAccessKey(@NonNull String email, @NonNull KeyDuration duration, @NonNull String... scopes) throws AccessKeyException {
        return createAccessKey(email, duration, Set.of(scopes));
    }

    public String createAccessKey(@NonNull String email, @NonNull KeyDuration duration, @NonNull Collection<String> scopes) throws AccessKeyException {
        logger.info("[Access Key Manager] Creating a new access key for user `{}` with duration `{}` and scopes: {}", email, duration.name(), scopes);
        AccessKey accessKey = new AccessKey(email, duration, new HashSet<>(scopes));
        this.accessKeys.put(accessKey.hash(), accessKey);
        return accessKey.hash();
    }
    public String renewAccessKey(AccessKey accessKey) throws AccessKeyException {
        logger.info("[Access Key Manager] Renewing access key from key: {}", accessKey);
        return createAccessKey(accessKey.email(), accessKey.duration(), accessKey.scope());
    }

    public AccessKey verifyAccessKey(@NonNull String accessKey, @NonNull String endpoint) {
        logger.info("[Access Key Manager] Verifying access key `{}`", accessKey);

        AccessKey key = this.accessKeys.get(accessKey);

        if (key == null) {
            logger.info("[Access Key Manager] Access key not found. May be malformed or expired.");
            return null;
        }

        logger.info("[Access Key Manager] Access key belongs to: {}", key.email());
        logger.info("[Access Key Manager] Expires: {}", key.expiryTime());
        logger.info("[Access Key Manager] Scope: {}", key.scope());

        if (key.isValid() && key.withinScope(endpoint)) return key;
        else return null;
    }

    @Scheduled(fixedDelay = 1_200_000)
    private void removeInvalidKeys() {
        this.accessKeys.entrySet()
                .stream()
                .filter((entry) -> !entry.getValue().isValid())
                .forEach((entry) -> this.accessKeys.remove(entry.getKey()));
    }
}
