package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.model.team.Team;

import java.sql.SQLException;

public interface IJobsDAO {
    String getJobAddress(int jobID) throws SQLException;
    Team getTeam(int jobID) throws SQLException;
}
