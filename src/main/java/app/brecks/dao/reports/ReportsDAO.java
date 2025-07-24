package app.brecks.dao.reports;

import app.brecks.exception.DatabaseException;
import app.brecks.exception.ServerException;
import app.brecks.model.report.Report;
import app.brecks.model.report.ReportHistory;
import app.brecks.model.report.SummarizedReport;
import app.brecks.reactive.CountSubscriber;
import app.brecks.reactive.Finder;
import app.brecks.reactive.InsertOneResultSubscriber;
import app.brecks.reactive.VanillaSubscriber;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

@Repository
public class ReportsDAO implements IReportsDAO {
    private static final Logger logger = LogManager.getLogger();
    
    private final DataSource dataSource;
    private final MongoDatabase mongoDB;
    private final MongoClient mongoClient;

    @Autowired
    public ReportsDAO(DataSource dataSource, MongoDatabase mongoDB, MongoClient mongoClient) {
        this.dataSource = dataSource;
        this.mongoDB = mongoDB;
        this.mongoClient = mongoClient;
    }

    @Override
    public CompletableFuture<InsertOneResult> saveReport(Report report) {
        logger.info("Saving report to database.\nJob ID: {}\tReport Date: {}\tBy: {} (ID# {})",
                report.getJobID(), report.getReportDate(), report.getReportBy(), report.getReportBy().getID());

        MongoCollection<Report> collection = this.mongoDB.getCollection("reports", Report.class);
        CompletableFuture<InsertOneResult> future = new CompletableFuture<>();
        collection.insertOne(report).subscribe(new InsertOneResultSubscriber(future));

        future.whenCompleteAsync((v, t) -> {
            if (t == null) logger.info("Successfully saved report to database.");
            else logger.error("An error occurred saving report to database: {}", t.getMessage());
        });

        return future;
    }

