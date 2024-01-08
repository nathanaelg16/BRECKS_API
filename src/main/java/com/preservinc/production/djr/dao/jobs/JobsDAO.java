package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.dao.teams.ITeamsDAO;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.model.team.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Repository
public class JobsDAO implements IJobsDAO {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;
    private final ITeamsDAO teamsDAO;

    @Autowired
    public JobsDAO(DataSource dataSource, ITeamsDAO teamsDAO) {
        this.dataSource = dataSource;
        this.teamsDAO = teamsDAO;
    }

    @Override
    public String getJobAddress(int jobID) throws SQLException {
        logger.info("[JobsDAO] Getting job address for job ID {}", jobID);
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT address FROM Jobs WHERE id = ?;")
        ) {
            p.setInt(1, jobID);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getString(1);
                else return null;
            }
        }
    }

    @Override
    public Job getJob(int id) throws SQLException {
        logger.info("[JobsDAO] Getting job details for job ID {}", id);
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("SELECT * FROM Jobs WHERE id = ?;")
        ) {
            p1.setInt(1, id);
            try (ResultSet r = p1.executeQuery()) {
                if (r.next()) {
                    Team team = this.teamsDAO.getTeam(r.getInt("team_id"), false);

                    Function<Date, LocalDate> convertSQLDate = date -> {
                        if (date == null) return null;
                        else return date.toLocalDate();
                    };

                    return new Job(
                            id,
                            r.getString("address"),
                            convertSQLDate.apply(r.getDate("start_date")),
                            convertSQLDate.apply(r.getDate("end_date")),
                            JobStatus.of(r.getString("status")),
                            team
                    );
                } else {
                    logger.info("[JobsDAO] No such job found.");
                    return null;
                }
            }
        }
    }

    @Override
    public void insertJob(String address, LocalDate startDate, int teamID, JobStatus status) throws SQLException {
        logger.info("[JobsDAO] Inserting new job...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("insert into Jobs (address, team_id, start_date, status) value (?, ?, ?, ?);")
        ) {
            p.setString(1, address);
            p.setInt(2, teamID);
            p.setDate(3, Date.valueOf(startDate));
            p.setString(4, status.getStatus());
            p.executeUpdate();
        }
    }

    @Override
    public void updateJobStatus(Integer id, JobStatus status) throws SQLException {
        logger.info("[JobsDAO] Updating job status...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("update Jobs set status = ? where id = ?;")
        ) {
            p.setString(1, status.getStatus());
            p.setInt(2, id);
            p.executeUpdate();
        }
    }

    @Override
    public List<Job> search(Integer teamID, LocalDate startDate, LocalDate endDate, JobStatus status) throws SQLException {
        logger.info("""
                [JobsDAO] Searching for jobs with the following params:
                \tid: {}
                \tstartDate: {}
                \tendDate: {}
                \tstatus: {}""", teamID, startDate, endDate, status);

        String query = "select J.*, T.pm from Jobs J inner join Teams T on J.team_id = T.id";
        StringBuilder whereClauseBuilder = new StringBuilder();

        if (teamID != null) whereClauseBuilder.append("J.team_id = ? ");

        if (startDate != null) whereClauseBuilder.append("J.startDate >= ? ");

        List<Job> results = new ArrayList<>();
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement();
        ) {

        }
    }
}
