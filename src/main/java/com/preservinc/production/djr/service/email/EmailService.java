package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.job.Job;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class EmailService implements IEmailService {
    private static final Logger logger = LogManager.getLogger();
    private final Session session;
    private final IReportsDAO reportsDAO;

    @Value("${webapp.host}")
    private String WEB_APP_HOST;

    @Autowired
    public EmailService(Session session, IReportsDAO reportsDAO) {
        this.session = session;
        this.reportsDAO = reportsDAO;
    }

    public void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException {
        logger.info("[Email Service] Sending report to report admins...");

        List<String> emailsForReportAdmins = reportsDAO.getEmailsForReportAdmins();

        InternetAddress[] authorEmailAddress = InternetAddress.parse(author.email());

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(session.getProperty("mail.from")));
        message.setReplyTo(authorEmailAddress);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", emailsForReportAdmins)));
        message.setRecipients(Message.RecipientType.CC, authorEmailAddress);
        message.setSubject(String.format("%s - Daily Job Report", job.address()));

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.attachFile(report);

        MimeBodyPart body = new MimeBodyPart();
        String email = new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/email/new_report_email.html")).readAllBytes(), StandardCharsets.UTF_8)
                .replace("{{ADDRESS}}", job.address())
                .replace("{{AUTHOR}}", author.displayName())
                .replace("{{DATE}}", reportDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .replace("{{JOB_ID}}", String.valueOf(job.id()))
                .replace("{{WEB_APP_HOST}}", WEB_APP_HOST);
        body.setContent(email, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(body);
        multipart.addBodyPart(attachment);

        message.setContent(multipart);
        message.setSentDate(Date.from(Instant.now()));
        Transport.send(message);

        logger.info("[Email Service] Report sent!");
    }

    @Override
    public void sendReportSubmissionNotification(Report report) {
        logger.info("[Email Service] Should be sending report submission notification here...");
    }

    public void notifySysAdmin(Throwable ex) {
        // TODO: Implement this
        logger.info("[Email Service] Should be notifying SysAdmin of error here...");
    }
}

// TODO implement caching of report admins