package app.brecks.dao.teams;

import app.brecks.model.team.Team;

import java.sql.SQLException;

public interface ITeamsDAO {

    Team getTeam(int teamID, boolean includeJobs) throws SQLException;
}
