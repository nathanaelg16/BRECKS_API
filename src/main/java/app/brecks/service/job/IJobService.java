package app.brecks.service.job;

import app.brecks.exception.ServerException;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStats;
import app.brecks.model.job.JobStatus;
import app.brecks.request.job.CreateJobSiteRequest;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public interface IJobService {
    List<Job> searchUsingFilters(Map<String, String> params) throws ServerException;

    Job getJob(int id) throws ServerException;

    boolean createJobSite(CreateJobSiteRequest request);

    boolean changeJobStatus(int id, JobStatus status);

    JobStats getStats(int id, @NonNull String basis, String value);
}
