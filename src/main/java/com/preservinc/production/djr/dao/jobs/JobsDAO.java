package com.preservinc.production.djr.dao.jobs;

import com.preservinc.production.djr.dao.teams.ITeamsDAO;
import com.preservinc.production.djr.exception.DatabaseException;
import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStats;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.model.job.JobStatusHistory;
import com.preservinc.production.djr.model.team.Team;
import com.preservinc.production.djr.model.time.Interval;
import com.preservinc.production.djr.util.function.CheckedBiConsumer;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

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
    public void insertJob(String address, String identifier, LocalDate startDate, int teamID, JobStatus status) throws SQLException {
        logger.info("[JobsDAO] Inserting new job...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("call new_job_site(?, ?, ?, ?, ?);")
        ) {
            p.setString(1, address);
            p.setString(2, identifier);
            p.setInt(3, teamID);
            p.setDate(4, Date.valueOf(startDate));
            p.setString(5, status.getStatus());
            p.executeUpdate();
        }
    }

    @Override
    public void updateJobStatus(Integer id, JobStatus status) throws SQLException {
        logger.info("[JobsDAO] Updating job status...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("call set_job_status(?, ?, ?, null);")
        ) {
            p.setInt(1, id);
            p.setString(2, status.getStatus());
            p.setDate(3, Date.valueOf(LocalDate.now(ZoneId.of("America/New_York"))));
            p.executeUpdate();
        }
    }

    @Override
    public void updateJobStatus(Integer id, JobStatus status, LocalDate startDate, LocalDate endDate) throws SQLException {
        logger.info("[JobsDAO] Updating job status...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("call set_job_status(?, ?, ?, ?);")
        ) {
            p.setInt(1, id);
            p.setString(2, status.getStatus());
            p.setDate(3, Date.valueOf(startDate));
            p.setDate(4, Date.valueOf(endDate));
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
    public JobStats getStats(int id, LocalDate startDate, LocalDate endDate, boolean countSaturdays, boolean countSundays, boolean countHolidays) throws SQLException {
        logger.info("[Jobs DAO] Retrieving stats for job id `{}` with start date `{}` and end date `{}`", id, startDate, endDate);

        String date_filter_clause;
        if (startDate == null && endDate == null) date_filter_clause = "";
        else if (startDate != null && endDate != null) date_filter_clause = " and reportDate between ? and ?";
        else throw new RuntimeException("Start Date and End Date must be both null or neither null");

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p1 = c.prepareStatement(("select sum(R.crewSize) as total_man_power, avg(R.crewSize) " +
                     "as avg_man_power from Reports R inner join Jobs J on R.job_id = J.id where J.id = ?%s;").formatted(date_filter_clause));
             PreparedStatement p2 = c.prepareStatement(("select reportDate from Reports " +
                     "where job_id = ?%s order by reportDate;").formatted(date_filter_clause));
             PreparedStatement p3 = c.prepareStatement("call get_job_statuses_on_date_range(?, ?, ?);")
        ) {
            p1.setInt(1, id);
            p2.setInt(1, id);
            
            if (startDate != null) {
                p1.setDate(2, Date.valueOf(startDate));
                p2.setDate(2, Date.valueOf(startDate));
                p1.setDate(3, Date.valueOf(endDate));
                p2.setDate(3, Date.valueOf(endDate));
            } else {
                try (PreparedStatement p4 = c.prepareStatement("select valid_start from JobStatus where job_id = ? " +
                        "and transaction_end > current_timestamp() and status = 'ACTIVE' order by valid_start limit 1;");
                     PreparedStatement p5 = c.prepareStatement("select valid_end from JobStatus where job_id = ? " +
                             "and transaction_end > current_timestamp() and (status = 'ACTIVE' or status = 'COMPLETED') " +
                             "order by valid_end desc limit 1;")
                ) {
                    p4.setInt(1, id);
                    p5.setInt(1, id);

                    try (ResultSet r4 = p4.executeQuery(); ResultSet r5 = p5.executeQuery()) {
                        if (r4.next()) startDate = r4.getDate("valid_start").toLocalDate();
                        if (r5.next()) endDate = r5.getDate("valid_end").toLocalDate();
                        assert startDate != null;
                        assert endDate != null;
                        if (endDate.equals(LocalDate.of(9999, 12, 31)))
                            endDate = LocalDate.now(ZoneId.of("America/New_York"));
                    }
                }
            }

            p3.setInt(1, id);
            p3.setDate(2, Date.valueOf(startDate));
            p3.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet r1 = p1.executeQuery(); ResultSet r2 = p2.executeQuery(); ResultSet r3 = p3.executeQuery()) {
                int totalManDays;
                double avgDailyManPower;

                if (r1.next()) {
                    totalManDays = r1.getInt("total_man_power");
                    avgDailyManPower = r1.getDouble("avg_man_power");

                    Set<LocalDate> reportDates = new HashSet<>();
                    while (r2.next()) reportDates.add(r2.getDate(1).toLocalDate());

                    JobStatusHistory.Builder jobStatusHistoryBuilder = new JobStatusHistory.Builder();
                    while (r3.next()) jobStatusHistoryBuilder = jobStatusHistoryBuilder
                            .addInterval(JobStatus.of(r3.getString("status")),
                                    Interval.between(r3.getDate("valid_start").toLocalDate(),
                                                    r3.getDate("valid_end").toLocalDate()));

                    List<LocalDate> missingReportDates = calculateMissingDates(startDate, endDate, reportDates, jobStatusHistoryBuilder.build(), countSaturdays, countSundays, countHolidays);

                    return new JobStats(totalManDays, avgDailyManPower, missingReportDates);
                }
            }
        }

        throw new DatabaseException();
    }
    
    private List<LocalDate> calculateMissingDates(@NonNull LocalDate startDate,
                                              @NonNull LocalDate endDate,
                                              @NonNull Set<LocalDate> dates,
                                              @NonNull JobStatusHistory jobStatusHistory,
                                              @NonNull Set<LocalDate> holidays,
                                              boolean countSaturdays, boolean countSundays, boolean countHolidays) {
    Period period = Period.between(startDate, endDate);
    List<Interval> activeIntervals = jobStatusHistory.getActiveIntervals();
    List<Interval> completedIntervals = jobStatusHistory.getCompletedIntervals();
    Predicate<LocalDate> saturdayFilter = countSaturdays ? (date) -> true : (date) -> date.getDayOfWeek() != DayOfWeek.SATURDAY;
    Predicate<LocalDate> sundayFilter = countSundays ? (date) -> true : (date) -> date.getDayOfWeek() != DayOfWeek.SUNDAY;
    Predicate<LocalDate> holidayFilter = countHolidays ? (date) -> true : (date) -> !holidays.contains(date);
    return IntStream.range(0, period.getDays())
            .parallel()
            .mapToObj(startDate::plusDays)
            .filter(saturdayFilter)
            .filter(sundayFilter)
            .filter(holidayFilter)
            .filter((date) -> !dates.contains(date))
            .filter((date) -> activeIntervals.stream().parallel().anyMatch((interval) -> interval.contains(date))
                    || completedIntervals.stream().parallel().anyMatch((interval) -> interval.contains(date)))
            .toList();
    }
}

// todo disallow use of NOT_STARTED job status