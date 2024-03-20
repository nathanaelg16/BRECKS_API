package app.brecks.controller;

import app.brecks.model.job.Job;
import app.brecks.model.job.JobStats;
import app.brecks.request.job.CreateJobSiteRequest;
import app.brecks.request.job.StatusChangeRequest;
import app.brecks.service.job.IJobService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/jobs")
public class JobController {
    private static final Logger logger = LogManager.getLogger();

    private final IJobService jobService;

    @Autowired
    public JobController(IJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> listJobSites(@RequestParam Map<String, String> params) {
        logger.info("[Job Controller] Received request to list job sites, with params: {}", params);
        return ResponseEntity.ok(this.jobService.searchUsingFilters(params));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobDetails(@PathVariable Integer id) {
        logger.info("[Job Controller] Received request for details for job ID {}", id);

        Job job = this.jobService.getJob(Objects.requireNonNull(id));

        if (job != null) return ResponseEntity.ok(job);
        else return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/status/change")
    public ResponseEntity<Object> changeStatus(@PathVariable Integer id, @RequestBody StatusChangeRequest request) {
        logger.info("[Job Controller] Received change status request for JOB ID {}: Change to {}", id, request);
        if (!request.isWellFormed()) return ResponseEntity.badRequest().build();
        if (this.jobService.changeJobStatus(Objects.requireNonNull(id), request)) return ResponseEntity.ok().build();
        else return ResponseEntity.badRequest().build();
    }

    @PostMapping("/new")
    public ResponseEntity<Object> createJobSite(@RequestBody CreateJobSiteRequest request) {
        logger.info("[Job Controller] Received request to create a new job site: {}", request);
        if (!request.isWellFormed()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(this.jobService.createJobSite(request));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<JobStats> getJobStats(@PathVariable("id") Integer id, @RequestParam String basis, @RequestParam(required = false) String value) {
        logger.info("[Job Controller] Received request for the stats of job ID `{}` with basis `{}` and value `{}`", id, basis, value);
        return ResponseEntity.ok(this.jobService.getStats(id, basis, value));
    }
}
