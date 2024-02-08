package app.brecks.service.job;

import app.brecks.dao.jobs.IJobsDAO;
import app.brecks.exception.BadRequestException;
import app.brecks.exception.DatabaseException;
import app.brecks.exception.ServerException;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStats;
import app.brecks.model.job.JobStatus;
import app.brecks.request.job.CreateJobSiteRequest;
import app.brecks.request.job.StatusChangeRequest;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;

@Service
public class JobService implements IJobService {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[Job Service]");
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

        logger.info(marker, """
                Searching for jobs with the following filters:
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
        logger.info(marker, "Retrieving job with id {}", id);
        try {
            return this.jobsDAO.getJob(id);
        } catch (SQLException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public boolean changeJobStatus(int id, StatusChangeRequest request) {
        logger.info(marker, "Changing job status for job with id {} to {}", id, request);

        try {
            this.jobsDAO.updateJobStatus(id, request.getStatus(), request.getStartDate(), request.getEndDate());
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }

        return true;
    }

    @Override
    public boolean createJobSite(CreateJobSiteRequest request) {
        logger.info(marker, "Creating jobsite at address {}", request.getAddress());

        JobStatus status;

        if (request.getStatus() == null) {
            if (request.getStartDate().isAfter(LocalDate.now(ZoneId.of("America/New_York")))) status = JobStatus.NOT_STARTED;
            else status = JobStatus.ACTIVE;
        } else status = JobStatus.of(request.getStatus());

        logger.info(marker, "Setting status to: {}", status);

        try {
            jobsDAO.insertJob(request.getAddress(), null, request.getStartDate(), request.getTeamID(), status);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public JobStats getStats(int id, @NonNull String basis, String value) {
        logger.info(marker, "Getting stats for job id `{}` with basis `{}` and value `{}`", id, basis, value);

        // todo implement getStats for range of dates

        LocalDate startDate, endDate, currentDate = LocalDate.now(ZoneId.of("America/New_York"));

        if (value == null) {
            switch (basis) {
                case "ytd" -> {
                    endDate = currentDate;
                    startDate = endDate.withDayOfYear(1);
                }
                case "lifetime" -> {
                    startDate = null;
                    endDate = null;
                }
                case "week" -> {
                    startDate = currentDate.with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.getValue());
                    endDate = startDate.plusDays(4);
                }
                default -> throw new BadRequestException();
            }
        } else {
            try {
                startDate = LocalDate.parse(value);
                endDate = switch (basis) {
                    case "week" -> {
                        if (!startDate.getDayOfWeek().equals(DayOfWeek.MONDAY)) throw new BadRequestException();
                        yield startDate.plusDays(4);
                    }
                    case "month" -> {
                        if (startDate.getDayOfMonth() != 1) throw new BadRequestException();
                        yield startDate.plusMonths(1).minusDays(1);
                    }
                    case "year" -> {
                        if (startDate.getDayOfYear() != 1) throw new BadRequestException();
                        yield startDate.withDayOfYear(startDate.isLeapYear() ? 366 : 365);
                    }
                    default -> throw new BadRequestException();
                };
            } catch (DateTimeParseException e) {
                logger.error(marker, "An error occurred parsing date from value `{}`: {}", value, e.getMessage());
                logger.error(e);
                throw new BadRequestException();
            }
        }

        if (endDate != null && endDate.isAfter(currentDate)) endDate = currentDate;

        try {
            return this.jobsDAO.getStats(id, startDate, endDate, false, false);
        } catch (SQLException e) {
            logger.error(e);
            throw new DatabaseException();
        }
    }
}
