package com.preservinc.production.djr.dao.jobs;

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
public class JobsDAO implements IJobsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;

    @Autowired
    public JobsDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getJobAddress(int jobID) throws SQLException {
        logger.info("[JobsDAO] Getting job address for job ID {}", jobID);
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT address FROM Jobs WHERE jobID = ?;")
        ) {
            p.setInt(1, jobID);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getString(1);
                else return null;
            }
        }
    }

    @Override
    public Team getTeam(int jobID) throws SQLException {
        logger.info("[JobsDAO] Getting team for job ID {}", jobID);
        Team team = null;
        Map<Employee, TeamMemberRole> teamMembers = new HashMap<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("SELECT T.id, T.pm FROM Teams T INNER JOIN Jobs J ON J.team_id = T.id WHERE J.id = ?;");
             PreparedStatement p2 = c.prepareStatement("SELECT E.id, E.firebase_uid AS uid, E.first_name, E.last_name, E.display_name, E.role AS employee_role, E.email, E.admin, TM.role AS team_member_role FROM TeamMembers TM INNER JOIN Employees E ON E.id = TM.member_id WHERE TM.team_id = ?;")
        ) {
            p1.setInt(1, jobID);
            try (ResultSet r1 = p1.executeQuery()) {
                if (r1.next()) {
                    int teamID = r1.getInt("id");
                    int pmID = r1.getInt("pm");

                    p2.setInt(1, teamID);
                    try (ResultSet r2 = p2.executeQuery()) {
                         while (r2.next()) {
                            Employee employee = new Employee(r2.getInt("id"),
                                    r2.getString("uid"),
                                    r2.getString("first_name"),
                                    r2.getString("last_name"),
                                    r2.getString("display_name"),
                                    r2.getString("employee_role"),
                                    r2.getString("email"),
                                    r2.getBoolean("admin")
                            );
                            TeamMemberRole role = TeamMemberRole.of(r2.getString("team_member_role"));
                            if (employee.id() == pmID) team = new Team(teamID, employee);
                            teamMembers.put(employee, role);
                        }
                    }
                } else return null;
            }
        }

        if (team == null) return null; // this should never happen
        else team.setTeamMembers(teamMembers);

        return team;
    }
}
