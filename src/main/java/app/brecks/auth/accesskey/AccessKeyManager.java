package app.brecks.auth.accesskey;

import app.brecks.exception.auth.AccessKeyException;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
public class AccessKeyManager {
    private static final Logger logger = LogManager.getLogger();
    private final AccessKeyRepository accessKeys;

    @Autowired
    public AccessKeyManager(AccessKeyRepository accessKeyRepository) {
        this.accessKeys = accessKeyRepository;
    }

    // todo define enum of allowed scopes

    public String createAccessKey(@NonNull String email, @NonNull KeyDuration duration, @NonNull String... scopes) throws AccessKeyException {
        return createAccessKey(email, duration, Set.of(scopes));
    }

    public String createAccessKey(@NonNull String email, @NonNull KeyDuration duration, @NonNull Collection<String> scopes) throws AccessKeyException {
        logger.info("Creating a new access key for user `{}` with duration `{}` and scopes: {}", email, duration.name(), scopes);
        AccessKey accessKey = AccessKey.create(email, duration, new HashSet<>(scopes));
        this.accessKeys.put(accessKey.hash(), accessKey);
        return accessKey.hash();
    }

    public String renewAccessKey(AccessKey accessKey) throws AccessKeyException {
        logger.info("Renewing access key from key: {}", accessKey);
        return createAccessKey(accessKey.email(), accessKey.duration(), accessKey.scope());
    }

    public AccessKey verifyAccessKey(@NonNull String accessKey, @NonNull String endpoint) {
        logger.info("Verifying access key `{}`", accessKey);

        AccessKey key = this.accessKeys.get(accessKey);

        if (key == null) {
            logger.info("Access key not found. May be malformed or expired.");
            return null;
        }

        logger.info("Access key belongs to: {}", key.email());
        logger.info("Expires: {}", key.expiryTime());
        logger.info("Scope: {}", key.scope());

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

    public void revokeAccessKey(@NotNull AccessKey accessKey) {
        logger.info("[Access Key Manager] Revoking access key: {}", accessKey);
        this.accessKeys.remove(accessKey.hash());
    }
}
