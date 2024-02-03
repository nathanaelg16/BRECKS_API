package com.preservinc.production.djr.service;

import com.preservinc.production.djr.auth.accesskey.AccessKeyManager;
import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.employee.EmployeeStatus;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.model.report.Report;
import com.preservinc.production.djr.model.team.Team;
import com.preservinc.production.djr.service.email.EmailService;
import jakarta.mail.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EmailServiceTest {
    private static final Logger logger = LogManager.getLogger();
    private EmailService emailService;

    @Autowired
    private Environment env;

    @Autowired
    private Session session;

    @Autowired
    private AccessKeyManager accessKeyManager;

    static class ReportsDAOMock implements IReportsDAO {

        @Override
        public void saveReport(Report report) throws SQLException {
            logger.info("Saving report {}", report);
        }

        @Override
        public List<String> getEmailsForReportAdmins() throws SQLException {
            List<String> emails = new ArrayList<>();
            emails.add("ngutierrez@preservinc.com");
            return emails;
        }
    }

    @BeforeEach
    void setUp() {
        session.setDebug(true);
        this.emailService = new EmailService(env, session, new ReportsDAOMock(), accessKeyManager);
    }

    @Test
    void testNotifySysAdmin() {
        Exception exception = new RuntimeException("This is a test of the SysAdmin notification system.");
        emailService.notifySysAdmin(exception);
    }

    @Test
    void testNotifyReportSubmission() {
        Employee employee = new Employee(1, "Robert", "Downey Jr.", "Bob", "PM", "nathanaelg16@gmail.com", false, EmployeeStatus.ACTIVE);
        Team team = new Team(1, employee);

        Report report = new Report();
        report.setReportBy(employee);
        report.setReportDate(LocalDate.now(ZoneId.of("America/New_York")));

        assertDoesNotThrow(() -> emailService.sendReportSubmissionNotification(report, new Job(1, null, "123 Main St", null, null, JobStatus.ACTIVE, team)));
    }

    @Test
    void testSendReportSubmission() throws URISyntaxException {
        Employee employee = new Employee(1, "Robert", "Downey Jr.", "Bob", "PM", "nathanaelg16@gmail.com", false, EmployeeStatus.ACTIVE);

        Team team = new Team(1, employee);

        File report = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sampleReport.pdf")).toURI());

        assertDoesNotThrow(() -> emailService.sendReportEmail(employee, new Job(1, null, "123 Main St", null, null, JobStatus.ACTIVE, team), LocalDate.now(), report));
    }

    @AfterEach
    void tearDown() {
        session.setDebug(false);
    }
}
