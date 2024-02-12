package app.brecks.controller;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.model.report.Report;
import app.brecks.service.report.IReportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[Reports Controller]");

    private final IReportService reportService;

    @Autowired
    public ReportsController(IReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Object> submitNewJobReport(@RequestAttribute("token") AuthorizationToken authorizationToken, @RequestBody Report report) {
        logger.info(marker, "Received new job report for job ID {} submitted by {}", report.getJobID(), authorizationToken);
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

    @GetMapping("/exists")
    public ResponseEntity<Object> checkReportExists(@RequestParam("job") Integer job, @RequestParam("date") LocalDate date) {
        logger.info(marker, "Checking to see if a report exists for job `{}` on date `{}`", job, date);
        record Response(boolean exists) {}
        return ResponseEntity.ok(new Response(this.reportService.checkExists(job, date)));
    }

    @GetMapping
    public ResponseEntity<List<Report>> getReports(@RequestParam("job") Integer job, @RequestParam("startDate") LocalDate startDate, @RequestParam("endDate") LocalDate endDate) {
        logger.traceEntry("{} getReports(job={}, startDate={}, endDate={})", marker, job, startDate, endDate);
        return ResponseEntity.ok(this.reportService.getReports(job, startDate, endDate));
    }
}
