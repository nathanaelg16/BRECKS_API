package app.brecks.service.email;

import app.brecks.model.employee.Employee;
import app.brecks.model.report.Report;
import app.brecks.model.job.Job;
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
    void notifyAccountCreation(String email) throws MessagingException, IOException;
}
