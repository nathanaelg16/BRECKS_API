package com.preservinc.production.djr.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.firebase.auth.FirebaseToken;
import com.preservinc.production.djr.dao.employees.IEmployeesDAO;
import com.preservinc.production.djr.dao.jobs.IJobsDAO;
import com.preservinc.production.djr.dao.reports.IReportsDAO;
import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.exception.report.*;
import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.team.Team;
import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.team.TeamMemberRole;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.email.IEmailService;
import com.preservinc.production.djr.service.weather.IWeatherService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Properties;

@Service
public class ReportService {
    private static final Logger logger = LogManager.getLogger();

    private final IWeatherService weatherService;
    private final IEmailService emailService;
    private final IReportsDAO reportsDAO;
    private final IJobsDAO jobsDAO;
    private final IEmployeesDAO employeesDAO;
    private final AmazonS3 spaces;
    private final Properties config;

    @Autowired
    public ReportService(IWeatherService weatherService, IEmailService emailService,
                         IReportsDAO reportsDAO, IJobsDAO jobsDAO, IEmployeesDAO employeesDAO,
                         AmazonS3 spaces, Properties config) {
        this.weatherService = weatherService;
        this.emailService = emailService;
        this.reportsDAO = reportsDAO;
        this.jobsDAO = jobsDAO;
        this.employeesDAO = employeesDAO;
        this.spaces = spaces;
        this.config = config;
    }

    public void submitReport(FirebaseToken firebaseToken, Report report) {
        logger.info("[Report Service] Handling report for job site ID {} submitted by {}", report.getJobID(), firebaseToken.getName());
        validateReport(report);
        checkWeather(report);

        try {
            Employee reportingUser;
            Team team = jobsDAO.getTeam(report.getJobID());

            if (team == null)
                throw new InvalidJobSiteException();

            Pair<Employee, TeamMemberRole> reportingUserAndRole = team.findTeamMemberByUID(firebaseToken.getUid());
            if (reportingUserAndRole == null) {
                reportingUser = employeesDAO.findEmployeeByUID(firebaseToken.getUid());
                report.setPS(team.findTeamMembersByRole(TeamMemberRole.PROJECT_SUPERVISOR).get(0));
            } else {
                reportingUser = reportingUserAndRole.getLeft();
                TeamMemberRole role = reportingUserAndRole.getRight();
                if (role == TeamMemberRole.PROJECT_SUPERVISOR) report.setPS(reportingUser);
                else report.setPS(team.findTeamMembersByRole(TeamMemberRole.PROJECT_SUPERVISOR).get(0));
            }

            report.setReportBy(reportingUser);
            report.setPM(team.getProjectManager());
            reportsDAO.saveReport(report);

            try {
                emailService.sendReportEmail(generateReportPDF(report));
            } catch (RuntimeException | IOException e) {
                logger.error("[Report Service] Error generating PDF: {}", e.getMessage());
                emailService.sendReportSubmissionNotification(report);
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

    private File generateReportPDF(Report report) throws IOException {
        // todo store template files locally and compare hashes before downloading from S3

        logger.info("[Report Service] Generating PDF...");

        String address;
        try {
            address = this.jobsDAO.getJobAddress(report.getJobID());
        } catch (SQLException e) {
            logger.error("[Report Service] Error retrieving job address: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        Path reportPDFPath = Path.of(System.getProperty("java.io.tmpdir"), "%s - DJR - %s.pdf"
                .formatted(address, report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
        Path tempFilePath = Files.createTempFile("report", ".pdf");

        logger.info("[Report Service] Retrieving PDF template...");
        S3Object reportPDFTemplateObject = spaces
                .getObject("%s/%s".formatted(config.getProperty("spaces.name"),
                        config.getProperty("spaces.folder")), "DJR%s.pdf".formatted(report.isOnsite() ? "" : "-R"));
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