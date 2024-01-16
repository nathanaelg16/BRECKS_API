package com.preservinc.production.djr.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.preservinc.production.djr.auth.AuthorizationToken;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Getter
@Accessors(fluent = true)
public final class RegistrationResponse {
    private static final Logger logger = LogManager.getLogger();
    private boolean validUsername;
    private boolean validPassword;
    private Boolean uniqueUsername;

    @Setter
    private String token;

    @JsonIgnore
    public boolean isValid() {
        return validUsername() && validPassword();
    }

    @JsonIgnore
    public boolean isUnique() {
        return uniqueUsername();
    }

    public void setValidUsername(boolean valid) {
        if (!valid) logger.info("[Registration Response] Username is blank or is not alphanumeric");
        this.validUsername = valid;
    }

    public void setValidPassword(boolean valid) {
        if (!valid) logger.info("[Registration Response] Password does not meet requirements");
        this.validPassword = valid;
    }

    public void setUniqueUsername(boolean uniqueUsername) {
        this.uniqueUsername = uniqueUsername;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RegistrationResponse) obj;
        return this.validUsername == that.validUsername &&
                this.validPassword == that.validPassword &&
                this.uniqueUsername == that.uniqueUsername;
    }

    @Override
    public int hashCode() {
        return Objects.hash(validUsername, validPassword, uniqueUsername);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("validUsername", validUsername)
                .append("validPassword", validPassword)
                .append("uniqueUsername", uniqueUsername)
                .append("token", token)
                .toString();
    }
}
