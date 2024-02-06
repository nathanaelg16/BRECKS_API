package app.brecks.dao.reports;

import com.mongodb.client.result.InsertOneResult;
import app.brecks.model.report.Report;
import lombok.NonNull;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IReportsDAO {
    CompletableFuture<InsertOneResult> saveReport(Report report);
    CompletableFuture<Boolean> checkReportExists(int jobID, @NonNull LocalDate reportDate);
    List<String> getEmailsForReportAdmins() throws SQLException;
}
