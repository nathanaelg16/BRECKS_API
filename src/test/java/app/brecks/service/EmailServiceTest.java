package app.brecks.service;

import app.brecks.auth.accesskey.AccessKeyManager;
import app.brecks.dao.reports.IReportsDAO;
import app.brecks.exception.DatabaseException;
import app.brecks.model.employee.EmployeeStatus;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStatus;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import app.brecks.model.team.Team;
import app.brecks.model.team.TeamMember;
import app.brecks.model.team.TeamMemberRole;
import app.brecks.service.email.EmailService;
import com.mongodb.client.result.InsertOneResult;
import jakarta.mail.Session;
import lombok.NonNull;
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
import java.util.concurrent.CompletableFuture;

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
        public CompletableFuture<InsertOneResult> saveReport(Report report) {
            logger.info("Saving report {}", report);
            return null;
        }

        @Override
        public void updateReport(Report report) throws DatabaseException {
            logger.info("Updating report {}", report);
        }

        @Override
        public CompletableFuture<Boolean> checkReportExists(int jobID, @NonNull LocalDate reportDate) {
            return null;
        }

        @Override
        public List<String> getEmailsForReportAdmins() throws SQLException {
            List<String> emails = new ArrayList<>();
            emails.add("ngutierrez@preservinc.com");
            return emails;
        }

        @Override
        public List<Report> getReports(Integer job, LocalDate startDate, LocalDate endDate) {
            return null;
        }

        @Override
        public List<SummarizedReport> getSummarizedReports(Integer job, LocalDate startDate, LocalDate endDate) {
            return null;
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
        TeamMember teamMember = new TeamMember(1, "Robert", "Downey Jr.", "Bob", "PM", "nathanaelg16@gmail.com", false, EmployeeStatus.ACTIVE, TeamMemberRole.PROJECT_SUPERVISOR);
        Team team = new Team(1, teamMember);

        Report report = new Report();
        report.setReportBy(teamMember);
        report.setReportDate(LocalDate.now(ZoneId.of("America/New_York")));

        assertDoesNotThrow(() -> emailService.sendReportSubmissionNotification(report, new Job(1, null, "123 Main St", null, null, JobStatus.ACTIVE, team)));
    }

    @Test
    void testSendReportSubmission() throws URISyntaxException {
        TeamMember teamMember = new TeamMember(1, "Robert", "Downey Jr.", "Bob", "PM", "nathanaelg16@gmail.com", false, EmployeeStatus.ACTIVE, TeamMemberRole.PROJECT_SUPERVISOR);

        Team team = new Team(1, teamMember);

        File report = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("sampleReport.pdf")).toURI());

        assertDoesNotThrow(() -> emailService.sendReportEmail(teamMember, new Job(1, null, "123 Main St", null, null, JobStatus.ACTIVE, team), LocalDate.now(), report));
    }

    @AfterEach
    void tearDown() {
        session.setDebug(false);
    }
}
