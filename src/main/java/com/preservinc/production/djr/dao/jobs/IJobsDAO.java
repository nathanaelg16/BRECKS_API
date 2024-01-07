package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.model.job.Job;

import java.sql.SQLException;

public interface IJobsDAO {
    String getJobAddress(int jobID) throws SQLException;
    Job getJob(int id) throws SQLException;
}
