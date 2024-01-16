package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.report.Report;
import com.preservinc.production.djr.model.job.Job;
import jakarta.mail.MessagingException;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public interface IEmailService {
    void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException;
    void sendReportSubmissionNotification(Report report, Job job) throws SQLException, IOException, MessagingException;
    void notifySysAdmin(Throwable ex);
    void sendPasswordResetEmail(@NonNull String email);
}
