package app.brecks.service.report;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.model.report.Report;

import java.time.LocalDate;

public interface IReportService {
    void submitReport(AuthorizationToken token, Report report);

    boolean checkExists(Integer job, LocalDate date);
}
