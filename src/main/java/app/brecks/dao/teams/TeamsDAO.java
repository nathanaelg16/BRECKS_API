package app.brecks.dao.teams;

import app.brecks.dao.jobs.IJobsDAO;
import app.brecks.model.employee.EmployeeStatus;
import app.brecks.model.team.Team;
import app.brecks.model.team.TeamMember;
import app.brecks.model.team.TeamMemberRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeamsDAO implements ITeamsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;
    private final IJobsDAO jobsDAO;

    @Autowired
    public TeamsDAO(DataSource dataSource, @Lazy IJobsDAO jobsDAO) {
        this.dataSource = dataSource;
        this.jobsDAO = jobsDAO;
    }

    @Override
    public List<Team> getTeams() throws SQLException {
        logger.traceEntry("getTeams()");

        List<Team> teams = new ArrayList<>();

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select id from Teams;");
             ResultSet r = p.executeQuery()
        ) {
            while (r.next()) {
                teams.add(this.getTeam(r.getInt("id"), true));
            }
        }

        return teams;
    }

    @Override
    public Team getTeam(int teamID, boolean includeJobs) throws SQLException {
        logger.traceEntry("getTeam(teamID={}, includeJobs={})", teamID, includeJobs);

        Team team = null;
        List<TeamMember> teamMembers = new ArrayList<>();

        try (Connection c = dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("WITH team_pm AS (SELECT T.pm FROM Teams T WHERE T.id = ?) " +
                     "SELECT E.id, E.first_name, E.last_name, E.display_name, " +
                            "E.role AS employee_role, E.email, E.admin, TM.role AS team_member_role, " +
                            "(SELECT IF(E.id = pm, 1, 0) FROM team_pm) AS is_pm, E.status " +
                     "FROM TeamMembers TM " +
                     "INNER JOIN Employees E ON E.id = TM.member_id " +
                     "WHERE TM.team_id = ?;")
        ) {
            p1.setInt(1, teamID);
            p1.setInt(2, teamID);

            try (ResultSet r1 = p1.executeQuery()) {
                while (r1.next()) {
                    TeamMember teamMember = new TeamMember(r1.getInt("id"),
                            r1.getString("first_name"),
                            r1.getString("last_name"),
                            r1.getString("display_name"),
                            r1.getString("employee_role"),
                            r1.getString("email"),
                            r1.getBoolean("admin"),
                            EmployeeStatus.valueOf(r1.getString("status")),
                            TeamMemberRole.of(r1.getString("team_member_role"))
                    );
                    if (r1.getBoolean("is_pm")) team = new Team(teamID, teamMember);
                    teamMembers.add(teamMember);
                }
            }
        }

        if (team == null) return null; // this should never happen
        else team.setTeamMembers(teamMembers);

        if (includeJobs)
            team.setAssignedJobs(this.jobsDAO.search(teamID, null, null, null, null, null));

        return team;
    }
}