    @Override
    public void updateReport(Report report) throws DatabaseException {
        logger.info("Saving updated report to database.\nJob ID: {}\tReport Date: {}\tBy: {} (ID# {})",
                report.getJobID(), report.getReportDate(), report.getReportBy(), report.getReportBy().getID());

        try {
            CompletableFuture<List<ClientSession>> clientSessionFuture = new CompletableFuture<>();
            this.mongoClient.startSession().subscribe(new Finder<>(clientSessionFuture));
            final ClientSession session = clientSessionFuture.thenApply((results) -> results.isEmpty() ? null : results.get(0)).get();
            final TransactionOptions transactionOptions = TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.LOCAL)
                    .writeConcern(WriteConcern.MAJORITY)
                    .build();

            /* BEGIN TRANSACTION */
            session.startTransaction(transactionOptions);
            logger.info("MongoDB Transaction: Started");

            try {
                logger.info("Retrieving current report...");
                MongoCollection<Report> reports = this.mongoDB.getCollection("reports", Report.class);
                CompletableFuture<List<Report>> reportsFuture = new CompletableFuture<>();
                reports.find(session, eq("_id", report.getId())).subscribe(new Finder<>(reportsFuture));

                MongoCollection<ReportHistory> historicalReports = this.mongoDB.getCollection("historicalReports", ReportHistory.class);

                Report currentReport = reportsFuture.thenApply((result) -> {
                    if (result == null || result.isEmpty()){
                        logger.info("Unable to find report with id {}!", report.getId());
                        throw new CompletionException(new IllegalArgumentException("A report with the given ID [%s] does not exist.".formatted(report.getId())));
                    } else {
                        logger.info("Report found!");
                        return result.get(0);
                    }
                }).get();

                logger.info("Attempting to insert current report into existing historical reports document...");
                CompletableFuture
                        // 1. try to insert current report into historical reports document
                        .supplyAsync(() -> historicalReports.updateOne(session, and(
                                        eq("jobID", report.getJobID()),
                                        eq("reportDate", report.getReportDate())),
                                Updates.push("history", currentReport), new UpdateOptions().upsert(true))
                        ).thenCompose((publisher) -> {
                            CompletableFuture<UpdateResult> resultFuture = new CompletableFuture<>();
                            publisher.subscribe(new VanillaSubscriber<>(resultFuture));
                            return resultFuture;
                        }).thenApply(UpdateResult::wasAcknowledged)
                        .thenCompose((successful) -> {
                            logger.info("Insertion {}!", successful ? "successful" : "UNSUCCESSFUL");
                            // 3. if step 1 or step 2 were successful, delete the current report
                            if (successful) {
                                logger.info("Attempting to delete the current report...");
                                CompletableFuture<DeleteResult> deleteResult = new CompletableFuture<>();
                                reports.deleteOne(session, eq("_id", report.getId()))
                                        .subscribe(new VanillaSubscriber<>(deleteResult));
                                return deleteResult;
                            } else return null;
                        }).thenApply((result) -> {
                            if (result == null) return false;
                            else return result.wasAcknowledged();
                        }).thenCompose((successful) -> {
                            // 4. if that was successful, insert the new report, else return false
                            if (successful) {
                                logger.info("Deletion successful!");
                                CompletableFuture<InsertOneResult> insertOneResult = new CompletableFuture<>();
                                reports.insertOne(session, report.clearID())
                                        .subscribe(new VanillaSubscriber<>(insertOneResult));
                                return insertOneResult;
                            } else return null;
                        }).thenApply((result) -> {
                            if (result == null) return false;
                            else return result.wasAcknowledged();
                        }).thenApply((successful) -> {
                            // 4. if that was successful, commit the transaction, else abort all changes
                            if (successful) {
                                logger.info("Committing transaction!");
                                return session.commitTransaction();
                            } else {
                                logger.info("Aborting transaction!");
                                return session.abortTransaction();
                            }
                        }).thenCompose((publisher) -> {
                            CompletableFuture<List<Void>> done = new CompletableFuture<>();
                            publisher.subscribe(new Finder<>(done));
                            return done;
                        }).get();
            } catch (Exception e) {
                logger.error(e);
                CompletableFuture<List<Void>> done = new CompletableFuture<>();
                if (session.hasActiveTransaction()) {
                    logger.info("Aborting transaction due to exception!");
                    session.abortTransaction().subscribe(new Finder<>(done));
                }
                done.get();
                throw e;
            }

            if (session.hasActiveTransaction()) {
                CompletableFuture<List<Void>> done = new CompletableFuture<>();
                logger.info("Transaction was never committed! Aborting....");
                session.abortTransaction().subscribe(new Finder<>(done));
                logger.info("MongoDB Transaction: Started");
            }

            logger.info("MongoDB Transaction: Ended");
            /* END TRANSACTION */

        } catch (ExecutionException | InterruptedException | CancellationException e) {
            logger.error(e);
            if (ExceptionUtils.getRootCause(e) instanceof CompletionException) throw new DatabaseException();
            throw new ServerException(e);
        } catch (Exception e) {
            logger.error(e);
            throw new ServerException(e);
        }
    }

    public CompletableFuture<Boolean> checkReportExists(int jobID, @NonNull LocalDate reportDate) {
        logger.info("Checking if report exists for job ID `{}` on `{}`", jobID, reportDate);
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
        logger.info("Getting emails for report admins.");

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
    public Report getReport(ObjectId reportID) {
        logger.traceEntry("getReport(reportID={})", reportID);
        CompletableFuture<List<Report>> completableFuture = new CompletableFuture<>();
        this.mongoDB.getCollection("reports")
                .find(eq("_id", reportID), Report.class)
                .subscribe(new Finder<>(completableFuture));
        try {
            List<Report> results = completableFuture.get();
            return results.isEmpty() ? null : results.get(0);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception occurred retrieving reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public List<Report> getReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        logger.traceEntry("getReports(job={}, startDate={}, endDate={})", startDate, endDate);
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
            logger.error("Exception occurred retrieving reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public List<SummarizedReport> getSummarizedReports(Integer job, LocalDate startDate, LocalDate endDate) {
        logger.traceEntry("getReports(job={}, startDate={}, endDate={})", startDate, endDate);
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
            logger.error("Exception occurred retrieving reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public List<SummarizedReport> getSummarizedHistoricalReports(Integer job, LocalDate date) {
        logger.traceEntry("getSummarizedHistoricalReports(job={}, date={})", job, date);

        CompletableFuture<List<ReportHistory>> completableFuture = new CompletableFuture<>();

        this.mongoDB.getCollection("historicalReports")
                .find(and(
                        eq("jobID", job),
                        eq("reportDate", date)
                ), ReportHistory.class)
                .subscribe(new Finder<>(completableFuture));

        try {
            return completableFuture.thenApply((results) -> {
                if (results.isEmpty()) return null;
                else return results.get(0);
            }).thenApply((reportHistory) -> {
                if (reportHistory == null) return new ArrayList<SummarizedReport>();
                else return reportHistory.getHistory().stream().map((history) -> {
                    SummarizedReport summarizedReport = new SummarizedReport();
                    summarizedReport.setId(history.getId());
                    summarizedReport.setDate(history.getReportDate());
                    summarizedReport.setCrew(history.getCrew());
                    return summarizedReport;
                }).collect(Collectors.toCollection(ArrayList::new));
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception occurred retrieving historical reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public Report getHistoricalReport(@NonNull Integer job, @NonNull LocalDate date, ObjectId objectId) {
        logger.traceEntry("getHistoricalReport(job={}, date={}, objectId={})", job, date, objectId);
        CompletableFuture<List<ReportHistory>> completableFuture = new CompletableFuture<>();

        this.mongoDB.getCollection("historicalReports")
                .find(and(
                        eq("jobID", job),
                        eq("reportDate", date)
                ), ReportHistory.class)
                .subscribe(new Finder<>(completableFuture));

        try {
            return completableFuture.thenApply((results) -> {
                if (results.isEmpty()) return null;
                else return results.get(0);
            }).thenApply((reportHistory) -> {
                if (reportHistory == null) return java.util.Optional.<Report>empty();
                else return reportHistory.getHistory().parallelStream().filter((report) -> report.getId().equals(objectId)).findFirst();
            }).thenApply((optional) -> optional.orElse(null)).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception occurred retrieving historical reports from database.");
            logger.error(e);
            throw new DatabaseException();
        }
    }
}
