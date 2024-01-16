package com.preservinc.production.djr.service.job;

import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.request.job.CreateJobSiteRequest;

import java.util.List;
import java.util.Map;

public interface IJobService {
    List<Job> searchUsingFilters(Map<String, String> params) throws ServerException;

    Job getJob(int id) throws ServerException;

    boolean createJobSite(CreateJobSiteRequest request);

    boolean changeJobStatus(int id, JobStatus status);
}
