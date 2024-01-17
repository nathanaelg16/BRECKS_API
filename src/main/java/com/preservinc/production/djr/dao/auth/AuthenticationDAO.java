package com.preservinc.production.djr.dao.auth;

import com.preservinc.production.djr.exception.DatabaseException;
import com.preservinc.production.djr.model.auth.SignInMethod;
import com.preservinc.production.djr.model.auth.User;
import com.preservinc.production.djr.model.auth.UserStatus;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class AuthenticationDAO implements IAuthenticationDAO {
    private final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public AuthenticationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User findUser(@NonNull String username) throws SQLException {
        logger.info("[Auth DAO] Finding user with username: {}", username);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select id, first_name, last_name, display_name, email, " +
                     "username, password, salt, status from Employees where username = ?;")
        ) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return new User(
                        r.getInt("id"),
                        r.getString("first_name"),
                        r.getString("last_name"),
                        r.getString("display_name"),
                        r.getString("email"),
                        r.getString("username"),
                        r.getString("password"),
                        r.getString("salt"),
                        UserStatus.valueOf(r.getString("status"))
                    );
                else return null;
            }
        }
    }

    @Override
    public void loginAttempt(@NonNull Integer userID, @NonNull SignInMethod method, boolean success, String reason) throws SQLException {
        logger.info("[Auth DAO] Marking a {} login attempt for user ID `{}` using method: {}",
                success ? "SUCCESSFUL" : "FAILED", userID, method);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("insert into SignInEvents (user_id, method, successful, reason) value (?, ?, ?, ?);")
        ) {
            p.setInt(1, userID);
            p.setString(2, method.getMethod());
            p.setBoolean(3, success);
            p.setString(4, reason);
            p.executeUpdate();
        }
    }

    @Override
    public boolean registerUser(@NonNull String email, @NonNull String username, @NonNull String displayName,
                                @NonNull String password, @NonNull String salt) throws SQLException {
        logger.info("[Auth DAO] Attempting to register user with email: {}", email);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("update Employees set display_name = ?, username = ?, " +
                     "password = ?, salt = ?, registration_ts = CURRENT_TIMESTAMP() where email = ?;")
        ) {
            p.setString(1, displayName);
            p.setString(2, username);
            p.setString(3, password);
            p.setString(4, salt);
            p.setString(5, email);
            return p.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.info("[Auth DAO] Integrity constraint violation: {}", e.getMessage());
            logger.info(e);
        }

        return false;
    }

    @Override
    public void setPassword(@NonNull String user, @NonNull String hash, @NonNull String salt) throws SQLException {
        logger.info("[Auth DAO] Setting password for user: {}", user);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("update Employees set password = ?, salt = ? where username = ?;")
        ) {
            p.setString(1, hash);
            p.setString(2, salt);
            p.setString(3, user);
            p.executeUpdate();
        }
    }

    @Override
    public void unlockAccount(@NonNull String user) throws SQLException {
        logger.info("[Auth DAO] Unlocking account for user: {}", user);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("update Employees set status = ?, " +
                     "failed_attempts = 0 where username = ?;")
        ) {
            p.setString(1, UserStatus.ACTIVE.name());
            p.setString(2, user);
            p.executeUpdate();
        }
    }

    @Override
    public User findUserByEmail(@NonNull String email) throws SQLException {
        logger.info("[Auth DAO] Finding user with email: {}", email);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select id, first_name, last_name, display_name, email, " +
                     "username, password, salt, status from Employees where email = ?;")
        ) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return new User(
                        r.getInt("id"),
                        r.getString("first_name"),
                        r.getString("last_name"),
                        r.getString("display_name"),
                        r.getString("email"),
                        r.getString("username"),
                        r.getString("password"),
                        r.getString("salt"),
                        UserStatus.valueOf(r.getString("status"))
                );
                else return null;
            }
        }
    }

    @Override
    public boolean checkForUserRegistration(@NotNull String email) throws SQLException {
        logger.info("[Auth DAO] Checking if user is registered: {}", email);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select count(*) from Employees where email = ? and registration_ts IS NOT NULL;")
        ) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getBoolean(1);
                else throw new DatabaseException();
            }
        }
    }

    @Override
    public boolean isUsernameUnique(@NotNull String username) throws SQLException {
        logger.info("[Auth DAO] Checking if username `{}` is unique", username);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select count(*) from Employees where username = ?;")
        ) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return !r.getBoolean(1);
                else throw new DatabaseException();
            }
        }
    }
}
