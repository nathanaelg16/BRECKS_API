package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.Report;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService implements IEmailService {
    public void sendReportEmail(File report) {
        // TODO: Implement this
    }

    @Override
    public void sendReportSubmissionNotification(Report report) {
        // TODO: Implement this
    }

    public void notifySysAdmin(Throwable ex) {
        // TODO: Implement this
    }
}
