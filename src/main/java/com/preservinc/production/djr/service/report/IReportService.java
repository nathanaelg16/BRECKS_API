package com.preservinc.production.djr.service.report;

import com.preservinc.production.djr.auth.jwt.AuthorizationToken;
import com.preservinc.production.djr.model.report.Report;

public interface IReportService {
    void submitReport(AuthorizationToken firebaseToken, Report report);
}
