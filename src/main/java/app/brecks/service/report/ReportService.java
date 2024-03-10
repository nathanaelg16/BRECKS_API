package app.brecks.service.report;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.dao.employees.IEmployeeDAO;
import app.brecks.dao.jobs.IJobsDAO;
import app.brecks.dao.reports.IReportsDAO;
import app.brecks.exception.BadRequestException;
import app.brecks.exception.DatabaseException;
import app.brecks.exception.ServerException;
import app.brecks.exception.report.*;
import app.brecks.model.employee.Employee;
import app.brecks.model.job.Job;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import app.brecks.model.team.TeamMember;
import app.brecks.model.team.TeamMemberRole;
import app.brecks.model.weather.Weather;
import app.brecks.service.email.IEmailService;
import app.brecks.service.weather.IWeatherService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.mongodb.client.result.InsertOneResult;
import io.jsonwebtoken.JwtParser;
import jakarta.mail.MessagingException;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.bson.types.ObjectId;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

@Service
public class ReportService implements IReportService {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[Report Service]");

    private final Environment env;
    private final IWeatherService weatherService;
    private final IEmailService emailService;
    private final IReportsDAO reportsDAO;
    private final IJobsDAO jobsDAO;
    private final IEmployeeDAO employeesDAO;
    private final AmazonS3 spaces;
    private final JwtParser jwtParser;

    @Autowired
    public ReportService(Environment env, IWeatherService weatherService, IEmailService emailService,
                         IReportsDAO reportsDAO, IJobsDAO jobsDAO, IEmployeeDAO employeesDAO,
                         AmazonS3 spaces, JwtParser jwtParser) {
        this.env = env;
        this.weatherService = weatherService;
        this.emailService = emailService;
        this.reportsDAO = reportsDAO;
        this.jobsDAO = jobsDAO;
        this.employeesDAO = employeesDAO;
        this.spaces = spaces;
        this.jwtParser = jwtParser;
    }

