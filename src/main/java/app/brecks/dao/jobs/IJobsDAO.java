package app.brecks.dao.jobs;

import app.brecks.model.job.Job;
import app.brecks.model.job.JobStats;
import app.brecks.model.job.JobStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface IJobsDAO {
    String getJobAddress(int jobID) throws SQLException;
    Job getJob(int id) throws SQLException;
    void insertJob(String address, String identifier, LocalDate startDate, int teamID, JobStatus status) throws SQLException;
    void updateJobStatus(Integer id, JobStatus status) throws SQLException;
    void updateJobStatus(Integer id, JobStatus status, LocalDate startDate, LocalDate endDate) throws SQLException;
    List<Job> search(Integer teamID, LocalDate startDateAfter, LocalDate startDateBefore, LocalDate endDateAfter,
                     LocalDate endDateBefore, JobStatus status) throws SQLException;
    JobStats getStats(int id, LocalDate startDate, LocalDate endDate, boolean countSaturdays, boolean countSundays) throws SQLException;
}
