package app.brecks.service.report;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.model.report.Report;

public interface IReportService {
    void submitReport(AuthorizationToken firebaseToken, Report report);
}
