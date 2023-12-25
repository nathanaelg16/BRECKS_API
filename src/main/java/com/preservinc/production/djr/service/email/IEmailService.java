package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.Report;

public interface IEmailService {
    void sendReportEmail(Report report);
    void notifyAdmin(Throwable ex);
}
