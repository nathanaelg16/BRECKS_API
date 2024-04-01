package app.brecks.service.admin;

import app.brecks.dao.admin.IAdminDAO;
import app.brecks.dao.auth.IAuthenticationDAO;
import app.brecks.request.auth.UserLoginRequest;
import app.brecks.service.authorization.IAuthorizationService;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class AdminService implements IAdminService {
    private static final Logger logger = LogManager.getLogger();
    private final IAuthorizationService authorizationService;
    private final IAdminDAO adminDAO;
    private final IAuthenticationDAO authenticationDAO;

    @Autowired
    public AdminService(IAdminDAO adminDAO, IAuthorizationService authorizationService, IAuthenticationDAO authenticationDAO) {
        this.adminDAO = adminDAO;
        this.authorizationService = authorizationService;
        this.authenticationDAO = authenticationDAO;
    }

    @Override
    public void unlockUser(@NonNull String username) {
        logger.traceEntry("unlockUser(username={})", username);
        CompletableFuture.runAsync(() -> {
            try {
                this.authenticationDAO.unlockAccount(username);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public void changePassword(@NonNull UserLoginRequest passwordChangeRequest) {
        logger.traceEntry("resetPassword(userLoginRequest={})", passwordChangeRequest.username());
        this.authorizationService.setPassword(passwordChangeRequest.username(), passwordChangeRequest.password());
    }
}
