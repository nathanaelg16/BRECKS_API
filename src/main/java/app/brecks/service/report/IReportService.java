package app.brecks.service.report;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.List;

public interface IReportService {
    void submitReport(AuthorizationToken token, Report report);

    void updateReport(AuthorizationToken authorizationToken, Report report);

    boolean checkExists(Integer job, LocalDate date);

    Report getReport(@NonNull String reportID);

    List<Report> getReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    List<SummarizedReport> getSummarizedReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    List<SummarizedReport> getSummarizedHistoricalReports(@NonNull Integer job, @NonNull LocalDate date);

    Report getHistoricalReport(@NonNull Integer job, @NonNull LocalDate date, @NonNull String versionID);

    void restoreReport(@NonNull Integer job, @NonNull LocalDate date, @NonNull String versionID);
}
