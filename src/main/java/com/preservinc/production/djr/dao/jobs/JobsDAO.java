package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.dao.teams.ITeamsDAO;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.model.team.Team;
import com.preservinc.production.djr.util.function.CheckedBiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
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
    public List<Job> search(final Integer teamID, final LocalDate startDateAfter,
                            final LocalDate startDateBefore, final LocalDate endDateAfter,
                            final LocalDate endDateBefore, final JobStatus status) throws SQLException {
        logger.info("""
                [JobsDAO] Searching for jobs with the following params:
                \tid: {}
                \tstartDateAfter: {}
                \tstartDateBefore: {}
                \tendDateAfter: {}
                \tendDateBefore: {}
                \tstatus: {}""", teamID, startDateAfter, startDateBefore, endDateAfter, endDateBefore, status);

        StringBuilder queryBuilder = new StringBuilder("select J.*, T.pm from Jobs J inner join Teams T on J.team_id = T.id where 1=1");
        List<CheckedBiConsumer<PreparedStatement, Integer, SQLException>> paramSetters = new ArrayList<>();

        if (teamID != null) {
            queryBuilder.append(" and J.team_id = ?");
            paramSetters.add((p, i) -> p.setInt(i, teamID));
        }

        if (startDateAfter != null) {
            queryBuilder.append(" and J.start_date >= ?");
            paramSetters.add((p, i) -> p.setDate(i, Date.valueOf(startDateAfter)));
        }

        if (startDateBefore != null) {
            queryBuilder.append(" and J.start_date <= ?");
            paramSetters.add((p, i) -> p.setDate(i, Date.valueOf(startDateBefore)));
        }

        if (endDateAfter != null) {
            queryBuilder.append(" and J.end_date >= ?");
            paramSetters.add((p, i) -> p.setDate(i, Date.valueOf(endDateAfter)));
        }

        if (endDateBefore != null) {
            queryBuilder.append(" and J.end_date <= ?");
            paramSetters.add((p, i) -> p.setDate(i, Date.valueOf(endDateBefore)));
        }

        if (status != null) {
            queryBuilder.append(" and status = ?");
            paramSetters.add((p, i) -> p.setString(i, status.getStatus()));
        }

        queryBuilder.append(";");

        List<Job> results = new ArrayList<>();

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement(queryBuilder.toString())
        ) {
            for (int i = 0; i < paramSetters.size();) paramSetters.get(i).accept(p, ++i);
            logger.info("SQL QUERY: {}", p);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Optional<Date> rStartDate = Optional.ofNullable(r.getDate("start_date"));
                    Optional<Date> rEndDate = Optional.ofNullable(r.getDate("end_date"));
                    results.add(new Job(r.getInt("id"),
                            r.getString("address"),
                            rStartDate.map(Date::toLocalDate).orElse(null),
                            rEndDate.map(Date::toLocalDate).orElse(null),
                            JobStatus.of(r.getString("status")),
                            new Team(r.getInt("team_id"),
                                    new Employee(r.getInt("pm"), null, null, null,
                                            null, null, null, null)
                            )
                    ));
                }
            }
        }

        return results;
    }

    @Override
    public JobStats getStats(int id, LocalDate startDate, LocalDate endDate) {
        logger.info("[Jobs DAO] Retrieving stats for job id `{}` with start date `{}` and end date `{}`", id, startDate, endDate);

        String p1_where_clause;
        if (startDate == null && endDate == null) p1_where_clause = ";"
        else if (startDate != null && endDate != null) p1_where_clause = " and reportDate between ? and ?;"

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement("select sum(crewSize) as total_man_power, avg(crewSize) as avg_man_power from Reports where job_id = ?%s".formatted(p1_where_clause));
             PreparedStatement p2 = c.prepareStatement("select reportDate from Reports where job_id = ?%s".formatted(p1_where_clause));
        ) {
            p1.setInt(1, id);
            p2.setInt(1, id);
            
            if (startDate != null) {
                p1.setDate(2, startDate);
                p2.setDate(2, startDate);
                p1.setDate(3, endDate);
                p2.setDate(3, endDate); 
            }
            
            try (ResultSet r1 = p1.executeQuery();
                 ResultSet r2 = p2.executeQuery();
            ) {
                Integer totalManDays = null;
                Double avgDailyManPower = null;
                if (r1.next()) {
                    totalManPower = r1.getInt("total_man_power");
                    avgManPower = r1.getDouble("avg_man_power");
                }
                
                Set<LocalDate> reportDates = new HashSet<>();
                while (r2.next()) reportDates.add(r2.getDate(1).toLocalDate());
                calculateMissingDates(startDate, endDate, reportDates);
                return new JobStats(totalManDays, avgDailyManPower, reportDates);
            }
        }
    }
    
    private void calculateMissingDates(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Set<LocalDate> dates) {
    Period period = Period.between(startDate, endDate);
    dates.remove(IntStream.builder()
        .range(0, period.getDays())
        .mapToObj((i) -> startDate.plus(i))
        .filter((date) -> date.getDayOfWeek() != DayOfWeek.SATURDAY)
        .filter((date) -> date.getDayOfWeek() != DayOfWeek.SUNDAY)
        .filter((date) -> dates.contains(date))
        .collect());
    }
}
