package com.preservinc.production.djr.service.report;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.firebase.auth.FirebaseToken;
import com.preservinc.production.djr.dao.employees.IEmployeesDAO;
import com.preservinc.production.djr.dao.jobs.IJobsDAO;
import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.exception.report.*;
import com.preservinc.production.djr.model.report.Report;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.team.TeamMemberRole;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.email.IEmailService;
import com.preservinc.production.djr.service.weather.IWeatherService;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class ReportService implements IReportService {
    private static final Logger logger = LogManager.getLogger();

    private final Environment env;
    private final IWeatherService weatherService;
    private final IEmailService emailService;
    private final IReportsDAO reportsDAO;
    private final IJobsDAO jobsDAO;
    private final IEmployeesDAO employeesDAO;
    private final AmazonS3 spaces;

    @Autowired
    public ReportService(Environment env, IWeatherService weatherService, IEmailService emailService,
                         IReportsDAO reportsDAO, IJobsDAO jobsDAO, IEmployeesDAO employeesDAO,
                         AmazonS3 spaces) {
        this.env = env;
        this.weatherService = weatherService;
        this.emailService = emailService;
        this.reportsDAO = reportsDAO;
        this.jobsDAO = jobsDAO;
        this.employeesDAO = employeesDAO;
        this.spaces = spaces;
    }

    @Override
    public void submitReport(FirebaseToken firebaseToken, Report report) {
        logger.info("[Report Service] Handling report for job site ID {} submitted by {}", report.getJobID(), firebaseToken.getName());
        validateReport(report);
        checkWeather(report);

        try {
            Employee reportingUser;
            Job job = jobsDAO.getJob(report.getJobID());

            if (job == null)
                throw new InvalidJobSiteException();

            logger.info("[Report Service] Team PM: {}", job.team().getProjectManager().fullName());

            Pair<Employee, TeamMemberRole> reportingUserAndRole = job.team().findTeamMemberByUID(firebaseToken.getUid());
            if (reportingUserAndRole == null) {
                logger.info("[Report Service] Reporting user is not a member of the assigned team.");
                reportingUser = employeesDAO.findEmployeeByUID(firebaseToken.getUid());
                report.setPS(job.team().findTeamMembersByRole(TeamMemberRole.PROJECT_SUPERVISOR).get(0));
            } else {
                reportingUser = reportingUserAndRole.getLeft();
                TeamMemberRole role = reportingUserAndRole.getRight();
                if (role == TeamMemberRole.PROJECT_SUPERVISOR) report.setPS(reportingUser);
                else report.setPS(job.team().findTeamMembersByRole(TeamMemberRole.PROJECT_SUPERVISOR).get(0));
                logger.debug("[Report Service] Reporting user is member of the team.");
            }

            report.setReportBy(reportingUser);
            report.setPM(job.team().getProjectManager());
            reportsDAO.saveReport(report);

            File reportPDF = null;

            try {
                reportPDF = generateReportPDF(report, job.address());
                emailService.sendReportEmail(reportingUser, job, report.getReportDate(), reportPDF);
            } catch (SQLException | RuntimeException | IOException e) {
                logger.error("[Report Service] Error generating PDF: {}", e.getMessage());
                logger.error(ExceptionUtils.getStackTrace(e));
                try { emailService.sendReportSubmissionNotification(report, job); }
                catch (Exception ignored) {}
            } catch (MessagingException e) {
                logger.error("[Report Service] An error occurred delivering the message: {}", e.getMessage());
                logger.error(ExceptionUtils.getStackTrace(e));
                try { emailService.sendReportSubmissionNotification(report, job); }
                catch (Exception ignored) {}
            } finally {
                try {
                    if (reportPDF != null) reportPDF.delete();
                } catch (Exception e) {
                    reportPDF.deleteOnExit();
                }
            }
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    private void validateReport(Report report) {
        logger.info("[Report Service] Validating report...");
        if (report.getJobID() == 0) throw new InvalidJobSiteException();
        if (report.getReportDate() == null || report.getReportDate().isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
            throw new InvalidReportDateException();
        if (report.getWorkArea1() == null || report.getWorkArea1().isBlank())
            throw new InvalidWorkAreaException();
        if (report.getSubs() == null || report.getSubs().isBlank())
            throw new InvalidSubcontractorException();
    }

    private void checkWeather(Report report) {
        logger.info("[Report Service] Checking weather...");
        if (report.getWeather() == null || report.getWeather().isBlank()) {
            logger.info("[Report Service] Weather not specified. Fetching weather...");

            Weather weather;

            if (report.getReportDate().isBefore(LocalDate.now(ZoneId.of("America/New_York"))))
                weather = weatherService.getWeatherOnDate(report.getReportDate());
            else weather = weatherService.getTodaysWeather();

            if (weather == null)
                throw new WeatherNotFoundException();
            else report.setWeather(weather.toString());
        }
    }

    private File generateReportPDF(Report report, String address) throws IOException {
        // todo store template files locally and compare hashes before downloading from S3

        logger.info("[Report Service] Generating PDF...");

        Path reportPDFPath = Path.of(System.getProperty("java.io.tmpdir"), "%s - DJR - %s.pdf"
                .formatted(address, report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
        Path tempFilePath = Files.createTempFile("report", ".pdf");

        logger.info("[Report Service] Retrieving PDF template...");
        S3Object reportPDFTemplateObject = spaces.getObject(env.getProperty("spaces.name"),
                "%s/DJR%s.pdf".formatted(env.getProperty("spaces.folder"), report.isOnsite() ? "" : "-R"));
        S3ObjectInputStream reportPDFTemplateObjectInputStream = reportPDFTemplateObject.getObjectContent();
        Files.copy(reportPDFTemplateObjectInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("[Report Service] Writing report data to PDF...");
        File reportTemplate = tempFilePath.toFile();
        PDDocumentCatalog catalog;
        try (PDDocument reportPDDocument = Loader.loadPDF(reportTemplate)) {
            catalog = reportPDDocument.getDocumentCatalog();

            PDAcroForm form = catalog.getAcroForm();
            form.getField("Project Address").setValue(address);
            form.getField("PMPS").setValue("%s / %s".formatted(report.getPM(), report.getPS()));
            form.getField("Date").setValue(report.getReportDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            form.getField("Person Filing Report").setValue(report.getReportBy().displayName());
            form.getField("Weather").setValue(report.getWeather());
            form.getField("Workers Onsite").setValue(String.valueOf(report.getCrewSize()));
            form.getField("Visitors").setValue(report.getVisitors());
            form.getField("Work1").setValue(report.getWorkArea1());
            form.getField("Work2").setValue(report.getWorkArea2());
            form.getField("Work3").setValue(report.getWorkArea3());
            form.getField("Work4").setValue(report.getWorkArea4());
            form.getField("Work5").setValue(report.getWorkArea5());
            form.getField("Materials1").setValue(report.getMaterials1());
            form.getField("Materials2").setValue(report.getMaterials2());
            form.getField("Materials3").setValue(report.getMaterials3());
            form.getField("Materials4").setValue(report.getMaterials4());
            form.getField("Subs").setValue(report.getSubs());
            form.flatten();

            reportPDDocument.save(reportPDFPath.toFile());
        }

        logger.info("[Report Service] PDF generated successfully.");
        return reportPDFPath.toFile();
    }
}