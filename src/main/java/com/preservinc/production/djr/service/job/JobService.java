package com.preservinc.production.djr.service.job;

import com.preservinc.production.djr.dao.jobs.IJobsDAO;
import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.request.job.CreateJobSiteRequest;
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
        String startDateAfterString = params.get("startDateAfter");
        String startDateBeforeString = params.get("startDateBefore");
        String endDateAfterString = params.get("endDateAfter");
        String endDateBeforeString = params.get("endDateBefore");
        String statusString = params.get("status");

        logger.info("""
                [Job Service] searching for jobs with the following filters:
                teamID: {}
                startDateAfter: {}
                startDateBefore: {}
                endDateAfter: {}
                endDateBefore: {}
                status: {}""", teamIDString, startDateAfterString, startDateBeforeString, endDateAfterString, endDateBeforeString, statusString);

        try {
            Integer teamID = (teamIDString != null && !teamIDString.isBlank()) ? Integer.valueOf(teamIDString) : null;
            LocalDate startDateAfter = (startDateAfterString != null && !startDateAfterString.isBlank()) ? LocalDate.parse(startDateAfterString) : null;
            LocalDate startDateBefore = (startDateBeforeString != null && !startDateBeforeString.isBlank()) ? LocalDate.parse(startDateBeforeString) : null;
            LocalDate endDateAfter = (endDateAfterString != null && !endDateAfterString.isBlank()) ? LocalDate.parse(endDateAfterString) : null;
            LocalDate endDateBefore = (endDateBeforeString != null && !endDateBeforeString.isBlank()) ? LocalDate.parse(endDateBeforeString) : null;
            JobStatus status = (statusString != null && !statusString.isBlank()) ? JobStatus.of(statusString) : null;
            return this.jobsDAO.search(teamID, startDateAfter, startDateBefore, endDateAfter, endDateBefore, status);
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
    public boolean changeJobStatus(int id, JobStatus status) {
        logger.info("[Job Service] Changing job status for job with id {} to {}", id, status);

        try {
            this.jobsDAO.updateJobStatus(id, status);
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
