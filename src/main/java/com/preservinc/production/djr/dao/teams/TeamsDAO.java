package com.preservinc.production.djr.dao.teams;

import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.team.Team;
import com.preservinc.production.djr.model.team.TeamMemberRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Repository
public class TeamsDAO implements ITeamsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public TeamsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Team getTeam(int teamID, boolean includeJobs) throws SQLException {
        // todo add jobs to the team
        logger.info("[JobsDAO] Getting team with team ID {}", teamID);
        Team team = null;
        Map<Employee, TeamMemberRole> teamMembers = new HashMap<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("WITH team_pm AS (SELECT T.pm FROM Teams T WHERE T.id = ?) " +
                     "SELECT E.id, E.firebase_uid AS uid, E.first_name, E.last_name, E.display_name, " +
                            "E.role AS employee_role, E.email, E.admin, TM.role AS team_member_role, " +
                            "(SELECT IF(E.id = pm, 1, 0) FROM team_pm) AS is_pm " +
                     "FROM TeamMembers TM " +
                     "INNER JOIN Employees E ON E.id = TM.member_id " +
                     "WHERE TM.team_id = ?;")
        ) {
            p1.setInt(1, teamID);
            p1.setInt(2, teamID);
            try (ResultSet r1 = p1.executeQuery()) {
                while (r1.next()) {
                    Employee employee = new Employee(r1.getInt("id"),
                            r1.getString("uid"),
                            r1.getString("first_name"),
                            r1.getString("last_name"),
                            r1.getString("display_name"),
                            r1.getString("employee_role"),
                            r1.getString("email"),
                            r1.getBoolean("admin")
                    );
                    TeamMemberRole role = TeamMemberRole.of(r1.getString("team_member_role"));
                    if (r1.getBoolean("is_pm")) team = new Team(teamID, employee);
                    teamMembers.put(employee, role);
                }
            }
        }

        if (team == null) return null; // this should never happen
        else team.setTeamMembers(teamMembers);

        return team;
    }
}
