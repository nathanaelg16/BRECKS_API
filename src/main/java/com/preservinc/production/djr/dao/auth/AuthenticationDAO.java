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
             PreparedStatement p = c.prepareStatement("select E.id, E.first_name, E.last_name, E.display_name, E.email, " +
                     "EUA.username, EUA.password, EUA.salt, EUA.status from Employees E " +
                     "inner join EmployeeUserAccounts EUA on E.id = EUA.id where EUA.username = ?;")
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
             PreparedStatement p = c.prepareStatement("insert into SignInEvents " +
                     "(user_id, method, successful, reason) value (?, ?, ?, ?);")
        ) {
            p.setInt(1, userID);
            p.setString(2, method.getMethod());
            p.setBoolean(3, success);
            p.setString(4, reason);
            p.executeUpdate();
        }
    }

    @Override
    public void registerUser(@NonNull String email, @NonNull String username, @NonNull String displayName,
                                @NonNull String password, @NonNull String salt) throws SQLException {
        logger.info("[Auth DAO] Attempting to register user with email: {}", email);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("insert into EmployeeUserAccounts " +
                     "(id, username, password, salt, status) select EmployeeID.id, ?, ?, ?, ? " +
                     "from (select id from Employees where email = ?) as EmployeeID;")
        ) {
            p1.setString(1, username);
            p1.setString(2, password);
            p1.setString(3, salt);
            p1.setString(4, UserStatus.ACTIVE.name());
            p1.setString(5, email);
            p1.executeUpdate();

            try (PreparedStatement p2 = c.prepareStatement("update Employees set display_name = ? where email = ?")) {
                p2.setString(1, displayName);
                p2.setString(2, email);
                p2.executeUpdate();
            } catch (SQLException e) {
                // err silently
                logger.error("[Auth DAO] Error occurred setting display_name during user registration: {}", e.getMessage());
                logger.error(e);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            logger.info("[Auth DAO] Integrity constraint violation: {}", e.getMessage());
            logger.info(e);
            throw e;
        }
    }

    @Override
    public void setPassword(@NonNull String user, @NonNull String hash, @NonNull String salt) throws SQLException {
        logger.info("[Auth DAO] Setting password for user: {}", user);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("update EmployeeUserAccounts set password = ?, salt = ? where username = ?;")
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
             PreparedStatement p = c.prepareStatement("update EmployeeUserAccounts set status = ?, " +
                     "failed_attempts = 0 where username = ? and status <> 'INACTIVE';")
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
             PreparedStatement p = c.prepareStatement("select E.id, E.first_name, E.last_name, E.display_name, E.email, " +
                     "EUA.username, EUA.password, EUA.salt, EUA.status from Employees E " +
                     "inner join EmployeeUserAccounts EUA on E.id = EUA.id where E.email = ?;")
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
             PreparedStatement p = c.prepareStatement("select count(*) from EmployeeUserAccounts EUA " +
                     "inner join Employees E on EUA.id = E.id where E.email = ? and EUA.registration_ts IS NOT NULL;")
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
             PreparedStatement p = c.prepareStatement("select count(*) from EmployeeUserAccounts where username = ?;")
        ) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return !r.getBoolean(1);
                else throw new DatabaseException();
            }
        }
    }
}
