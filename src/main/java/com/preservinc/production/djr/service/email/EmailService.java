package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.Report;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements IEmailService {
    public void sendReportEmail(Report report) {
        // TODO: Implement this
    }

    public void notifyAdmin(Throwable ex) {
        // TODO: Implement this
    }
}
