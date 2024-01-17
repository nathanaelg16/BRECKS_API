package com.preservinc.production.djr.auth;

import com.preservinc.production.djr.dao.auth.IAuthenticationDAO;
import lombok.NonNull;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

@Component
public class AccessKeyManager {
    private static final Logger logger = LogManager.getLogger();
    private final IAuthenticationDAO authenticationDAO;
    private final MultiValuedMap<String, AccessKey> accessKeys;
    private final RandomStringGenerator saltGenerator;

    @Autowired
    public AccessKeyManager(IAuthenticationDAO authenticationDAO) {
        this.authenticationDAO = authenticationDAO;
        this.accessKeys = new HashSetValuedHashMap<>();
        this.saltGenerator = new RandomStringGenerator.Builder().filteredBy(CharacterPredicates.ASCII_ALPHA_NUMERALS).build();
    }

    public String createAccessKey(@NonNull String email, @NonNull KeyDuration duration) throws Exception {
        logger.info("[Access Key Manager] Creating a new access key for user `{}` with duration: {}", email, duration.name());

        String salt = saltGenerator.generate(8, 20);
        String hash = hash(email, salt);
        LocalDateTime expiryTime = LocalDateTime.now(ZoneId.of("America/New_York")).plus(duration.getDuration());

        this.accessKeys.put(email, new AccessKey(hash, salt, expiryTime));

        return hash;
    }

    public boolean verifyAccessKey(@NonNull String email, @NonNull String accessKey) {
        logger.info("[Access Key Manager] Verifying access key `{}` for user `{}`", accessKey, email);
        Collection<AccessKey> keys = this.accessKeys.get(email);
        // todo finish this
    }

    private String hash(@NonNull String value, @NonNull String salt) throws Exception {
        try {
            return new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(salt(value, salt).getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException e) {
            logger.error("[Access Key Manager] No hash algorithm with given name found. Exception: {}", e.getMessage());
            logger.error(e);
            throw new Exception();
        }
    }

    private String salt(@NonNull String value, @NonNull String salt) {
        // todo implement
    }

    record AccessKey (@NonNull String hash, @NonNull String salt, @NonNull LocalDateTime expiryTime) {}
}
