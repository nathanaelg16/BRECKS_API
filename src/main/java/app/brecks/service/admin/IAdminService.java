package app.brecks.service.admin;

import app.brecks.request.auth.UserLoginRequest;
import lombok.NonNull;

public interface IAdminService {
    void unlockUser(@NonNull String username);

    void changePassword(@NonNull UserLoginRequest passwordChangeRequest);
}
