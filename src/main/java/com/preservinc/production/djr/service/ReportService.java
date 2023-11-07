package com.preservinc.production.djr.service;

import com.google.firebase.auth.FirebaseToken;
import com.preservinc.production.djr.model.Report;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ReportService {
    private static final Logger logger = LogManager.getLogger();

    public void submitReport(FirebaseToken firebaseToken, Report report) {
        logger.info("[Report Service] Handling report for job site ID {} submitted by {}", report.jobID(), firebaseToken.getName());
        validateReport(report);
    }

    private void validateReport(Report report) throws IllegalArgumentException {
        logger.info("[Report Service] Validating report...");
        if (report.jobID() == 0) throw new IllegalArgumentException("Invalid Job site selected.");
        if (report.reportDate() == null || report.reportDate().isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
            throw new IllegalArgumentException("Invalid Report Date");
        if (report.workArea1() == null || report.workArea1().isBlank())
            throw new IllegalArgumentException("At least one work area must be specified.");
        if (report.subs() == null || report.subs().isBlank())
            throw new IllegalArgumentException("Subcontractor names must be listed.");
    }
}
