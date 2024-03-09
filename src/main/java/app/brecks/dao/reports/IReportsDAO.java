package app.brecks.dao.reports;

import app.brecks.exception.DatabaseException;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import com.mongodb.client.result.InsertOneResult;
import lombok.NonNull;
import org.bson.types.ObjectId;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IReportsDAO {
    CompletableFuture<InsertOneResult> saveReport(Report report);
    CompletableFuture<Boolean> checkReportExists(int jobID, @NonNull LocalDate reportDate);
    List<String> getEmailsForReportAdmins() throws SQLException;
    Report getReport(ObjectId objectId);
    List<Report> getReports(Integer job, LocalDate startDate, LocalDate endDate);
    List<SummarizedReport> getSummarizedReports(Integer job, LocalDate startDate, LocalDate endDate);
    void updateReport(Report report) throws DatabaseException;
}
