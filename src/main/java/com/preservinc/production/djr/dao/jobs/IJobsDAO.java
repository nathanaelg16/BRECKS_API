package com.preservinc.production.djr.dao.jobs;

import java.sql.SQLException;

public interface IJobsDAO {
    String getJobAddress(int jobID) throws SQLException;
}
