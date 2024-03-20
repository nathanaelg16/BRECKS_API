package app.brecks.service.team;

import app.brecks.dao.jobs.IJobsDAO;
import app.brecks.dao.teams.ITeamsDAO;
import app.brecks.exception.DatabaseException;
import app.brecks.model.team.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class TeamService implements ITeamService {
    private static final Logger logger = LogManager.getLogger();
    private final ITeamsDAO teamsDAO;
    private final IJobsDAO jobsDAO;

    @Autowired
    public TeamService(ITeamsDAO teamsDAO, IJobsDAO jobsDAO) {
        this.teamsDAO = teamsDAO;
        this.jobsDAO = jobsDAO;
    }

    @Override
    public List<Team> getTeams() {
        logger.traceEntry("getTeams()");
        try {
            return this.teamsDAO.getTeams();
        } catch (SQLException e) {
            logger.error(e);
            throw new DatabaseException();
        }
    }

    @Override
    public Team getTeam(int id) {
        logger.traceEntry("getTeams(id={})", id);
        try {
            return this.teamsDAO.getTeam(id, true);
        } catch (SQLException e) {
            logger.error(e);
            throw new DatabaseException();
        }
    }
}