    @Override
    public boolean checkExists(Integer job, LocalDate date) {
        if (job == null || date == null || job <= 0 || date.isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
            throw new BadRequestException();

        try {
            Boolean result = this.reportsDAO.checkReportExists(job, date).get();
            if (result == null) throw new DatabaseException();
            else return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void submitReport(AuthorizationToken authorizationToken, Report report) {
        Integer tokenUserID = this.jwtParser.parseSignedClaims(authorizationToken.token()).getPayload().get("userID", Integer.class);
        logger.info("[Report Service] Handling report for job site ID {} submitted by user id#{}", report.getJobID(), tokenUserID);
        validateReport(report, false);
        checkWeather(report);

        try {
            Job job = jobsDAO.getJob(report.getJobID());

            if (job == null)
                throw new InvalidJobSiteException();

            logger.info("[Report Service] Team PM: {}", job.team().getProjectManager().getFullName());

            TeamMember tmReportingUser = job.team().findTeamMemberByID(tokenUserID);

            TeamMember PS, PM = job.team().getProjectManager();

            if (tmReportingUser != null && tmReportingUser.teamRole() == TeamMemberRole.PROJECT_SUPERVISOR) PS = tmReportingUser;
            else PS = job.team().findTeamMembersByRole(TeamMemberRole.PROJECT_SUPERVISOR).get(0);

            Employee reportingUser = tmReportingUser == null ? employeesDAO.findEmployeeByID(tokenUserID) : tmReportingUser;

            report.setReportBy(reportingUser);
            CompletableFuture<InsertOneResult> result = reportsDAO.saveReport(report);
            result.whenCompleteAsync((v, t) -> {
                if (t == null && v.wasAcknowledged()) {
                    String reportEmailProperty = this.env.getProperty("platform.reports.send-email");
                    if (reportEmailProperty != null) {
                        if (reportEmailProperty.equalsIgnoreCase("pdf")) {
                            File reportPDF = null;
                            try {
                                reportPDF = generateReportPDF(report, job.address(), PM, PS);
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
                        } else if (reportEmailProperty.equalsIgnoreCase("true") || reportEmailProperty.equalsIgnoreCase("notification")) {
                            try { emailService.sendReportSubmissionNotification(report, job); }
                            catch (Exception ignored) {}
                        }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void updateReport(AuthorizationToken authorizationToken, Report report) {
        Integer tokenUserID = this.jwtParser.parseSignedClaims(authorizationToken.token()).getPayload().get("userID", Integer.class);
        logger.info("[Report Service] Handling report update for job site ID {} submitted by user id#{}", report.getJobID(), tokenUserID);
        validateReport(report, true);
        try {
            Employee reportingEmployee = this.employeesDAO.findEmployeeByID(tokenUserID);
            report.setReportBy(reportingEmployee);
            this.reportsDAO.updateReport(report);
        } catch (SQLException e) {
            throw new DatabaseException();
        }
    }

    @Override
    public List<Report> getReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        logger.traceEntry("{} getReports(job={}, startDate={}, endDate={}", marker, job, startDate, endDate);

        if (job == 0) throw new BadRequestException();

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        List<Report> reports = this.reportsDAO.getReports(job, startDate, endDate);
        reports.sort(Comparator.comparing(Report::getReportDate));

        return reports;
    }

    @Override
    public Report getReport(@NonNull String reportID) {
        logger.traceEntry("{} getReport(reportID={})", marker, reportID);
        try {
            return this.reportsDAO.getReport(new ObjectId(reportID));
        } catch (IllegalArgumentException e) {
            logger.error("{} Could not convert string `{}` to ObjectID", marker, reportID);
            throw new BadRequestException();
        }
    }

    @Override
    public Report getHistoricalReport(@NonNull Integer job, @NonNull LocalDate date, @NonNull String versionID) {
        logger.traceEntry("{} getHistoricalReport(job={}, date={}, versionID={})", marker, job, date, versionID);

        if (job == 0) throw new BadRequestException();

        try {
            return this.reportsDAO.getHistoricalReport(job, date, new ObjectId(versionID));
        } catch (IllegalArgumentException e) {
            logger.error("{} Could not convert string `{}` to ObjectID", marker, versionID);
            throw new BadRequestException();
        }
    }

    @Override
    public List<SummarizedReport> getSummarizedReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        logger.traceEntry("{} getSummarizedReports(job={}, startDate={}, endDate={}", marker, job, startDate, endDate);

        if (job == 0) throw new BadRequestException();

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        List<SummarizedReport> reports = this.reportsDAO.getSummarizedReports(job, startDate, endDate);
        reports.sort(Comparator.comparing(SummarizedReport::getDate));

        return reports;
    }

    @Override
    public List<SummarizedReport> getSummarizedHistoricalReports(@NonNull Integer job, @NonNull LocalDate date) {
        logger.traceEntry("{} getSummarizedHistoricalReports(job={}, date={})", marker, job, date);

        if (job == 0) throw new BadRequestException();

        List<SummarizedReport> reports = this.reportsDAO.getSummarizedHistoricalReports(job, date);
        reports.sort(Comparator.comparing(SummarizedReport::getTimestamp).reversed());

        return reports;
    }

    private void validateReport(Report report, boolean update) {
        logger.info("[Report Service] Validating report...");

        if (report.getJobID() == 0) throw new InvalidJobSiteException();

        if (report.getReportDate() == null || report.getReportDate().isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
            throw new InvalidReportDateException();

        CompletableFuture<Boolean> dbCheckFuture;
        if (update)
            dbCheckFuture = new CompletableFuture<>()
                    .completeAsync(() -> this.reportsDAO.getReport(report.getId()))
                    .thenApply((resultObj) -> {
                        if (resultObj == null) throw new CompletionException(new NullPointerException("Report obtained from DB was null."));
                        Report result = (Report) resultObj;
                        return result.getJobID() == report.getJobID() && result.getReportDate().equals(report.getReportDate());
                    });
        else dbCheckFuture = this.reportsDAO.checkReportExists(report.getJobID(), report.getReportDate())
                .thenApply((result) -> !result);

        if (report.getWorkDescriptions() == null || report.getWorkDescriptions().isEmpty())
            throw new InvalidWorkDescriptionException();

        if (report.getCrew().values().stream().anyMatch(Objects::isNull)) throw new InvalidCrewException();

        try {
            if (!dbCheckFuture.get()) throw update ? new BadRequestException() : new DuplicateReportException();
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            logger.error("{} Error: {}", marker, e.getMessage());
            if (ExceptionUtils.getRootCause(e) instanceof NullPointerException) throw new BadRequestException();
            else throw new ServerException(e);
        }
    }

    private void checkWeather(Report report) {
        logger.info("[Report Service] Checking weather...");
        if (report.getWeather() == null || report.getWeather().isBlank()) {
            logger.info("[Report Service] Weather not specified. Fetching weather...");

            Weather weather;

            if (report.getReportDate().isBefore(LocalDate.now(ZoneId.of("America/New_York"))))
                weather = weatherService.getWeatherOnDate(report.getReportDate());
            else weather = weatherService.getCurrentWeather();

            if (weather == null)
                throw new WeatherNotFoundException();
            else report.setWeather(weather.toString());
        }
    }

    private File generateReportPDF(Report report, String address, Employee PM, Employee PS) throws IOException {
        // todo store template files locally and compare hashes before downloading from S3

        logger.info("[Report Service] Generating PDF...");

        Path reportPDFPath = Path.of(System.getProperty("java.io.tmpdir"), "%s - DJR - %s.pdf"
                .formatted(address, report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
        Path tempFilePath = Files.createTempFile("report", ".pdf");

        logger.info("[Report Service] Retrieving PDF template...");
        S3Object reportPDFTemplateObject = spaces.getObject(env.getProperty("spaces.name"), "templates/DJR.pdf");
        S3ObjectInputStream reportPDFTemplateObjectInputStream = reportPDFTemplateObject.getObjectContent();
        Files.copy(reportPDFTemplateObjectInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("[Report Service] Writing report data to PDF...");
        File reportTemplate = tempFilePath.toFile();
        PDDocumentCatalog catalog;
        try (PDDocument reportPDDocument = Loader.loadPDF(reportTemplate)) {
            catalog = reportPDDocument.getDocumentCatalog();

            BiFunction<List<String>, Integer, String[]> condense = (descriptions, numFields) -> {
                if (numFields >= descriptions.size()) return descriptions.toArray(new String[numFields]);
                int numPerGroup = descriptions.size() / numFields;
                int remainder = descriptions.size() % numFields;
                String[] condensed = new String[numFields];
                int j = 0;
                for (int i = 0; i < numFields; i++) {
                    int total = numPerGroup;
                    if (i < remainder) total++;
                    condensed[i] = (String.join("; ", descriptions.subList(j, total)));
                    j += total;
                }
                return condensed;
            };

            String[] condensedWorkDescriptions = condense.apply(report.getWorkDescriptions(), 5);
            String[] condensedMaterials = condense.apply(report.getMaterials(), 4);

            PDAcroForm form = catalog.getAcroForm();
            form.getField("Project Address").setValue(address);
            form.getField("PMPS").setValue("%s / %s".formatted(PM, PS));
            form.getField("Date").setValue(report.getReportDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            form.getField("Person Filing Report").setValue(report.getReportBy().getDisplayName());
            form.getField("Weather").setValue(report.getWeather());
            form.getField("Workers Onsite").setValue(String.valueOf(report.getCrew().values().stream().reduce(0, Integer::sum)));
            form.getField("Visitors").setValue(report.getVisitors());
            form.getField("Work1").setValue(condensedWorkDescriptions[0]);
            form.getField("Work2").setValue(condensedWorkDescriptions[1]);
            form.getField("Work3").setValue(condensedWorkDescriptions[2]);
            form.getField("Work4").setValue(condensedWorkDescriptions[3]);
            form.getField("Work5").setValue(condensedWorkDescriptions[4]);
            form.getField("Materials1").setValue(condensedMaterials[0]);
            form.getField("Materials2").setValue(condensedMaterials[1]);
            form.getField("Materials3").setValue(condensedMaterials[2]);
            form.getField("Materials4").setValue(condensedMaterials[3]);
            form.getField("Subs").setValue(report.getCrew().keySet()
                    .stream()
                    .filter((key) -> !key.equalsIgnoreCase("preserv"))
                    .reduce("", (iden, val) -> String.join(" ", iden, val))
                    .strip());
            form.flatten();

            reportPDDocument.save(reportPDFPath.toFile());
        }

        logger.info("[Report Service] PDF generated successfully.");
        return reportPDFPath.toFile();
    }
}