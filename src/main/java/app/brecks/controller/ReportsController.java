package app.brecks.controller;

import app.brecks.auth.jwt.AuthorizationToken;
import app.brecks.exception.BadRequestException;
import app.brecks.model.report.Report;
import app.brecks.model.report.SummarizedReport;
import app.brecks.service.report.IReportService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<Report>> getReports(@RequestParam Map<String, String> params) {
        logger.traceEntry("{} getReports(params={})", marker, params);
        if (params.containsKey("id")) return ResponseEntity.ok(List.of(this.reportService.getReport(params.get("id"))));
        else {
            try {
                Integer job = Integer.parseInt(params.get("job"));
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                LocalDate endDate = LocalDate.parse(params.get("endDate"));
                return ResponseEntity.ok(this.reportService.getReports(job, startDate, endDate));
            } catch (NumberFormatException | DateTimeParseException | NullPointerException e) {
                logger.error("{} Could not parse params: {}", marker, params);
                throw new BadRequestException();
            }
        }
    }

    @PutMapping
    public ResponseEntity<Object> putReport(@RequestAttribute("token") AuthorizationToken authorizationToken, @RequestBody Report report) {
        logger.info(marker, "Received updated job report for job ID {} submitted by {}", report.getJobID(), authorizationToken);
        try {
            reportService.updateReport(authorizationToken, report);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summarized")
    public ResponseEntity<List<SummarizedReport>> getSummarizedReports(@RequestParam("job") Integer job, @RequestParam("startDate") LocalDate startDate, @RequestParam("endDate") LocalDate endDate) {
        logger.traceEntry("{} getSummarizedReports(job={}, startDate={}, endDate={})", marker, job, startDate, endDate);
        return ResponseEntity.ok(this.reportService.getSummarizedReports(job, startDate, endDate));
    }
}
