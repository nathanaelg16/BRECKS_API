package app.brecks.dao.teams;

import app.brecks.model.team.Team;

import java.sql.SQLException;
import java.util.List;

public interface ITeamsDAO {
    List<Team> getTeams() throws SQLException;
    Team getTeam(int teamID, boolean includeJobs) throws SQLException;
}
