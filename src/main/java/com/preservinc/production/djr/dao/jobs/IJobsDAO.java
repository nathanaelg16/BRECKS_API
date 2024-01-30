package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStats;
import com.preservinc.production.djr.model.job.JobStatus;

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
    JobStats getStats(int id, LocalDate startDate, LocalDate endDate) throws SQLException;
}
