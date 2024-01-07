package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.job.Job;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.util.Objects;

@Service
public class EmailService implements IEmailService {
    private static final Logger logger = LogManager.getLogger();
    private final Session session;
    private final IReportsDAO reportsDAO;

    @Value("${webapp.host}")
    private String WEB_APP_HOST;

    @Value("sysadmin.email")
    private String SYSADMIN_EMAIL;

    @Autowired
    public EmailService(Session session, IReportsDAO reportsDAO) {
        this.session = session;
        this.reportsDAO = reportsDAO;
    }

    public void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException {
        logger.info("[Email Service] Sending report to report admins...");

        MimeBodyPart body = new MimeBodyPart();
        String email = new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/email/new_report_email.html")).readAllBytes(), StandardCharsets.UTF_8)
                .replace("{{ADDRESS}}", job.address())
                .replace("{{AUTHOR}}", author.displayName())
                .replace("{{DATE}}", reportDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .replace("{{JOB_ID}}", String.valueOf(job.id()))
                .replace("{{WEB_APP_HOST}}", WEB_APP_HOST);
        body.setContent(email, "text/html; charset=utf-8");

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.attachFile(report);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(body);
        multipart.addBodyPart(attachment);

        MimeMessage message = prepareEnvelope(String.format("%s - Daily Job Report", job.address()),
                author.email(), reportsDAO.getEmailsForReportAdmins().toArray(String[]::new));
        message.setContent(multipart);
        message.setSentDate(Date.from(Instant.now()));
        Transport.send(message);

        logger.info("[Email Service] Report sent!");
    }

    @Override
    public void sendReportSubmissionNotification(Report report, Job job) throws SQLException, IOException, MessagingException {
        logger.info("[Email Service] Sending report submission notification to report admins...");

        MimeBodyPart body = new MimeBodyPart();
        String email = new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/email/new_report_notification_email.html")).readAllBytes(), StandardCharsets.UTF_8)
                .replace("{{ADDRESS}}", job.address())
                .replace("{{AUTHOR}}", report.getReportBy().displayName())
                .replace("{{DATE}}", report.getReportDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .replace("{{JOB_ID}}", String.valueOf(job.id()))
                .replace("{{REPORT_DATE_ID}}", report.getReportDate().format(DateTimeFormatter.ofPattern("yyMMdd")))
                .replace("{{WEB_APP_HOST}}", WEB_APP_HOST);
        body.setContent(email, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(body);

        MimeMessage message = prepareEnvelope(String.format("%s - Daily Job Report", job.address()),
                report.getReportBy().email(), reportsDAO.getEmailsForReportAdmins().toArray(String[]::new));
        message.setContent(multipart);
        message.setSentDate(Date.from(Instant.now()));
        Transport.send(message);

        logger.info("[Email Service] Report submission notification sent!");
    }

    public void notifySysAdmin(Throwable ex) {
        logger.info("[Email Service] Notifying SysAdmin of exception: {}", ex.getMessage());

        try {
            MimeBodyPart body = new MimeBodyPart();
            String email = new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/email/error_notification_email.html")).readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{EXCEPTION}}", ex.toString())
                    .replace("{{STACK_TRACE}}", ExceptionUtils.getStackTrace(ex))
                    .replace("{{WEB_APP_HOST}}", WEB_APP_HOST);
            body.setContent(email, "text/html; charset=utf-8");
        } catch (Exception e) {
            logger.info("[Email Service] An exception occurred while attempting to notify SysAdmin...");
            logger.error(e);
        }
    }

    private MimeMessage prepareEnvelope(String subject, String replyTo, String... recipientEmails) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(session.getProperty("mail.from")));
        message.setReplyTo(InternetAddress.parse(replyTo));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipientEmails)));
        message.setRecipients(Message.RecipientType.CC, replyTo);
        message.setSubject(subject);
        return message;
    }
}

// TODO implement caching of report admins