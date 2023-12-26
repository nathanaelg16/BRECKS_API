package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.Report;

import java.io.File;

public interface IEmailService {
    void sendReportEmail(File report);
    void sendReportSubmissionNotification(Report report);
    void notifySysAdmin(Throwable ex);
}
