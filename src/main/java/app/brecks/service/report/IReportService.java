package app.brecks.service.report;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.model.report.Report;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.List;

public interface IReportService {
    void submitReport(AuthorizationToken token, Report report);

    boolean checkExists(Integer job, LocalDate date);

    List<Report> getReports(@NonNull Integer job, @NonNull LocalDate startDate, @NonNull LocalDate endDate);
}
