package com.preservinc.production.djr.controller;

import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.request.job.CreateJobSiteRequest;
import com.preservinc.production.djr.request.job.StatusChangeRequest;
import com.preservinc.production.djr.service.job.IJobService;
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
        if (this.jobService.changeJobStatus(Objects.requireNonNull(id), request.getStatus())) return ResponseEntity.ok().build();
        else return ResponseEntity.badRequest().build();
    }

    @PostMapping("/new")
    public ResponseEntity<Object> createJobSite(@RequestBody CreateJobSiteRequest request) {
        logger.info("[Job Controller] Received request to create a new job site: {}", request);
        if (!request.isWellFormed()) return ResponseEntity.badRequest().build();
        if (this.jobService.createJobSite(request)) return ResponseEntity.ok().build();
        else return ResponseEntity.badRequest().build();
    }
}
