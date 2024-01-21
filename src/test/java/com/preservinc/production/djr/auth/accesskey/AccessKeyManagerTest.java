package com.preservinc.production.djr.auth.accesskey;

import com.preservinc.production.djr.dao.auth.IAuthenticationDAO;
import com.preservinc.production.djr.model.auth.SignInMethod;
import com.preservinc.production.djr.model.auth.User;
import com.preservinc.production.djr.model.auth.UserStatus;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AccessKeyManagerTest {

    private static final Logger logger = LogManager.getLogger();
    private AccessKeyManager accessKeyManager;
    private IAuthenticationDAO authenticationDAO;

    @BeforeEach
    void setUp() {
        this.accessKeyManager = new AccessKeyManager();
    }

    @Test
    void testAccessKeyGeneration() {
        assertDoesNotThrow(() -> {
            User user = authenticationDAO.findUser("bmarley");
            String accessKey = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/registration");
            assertNotNull(accessKey);
            logger.info("Access key: {}", accessKey);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey, "/registration"));
            assertNull(this.accessKeyManager.verifyAccessKey(accessKey, "/registration"));
        });
    }

    @Test
    void testEndpointCheck() {
        assertDoesNotThrow(() -> {
            User user = authenticationDAO.findUser("bmarley");
            String accessKey = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/registration");
            assertNotNull(accessKey);
            logger.info("Access key: {}", accessKey);
            assertNull(this.accessKeyManager.verifyAccessKey(accessKey, "/registration/test"));

            String accessKey2 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/registration/*");
            assertNotNull(accessKey2);
            logger.info("Access key: {}", accessKey2);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey2, "/registration/test"));

            String accessKey3 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/registration/*");
            assertNotNull(accessKey3);
            logger.info("Access key: {}", accessKey3);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey3, "/registration/test/alpha"));

            String accessKey4 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/*");
            assertNotNull(accessKey4);
            logger.info("Access key: {}", accessKey4);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey4, "/registration/test/alpha"));

            String accessKey5 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/*/test");
            assertNotNull(accessKey5);
            logger.info("Access key: {}", accessKey5);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey5, "/registration/test"));

            String accessKey6 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/*/test");
            assertNotNull(accessKey6);
            logger.info("Access key: {}", accessKey6);
            assertNull(this.accessKeyManager.verifyAccessKey(accessKey6, "/registration/test/alpha"));

            String accessKey7 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/*/test/*");
            assertNotNull(accessKey7);
            logger.info("Access key: {}", accessKey7);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey7, "/registration/test/alpha"));

            String accessKey8 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/test/*/registration");
            assertNotNull(accessKey8);
            logger.info("Access key: {}", accessKey8);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey8, "/test/registration/registration"));

            String accessKey9 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/test/active/registration");
            assertNotNull(accessKey9);
            logger.info("Access key: {}", accessKey9);
            assertNotNull(this.accessKeyManager.verifyAccessKey(accessKey9, "/test/active/registration"));

            String accessKey10 = this.accessKeyManager.createAccessKey(user.email(), KeyDuration.LONG, "/test/active/registration");
            assertNotNull(accessKey10);
            logger.info("Access key: {}", accessKey10);
            assertNull(this.accessKeyManager.verifyAccessKey(accessKey10, "/test/active"));
        });
    }

    static class MockAuthenticationDAO implements IAuthenticationDAO {
        private static final Logger logger = LogManager.getLogger();

        @Override
        public User findUser(@NonNull String username) throws SQLException {
            logger.info("[Mock Auth Dao] Finding user");
            return new User(1, "Robert", "Marley", "Bob",
                    "bmarley@aol.com", "bmarley", "adsfghjk", "sdfg", UserStatus.ACTIVE);
        }

        @Override
        public void loginAttempt(@NonNull Integer userID, @NonNull SignInMethod method, boolean success, String reason) throws SQLException {
            logger.info("[Mock auth dao] login attempt registered");
            logger.info("[Mock Auth Dao] Login attempt for user `{}` via method `{}` {} {} `{}`", userID, method.getMethod(), success ? "SUCCEEDED" : "FAILED", !success ? "with reason" : null, !success ? reason : null);
        }

        @Override
        public void registerUser(@NonNull String email, @NonNull String username, @NonNull String displayName, @NonNull String password, @NonNull String salt) throws SQLException {
            logger.info("[Mock auth dao] user registered registered");
        }

        @Override
        public void setPassword(@NonNull String user, @NonNull String hash, @NonNull String salt) throws SQLException {
            logger.info("[Mock auth dao] password registered");
        }

        @Override
        public void unlockAccount(@NonNull String user) throws SQLException {
            logger.info("[Mock auth dao] unlock account registered");
        }

        @Override
        public User findUserByEmail(@NonNull String email) throws SQLException {
            logger.info("[Mock Auth Dao] Finding user w/ email");
            return new User(1, "Robert", "Marley", "Bob",
                    "bmarley@aol.com", "bmarley", "adsfghjk", "sdfg", UserStatus.ACTIVE);
        }

        @Override
        public boolean checkForUserRegistration(@NonNull String email) throws SQLException {
            return true;
        }

        @Override
        public boolean isUsernameUnique(@NotNull String username) throws SQLException {
            return true;
        }
    }
}
