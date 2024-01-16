package com.preservinc.production.djr.dao.auth;

import com.preservinc.production.djr.model.auth.User;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface IAuthenticationDAO {
    User findUser(@NonNull String username) throws SQLException;

    void loginAttempt(@NonNull String username, boolean b) throws SQLException;

    boolean registerUser(@NonNull String email, @NonNull String username, @NonNull String displayName, @NonNull String password, @NonNull String salt) throws SQLException;

    void setPassword(@NonNull String user, @NonNull String hash, @NonNull String salt) throws SQLException;

    void unlockAccount(@NonNull String user) throws SQLException;

    User findUserByEmail(@NonNull String email) throws SQLException;

    boolean checkForUserRegistration(@NonNull String email) throws SQLException;

    boolean isUsernameUnique(@NotNull String username) throws SQLException;
}
