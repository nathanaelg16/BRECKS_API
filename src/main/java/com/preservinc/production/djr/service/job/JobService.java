package com.preservinc.production.djr.service.job;

import com.preservinc.production.djr.dao.jobs.IJobsDAO;
import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.request.CreateJobSiteRequest;
import com.preservinc.production.djr.request.StatusChangeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class JobService implements IJobService {
    private static final Logger logger = LogManager.getLogger();
    private final IJobsDAO jobsDAO;

    @Autowired
    public JobService(IJobsDAO jobsDAO) {
        this.jobsDAO = jobsDAO;
    }

    @Override
    public List<Job> searchUsingFilters(Map<String, String> params) throws ServerException {
        String teamIDString = params.get("teamID");
        String startDateString = params.get("startDate");
        String endDateString = params.get("endDate");
        String statusString = params.get("status");

        logger.info("""
                [Job Service] searching for jobs with the following filters:
                teamID: {}
                startDate: {}
                endDate: {}
                status: {}""", teamIDString, startDateString, endDateString, statusString);

        try {
            Integer teamID = (teamIDString != null && !teamIDString.isBlank()) ? Integer.valueOf(teamIDString) : null;
            LocalDate startDate = (startDateString != null && !startDateString.isBlank()) ? LocalDate.parse(startDateString) : null;
            LocalDate endDate = (endDateString != null && !endDateString.isBlank()) ? LocalDate.parse(endDateString) : null;
            JobStatus status = (statusString != null && !statusString.isBlank()) ? JobStatus.of(statusString) : null;
            return this.jobsDAO.search(teamID, startDate, endDate, status);
        } catch (SQLException | RuntimeException e) {
            logger.error(e);
            throw new ServerException(e);
        }
    }

    @Override
    public Job getJob(int id) throws ServerException {
        logger.info("[Job Service] Retrieving job with id {}", id);
        try {
            return this.jobsDAO.getJob(id);
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public boolean changeJobStatus(StatusChangeRequest request) {
        logger.info("[Job Service] Changing job status for job with id {} to {}", request.getId(), request.getStatus());

        JobStatus status = JobStatus.of(request.getStatus());

        try {
            this.jobsDAO.updateJobStatus(request.getId(), status);
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }

        return true;
    }

    @Override
    public boolean createJobSite(CreateJobSiteRequest request) {
        logger.info("[Job Service] Creating jobsite at address {}", request.getAddress());

        JobStatus status;

        if (request.getStatus() == null) {
            if (request.getStartDate().isAfter(LocalDate.now(ZoneId.of("America/New_York")))) status = JobStatus.NOT_STARTED;
            else status = JobStatus.ACTIVE;
        } else status = JobStatus.of(request.getStatus());

        logger.info("[Job Service] Setting status to: {}", status);

        try {
            jobsDAO.insertJob(request.getAddress(), request.getStartDate(), request.getTeamID(), status);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
