package com.preservinc.production.djr.service;

import com.google.firebase.auth.FirebaseToken;
import com.preservinc.production.djr.dao.ReportDAO;
import com.preservinc.production.djr.exception.ReportSubmissionException;
import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.email.EmailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ReportService {
    private static final Logger logger = LogManager.getLogger();

    private final WeatherService weatherService;
    private final EmailService emailService;
    private final ReportDAO reportDAO;

    @Autowired
    public ReportService(WeatherService weatherService, ReportDAO reportDAO, EmailService emailService) {
        this.weatherService = weatherService;
        this.reportDAO = reportDAO;
        this.emailService = emailService;
    }

    public void submitReport(FirebaseToken firebaseToken, Report report) {
        logger.info("[Report Service] Handling report for job site ID {} submitted by {}", report.jobID(), firebaseToken.getName());
        validateReport(report);
        checkWeather(report);
        reportDAO.saveReport(report);
        emailService.sendReportEmail(report);
    }

    private void validateReport(Report report) throws ReportSubmissionException {
        logger.info("[Report Service] Validating report...");
        if (report.jobID() == 0) throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.INVALID_JOB_SITE);
        if (report.reportDate() == null || report.reportDate().isAfter(LocalDate.now(ZoneId.of("America/New_York"))))
            throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.INVALID_REPORT_DATE);
        if (report.workArea1() == null || report.workArea1().isBlank())
            throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.INVALID_WORK_AREA);
        if (report.subs() == null || report.subs().isBlank())
            throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.INVALID_SUBCONTRACTOR);
    }

    private void checkWeather(Report report) {
        logger.info("[Report Service] Checking weather...");
        if (report.weather() == null || report.weather().isBlank()) {
            logger.info("[Report Service] Weather not specified.");
            if (report.reportDate().isBefore(LocalDate.now(ZoneId.of("America/New_York"))))
                throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.WEATHER_NOT_SPECIFIED);
            logger.info("[Report Service] Fetching weather...");
            Weather weather = weatherService.getTodaysWeather();
            if (weather == null)
                throw new ReportSubmissionException(ReportSubmissionException.ExceptionType.WEATHER_NOT_FOUND);
            else report.setWeather(weather.toString());
        }
    }
}
