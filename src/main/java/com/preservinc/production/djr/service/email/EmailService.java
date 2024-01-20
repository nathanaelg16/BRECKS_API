package com.preservinc.production.djr.service.email;

import com.preservinc.production.djr.auth.accesskey.AccessKeyManager;
import com.preservinc.production.djr.auth.accesskey.KeyDuration;
import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.exception.auth.AccessKeyException;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.report.Report;
import com.preservinc.production.djr.model.job.Job;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    private final Environment env;
    private final AccessKeyManager accessKeyManager;

    @Autowired
    public EmailService(Environment env, Session session, IReportsDAO reportsDAO, AccessKeyManager accessKeyManager) {
        this.env = env;
        this.session = session;
        this.reportsDAO = reportsDAO;
        this.accessKeyManager = accessKeyManager;
    }

    public void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException {
        logger.info("[Email Service] Sending report to report admins...");

        MimeBodyPart body = new MimeBodyPart();
        String email = loadTemplate("templates/email/new_report_email.html", "templates/email/styles.css")
                .replace("{{ADDRESS}}", job.address())
                .replace("{{AUTHOR}}", author.displayName())
                .replace("{{DATE}}", reportDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .replace("{{JOB_ID}}", String.valueOf(job.id()))
                .replace("{{WEB_APP_HOST}}", Objects.requireNonNull(env.getProperty("webapp.host")));
        body.setContent(email, "text/html; charset=utf-8");

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.attachFile(report);

        Envelope.Builder(this.session)
                .subject(String.format("%s - Daily Job Report", job.address()))
                .replyTo(author.email())
                .sendTo(reportsDAO.getEmailsForReportAdmins().toArray(String[]::new))
                .withPart(attachment)
                .withPart(body)
                .send();

        logger.info("[Email Service] Report sent!");
    }

    @Override
    public void sendReportSubmissionNotification(Report report, Job job) throws SQLException, IOException, MessagingException {
        logger.info("[Email Service] Sending report submission notification to report admins...");

        MimeBodyPart body = new MimeBodyPart();
        String email = loadTemplate("templates/email/new_report_notification_email.html", "templates/email/styles.css")
                .replace("{{ADDRESS}}", job.address())
                .replace("{{AUTHOR}}", report.getReportBy().displayName())
                .replace("{{DATE}}", report.getReportDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .replace("{{JOB_ID}}", String.valueOf(job.id()))
                .replace("{{REPORT_DATE_ID}}", report.getReportDate().format(DateTimeFormatter.ofPattern("yyMMdd")))
                .replace("{{WEB_APP_HOST}}", Objects.requireNonNull(env.getProperty("webapp.host")));
        body.setContent(email, "text/html; charset=utf-8");

        Envelope.Builder(this.session)
                .subject(String.format("%s - Daily Job Report", job.address()))
                .replyTo(report.getReportBy().email())
                .sendTo(reportsDAO.getEmailsForReportAdmins().toArray(String[]::new))
                .CC(report.getReportBy().email())
                .withPart(body)
                .send();

        logger.info("[Email Service] Report submission notification sent!");
    }

    public void notifySysAdmin(Throwable ex) {
        logger.info("[Email Service] Notifying SysAdmin of exception: {}", ex.getMessage());

        try {
            MimeBodyPart body = new MimeBodyPart();
            String email = loadTemplate("templates/email/error_notification_email.html", "templates/email/styles.css")
                    .replace("{{EXCEPTION}}", ex.toString())
                    .replace("{{STACK_TRACE}}", ExceptionUtils.getStackTrace(ex))
                    .replace("{{WEB_APP_HOST}}", Objects.requireNonNull(env.getProperty("webapp.host")));
            body.setContent(email, "text/html; charset=utf-8");

            Envelope.Builder(this.session)
                    .subject("Exception Notification")
                    .replyTo("noreply@brecks.app")
                    .sendTo(Objects.requireNonNull(env.getProperty("sysadmin.email")))
                    .withPart(body)
                    .send();

            logger.info("[Email Service] Error notification sent!");
        } catch (Exception e) {
            logger.info("[Email Service] An exception occurred while attempting to notify SysAdmin...");
            logger.error(e);
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public void notifyAccountCreation(@NonNull String email) throws MessagingException, IOException {
        logger.info("[Email Service] Notifying user with email `{}` that they have been added to the platform...", email);

        String accessKey = "";

        try {
            accessKey = this.accessKeyManager.createAccessKey(email, KeyDuration.LONG);
        } catch (AccessKeyException e) {
            this.notifySysAdmin(e);
        }

        MimeBodyPart body = new MimeBodyPart();
        String emailTemplate = loadTemplate("templates/email/account_creation_notification_email.html", "templates/email/styles.css")
                .replace("{{WEB_APP_HOST}}", "")
                .replace("{{ACCESS_KEY}}", accessKey)
                .replace("{{LINK}}", Objects.requireNonNull(env.getProperty("webapp.host")));
        body.setContent(emailTemplate, "text/html; charset=utf-8");

        Envelope.Builder(this.session)
                .subject("Welcome to BRECKS")
                .replyTo("noreply@brecks.app")
                .sendTo(email)
                .withPart(body)
                .send();

        logger.info("[Email Service] Notification sent!");
    }

    @Override
    public void sendPasswordResetEmail(@NonNull String email) {
        // todo implement method
    }

    private String loadTemplate(@NonNull String pathToEmailTemplate, @NonNull String pathToCSSTemplate) throws IOException {
        return new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(pathToEmailTemplate)).readAllBytes(), StandardCharsets.UTF_8)
                .replace("{{CSS_RESETS", new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("templates/email/resets.css")).readAllBytes(), StandardCharsets.UTF_8))
                .replace("{{CSS_STYLES}}", new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(pathToCSSTemplate)).readAllBytes(), StandardCharsets.UTF_8));
    }

    static final class Envelope {

        public static Envelope Builder(@NonNull Session session) throws MessagingException {
            return new Envelope(session);
        }

        private final MimeMessage message;
        private final Multipart multipart;

        private Envelope(@NonNull Session session) throws MessagingException {
            this.multipart = new MimeMultipart();
            this.message = new MimeMessage(session);
            this.message.setFrom(new InternetAddress(session.getProperty("mail.from")));
        }

        public Envelope subject(@NonNull String subject) throws MessagingException {
            this.message.setSubject(subject);
            return this;
        }

        public Envelope withPart(@NonNull MimeBodyPart part) throws MessagingException {
            this.multipart.addBodyPart(part);
            return this;
        }

        public Envelope replyTo(@NonNull String replyTo) throws MessagingException {
            this.message.setReplyTo(InternetAddress.parse(replyTo));
            return this;
        }

        public Envelope sendTo(@NonNull String... recipients) throws MessagingException {
            this.message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
            return this;
        }

        public Envelope CC(@NonNull String... recipients) throws MessagingException {
            this.message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(String.join(",", recipients)));
            return this;
        }

        public Envelope BCC(@NonNull String... recipients) throws MessagingException {
            this.message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(String.join(",", recipients)));
            return this;
        }

        public void send() throws MessagingException {
            if (message.getAllRecipients().length > 0) {
                message.setContent(this.multipart);
                message.setSentDate(Date.from(Instant.now()));
                Transport.send(message);
            } else throw new MessagingException("Recipients list is empty!");
        }
    }
}

// TODO implement caching of report admins