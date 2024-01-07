package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.job.Job;
import jakarta.mail.MessagingException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public interface IEmailService {
    void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException;
    void sendReportSubmissionNotification(Report report);
    void notifySysAdmin(Throwable ex);
}
