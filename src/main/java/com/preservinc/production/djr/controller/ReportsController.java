package com.preservinc.production.djr.controller;

import com.preservinc.production.djr.auth.jwt.AuthorizationToken;
import com.preservinc.production.djr.model.report.Report;
import com.preservinc.production.djr.service.report.ReportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private static final Logger logger = LogManager.getLogger();

    private final ReportService reportService;

    @Autowired
    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Object> submitNewJobReport(@RequestAttribute("token") AuthorizationToken authorizationToken, @RequestBody Report report) {
        logger.info("[Reports Controller] Received new job report for job ID {} submitted by {}", report.getJobID(), authorizationToken);
        try {
            reportService.submitReport(authorizationToken, report);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
