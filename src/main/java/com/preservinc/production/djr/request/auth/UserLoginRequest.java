package com.preservinc.production.djr.request.auth;

import com.preservinc.production.djr.request.Request;
import lombok.NonNull;

public record UserLoginRequest(@NonNull String username, @NonNull String password) implements Request {
    @Override
    public boolean isWellFormed() {
        return (!username.isBlank() && !password.isBlank());
    }
}
