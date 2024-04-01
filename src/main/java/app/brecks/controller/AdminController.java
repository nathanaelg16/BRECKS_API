package app.brecks.controller;

import app.brecks.request.auth.UserLoginRequest;
import app.brecks.service.admin.IAdminService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/admin")
public class AdminController {
    private static final Logger logger = LogManager.getLogger();

    private final IAdminService adminService;

    @Autowired
    public AdminController(IAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/unlock")
    public ResponseEntity<Void> unlockUser(@RequestParam String username) {
        logger.traceEntry("unlockUser(username={})", username);
        this.adminService.unlockUser(username);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/changePassword")
    public ResponseEntity<Void> changePassword(@RequestBody UserLoginRequest passwordChangeRequest) {
        logger.traceEntry("resetPassword(userLoginRequest={})", passwordChangeRequest.username());
        if (!passwordChangeRequest.isWellFormed()) return ResponseEntity.badRequest().build();
        this.adminService.changePassword(passwordChangeRequest);
        return ResponseEntity.ok().build();
    }
}
