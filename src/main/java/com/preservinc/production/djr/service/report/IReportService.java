package com.preservinc.production.djr.service.report;

import com.google.firebase.auth.FirebaseToken;
import com.preservinc.production.djr.model.report.Report;

public interface IReportService {
    void submitReport(FirebaseToken firebaseToken, Report report);
}
