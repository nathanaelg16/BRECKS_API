package app.brecks.dao.reports;

import app.brecks.exception.DatabaseException;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import app.brecks.reactive.CountSubscriber;
import app.brecks.reactive.Finder;
import app.brecks.reactive.InsertOneResultSubscriber;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.mongodb.client.model.Filters.*;

@Repository
public class ReportsDAO implements IReportsDAO {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[Reports DAO]");
    private final DataSource dataSource;
    private final MongoDatabase mongoDB;

    @Autowired
    public ReportsDAO(DataSource dataSource, MongoDatabase mongoDB) {
        this.dataSource = dataSource;
        this.mongoDB = mongoDB;
    }

    @Override
    public CompletableFuture<InsertOneResult> saveReport(Report report) {
        logger.info("[ReportsDAO] Saving report to database.\nJob ID: {}\tReport Date: {}\tBy: {} (ID# {})",
                report.getJobID(), report.getReportDate(), report.getReportBy(), report.getReportBy().getID());

        MongoCollection<Report> collection = this.mongoDB.getCollection("reports", Report.class);
        CompletableFuture<InsertOneResult> future = new CompletableFuture<>();
        collection.insertOne(report).subscribe(new InsertOneResultSubscriber(future));

        future.whenCompleteAsync((v, t) -> {
            if (t == null) logger.info("[ReportsDAO] Successfully saved report to database.");
            else logger.error("[ReportsDAO] An error occurred saving report to database: {}", t.getMessage());
        });

        return future;
    }

    public CompletableFuture<Boolean> checkReportExists(int jobID, @NonNull LocalDate reportDate) {
        logger.info("[ReportsDAO] Checking if report exists for job ID `{}` on `{}`", jobID, reportDate);
        MongoCollection<Report> collection = this.mongoDB.getCollection("reports", Report.class);
        CompletableFuture<Long> count = new CompletableFuture<>();
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        collection.countDocuments(and(eq("jobID", jobID), eq("reportDate", reportDate)))
                .subscribe(new CountSubscriber(count));
        count.whenCompleteAsync((v, t) -> {
            if (t == null && v != null) result.complete(v != 0);
            else if (v == null) result.complete(null);
            else result.completeExceptionally(t);
        });

        return result;
    }

    @Override
    public List<String> getEmailsForReportAdmins() throws SQLException {
        logger.info("[ReportsDAO] Getting emails for report admins.");

        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select E.email " +
                     "from ReportAdmins R " +
                     "inner join Employees E on E.id = R.employee_id " +
                     "where R.notify_reports is true;");
             ResultSet r = p.executeQuery()
        ) {
            List<String> emails = new ArrayList<>();
            while (r.next()) emails.add(r.getString("email"));
            return emails;
        }
    }

    @Override
    public List<Report> getReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        logger.traceEntry("{} getReports(job={}, startDate={}, endDate={})", marker, startDate, endDate);
        CompletableFuture<List<Report>> completableFuture = new CompletableFuture<>();
        this.mongoDB.getCollection("reports")
                .find(and(
                                eq("jobID", job),
                                gte("reportDate", startDate),
                                lte("reportDate", endDate)
                        ), Report.class
                ).subscribe(new Finder<>(completableFuture));
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(marker, "Exception occurred retrieving reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public List<SummarizedReport> getSummarizedReports(Integer job, LocalDate startDate, LocalDate endDate) {
        logger.traceEntry("{} getReports(job={}, startDate={}, endDate={})", marker, startDate, endDate);
        CompletableFuture<List<SummarizedReport>> completableFuture = new CompletableFuture<>();
        this.mongoDB.getCollection("reports")
                .find(and(
                                eq("jobID", job),
                                gte("reportDate", startDate),
                                lte("reportDate", endDate)
                        ), SummarizedReport.class
                ).subscribe(new Finder<>(completableFuture));
        try {
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(marker, "Exception occurred retrieving reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }
}
