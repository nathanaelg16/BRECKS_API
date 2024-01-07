package com.preservinc.production.djr.dao.teams;

import com.preservinc.production.djr.model.team.Team;

import java.sql.SQLException;

public interface ITeamsDAO {

    Team getTeam(int teamID, boolean includeJobs) throws SQLException;
}
