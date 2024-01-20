package com.preservinc.production.djr.auth.accesskey;

import com.preservinc.production.djr.exception.auth.AccessKeyException;
import lombok.NonNull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;

public final class AccessKey {
    private static final Logger logger = LogManager.getLogger();
    private static final RandomStringGenerator SALT_GENERATOR;
    private final String email;
    private final KeyDuration duration;
    private final HashSet<String> scope;
    private final LocalDateTime expiryTime;
    private final String hash;

    static {
        SALT_GENERATOR = new RandomStringGenerator.Builder()
                .filteredBy(CharacterPredicates.ASCII_ALPHA_NUMERALS)
                .build();
    }

    AccessKey(@NonNull String email, @NonNull KeyDuration duration, @NonNull HashSet<String> scope) throws AccessKeyException {
        this.email = email;
        this.duration = duration;
        this.scope = scope;
        this.expiryTime = LocalDateTime.now(ZoneId.of("America/New_York")).plus(duration.getDuration());
        this.hash = hash(salt(this.email, this.expiryTime.toString()));
    }

    public String email() {
        return email;
    }

    public KeyDuration duration() {
        return duration;
    }

    public HashSet<String> scope() {
        return scope;
    }

    public LocalDateTime expiryTime() {
        return expiryTime;
    }

    String hash() {
        return hash;
    }

    private static String salt(@NonNull String... values) {
        return String.join(":", values).concat(":%s".formatted(SALT_GENERATOR.generate(8, 20)));
    }

    private static String hash(@NonNull String value) throws AccessKeyException {
        try {
            return new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException e) {
            logger.error("[Access Key] No hash algorithm with given name found. Exception: {}", e.getMessage());
            logger.error(e);
            throw new AccessKeyException(e);
        }
    }

    boolean isValid() {
        return this.expiryTime.isAfter(LocalDateTime.now(ZoneId.of("America/New_York")));
    }

    boolean withinScope(String endpoint) {
        return this.scope.stream().anyMatch((scope) -> {
            if (scope.contains("*")) {
                String[] scopeTree = scope.split("/");
                String[] endpointTree = endpoint.split("/");
                if (endpointTree.length < scopeTree.length) return false;
                if (endpointTree.length != scopeTree.length && !scopeTree[scopeTree.length - 1].equals("*")) return false;
                for (int i = 0; i < scopeTree.length; i++) {
                    if (!scopeTree[i].equals("*") && !scopeTree[i].equals(endpointTree[i])) return false;
                }
                return true;
            } else return scope.equals(endpoint);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AccessKey accessKey = (AccessKey) o;

        return new EqualsBuilder().append(hash, accessKey.hash).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(hash).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("email", email)
                .append("duration", duration)
                .append("scope", scope)
                .append("expiryTime", expiryTime)
                .toString();
    }
}
