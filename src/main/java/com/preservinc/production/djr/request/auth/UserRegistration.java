package com.preservinc.production.djr.request.auth;

import com.preservinc.production.djr.request.Request;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
public class UserRegistration implements Request {
    private String displayName;
    private String email;
    private String username;

    @Setter
    private String password;

    public UserRegistration() {

    }

    public UserRegistration(String displayName, String email, String username, String password) {
        this.displayName = displayName;
        this.setEmail(email);
        this.setUsername(username);
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }

    public void setEmail(String email) {
        this.email = email.toLowerCase();
    }

    @Override
    public boolean isWellFormed() {
        return BooleanUtils.and(new boolean[]{
                this.displayName != null && !this.displayName.isBlank(),
                this.username != null && !this.username.isBlank(),
                StringUtils.isAlphanumeric(username),
                this.password != null && !this.password.isBlank(),
                this.email != null && !this.email.isBlank()
        });
    }
}
