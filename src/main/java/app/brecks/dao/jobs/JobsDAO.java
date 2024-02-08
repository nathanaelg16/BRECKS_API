package app.brecks.dao.jobs;

import app.brecks.dao.teams.ITeamsDAO;
import app.brecks.exception.DatabaseException;
import app.brecks.model.employee.Employee;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStats;
import app.brecks.model.job.JobStatus;
import app.brecks.model.job.JobStatusHistory;
import app.brecks.model.team.Team;
import app.brecks.model.time.Interval;
import app.brecks.reactive.Subscriber;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Subscription;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public class JobsDAO implements IJobsDAO {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[Jobs DAO]");
    private final DataSource dataSource;
    private final MongoDatabase mongoDB;
    private final ITeamsDAO teamsDAO;

    @Autowired
    public JobsDAO(DataSource dataSource, MongoDatabase mongoDB, ITeamsDAO teamsDAO) {
        this.dataSource = dataSource;
        this.mongoDB = mongoDB;
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
             PreparedStatement p1 = c.prepareStatement("select *, " +
                     "job_start_date(id) as start_date, " +
                     "job_last_active_date(id) as end_date, " +
                     "current_job_status(id) as status from Jobs where id = ?;")
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
                            r.getString("identifier"),
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
    public void updateJobStatus(@NonNull Integer id, @NonNull JobStatus status, @NonNull LocalDate startDate, LocalDate endDate) throws SQLException {
        logger.info("[JobsDAO] Updating job status...");
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("call set_job_status(?, ?, ?, ?);")
        ) {
            p.setInt(1, id);
            p.setString(2, status.getStatus());
            p.setDate(3, Date.valueOf(startDate));

            if (endDate == null) p.setNull(4, Types.DATE);
            else p.setDate(4, Date.valueOf(endDate));

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

        List<Job> results = new ArrayList<>();

        try (Connection c = this.dataSource.getConnection();
             CallableStatement cs = c.prepareCall("call search_jobs(?, ?, ?, ?, ?, ?);")
        ) {
            Function<LocalDate, Date> toSQLDate = (localdate) -> localdate == null ? null : Date.valueOf(localdate);
            cs.setObject(1, teamID, Types.INTEGER);
            cs.setDate(2, toSQLDate.apply(startDateAfter));
            cs.setDate(3, toSQLDate.apply(startDateBefore));
            cs.setDate(4, toSQLDate.apply(endDateAfter));
            cs.setDate(5, toSQLDate.apply(endDateBefore));
            cs.setString(6, status == null ? null : status.getStatus());
            try (ResultSet r = cs.executeQuery()) {
                while (r.next()) {
                    Optional<Date> rStartDate = Optional.ofNullable(r.getDate("start_date"));
                    Optional<Date> rEndDate = Optional.ofNullable(r.getDate("end_date"));
                    results.add(new Job(r.getInt("id"),
                            r.getString("identifier"),
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

        logger.info("[Jobs DAO] Search returned {} results", results.size());
        return results;
    }

    @Override
    public JobStats getStats(int id, LocalDate startDate, LocalDate endDate, boolean countSaturdays, boolean countSundays) throws SQLException {
        logger.info("[Jobs DAO] Retrieving stats for job id `{}` with start date `{}` and end date `{}`", id, startDate, endDate);

        if (!Boolean.logicalOr(Boolean.logicalAnd(startDate == null, endDate == null), Boolean.logicalAnd(startDate != null, endDate != null)))
            throw new RuntimeException("Start Date and End Date must be both null or neither null");

        @Getter
        @AllArgsConstructor
        class AggregateInformation {
            private final Integer totalManDays;
            private final Double avgManPower;
            private final Set<LocalDate> reportDates;
        }

        class AggregateSubscriber extends Subscriber<AggregateInformation, Document> {
            private static final Logger logger = LogManager.getLogger();
            private static final Marker marker = MarkerManager.getMarker("[Aggregate Subscriber]");

            public AggregateSubscriber(CompletableFuture<AggregateInformation> future) {
                super(future);
            }

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Document document) {
                logger.traceEntry("{} onNext(Document={})", marker.getName(), document);
                super.future.complete(new AggregateInformation(document.getInteger("totalManDays"),
                        document.getDouble("avgManPower"),
                        new HashSet<>(document.getList("reportDates", java.util.Date.class)
                                .stream().map((date) -> date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate())
                                .collect(Collectors.toSet()))));
            }

            @Override
            public void onError(Throwable t) {
                logger.traceEntry("{} onError(Throwable={})", marker.getName(), t);
                super.future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                logger.traceEntry("{} onComplete()", marker.getName());
                super.future.complete(new AggregateInformation(0, 0.0, new HashSet<>()));
            }
        }

        List<BsonField> accumulators = new ArrayList<>(3);
        accumulators.add(Accumulators.sum("totalManDays", "$crewSize"));
        accumulators.add(Accumulators.avg("avgManPower", "$crewSize"));
        accumulators.add(Accumulators.addToSet("reportDates", "$reportDate"));

        Bson jobIDFilter = Filters.eq("jobID", id);
        Bson matchFilters = startDate == null ? jobIDFilter : Filters.and(jobIDFilter, Filters.gte("reportDate", startDate), Filters.lte("reportDate", endDate));

        List<Bson> stages = new ArrayList<>(2);
        stages.add(Aggregates.match(matchFilters));
        stages.add(Aggregates.group(null, accumulators));

        CompletableFuture<AggregateInformation> future = new CompletableFuture<>();
        MongoCollection<Document> collection = this.mongoDB.getCollection("reports");
        collection.aggregate(stages).subscribe(new AggregateSubscriber(future));

        try (Connection c = this.dataSource.getConnection();
             CallableStatement c1 = c.prepareCall("call get_job_statuses_on_date_range(?, ?, ?);")
        ) {

            if (startDate == null) {
                try (PreparedStatement p3 = c.prepareStatement("select job_start_date(?) as start_date, " +
                        "job_last_active_date(?) as last_active_date;")) {
                    p3.setInt(1, id);
                    p3.setInt(2, id);

                    try (ResultSet r3 = p3.executeQuery()) {
                        if (r3.next()) {
                            startDate = r3.getDate("start_date").toLocalDate();
                            endDate = r3.getDate("end_date").toLocalDate();
                        }
                    }
                }

                assert startDate != null;
                assert endDate != null;
                if (endDate.equals(LocalDate.of(9999, 12, 31)))
                    endDate = LocalDate.now(ZoneId.of("America/New_York"));
            }

            c1.setInt(1, id);
            c1.setDate(2, Date.valueOf(startDate));
            c1.setDate(3, Date.valueOf(endDate));

            try (ResultSet rc1 = c1.executeQuery()) {
                JobStatusHistory.Builder jobStatusHistoryBuilder = new JobStatusHistory.Builder();

                while (rc1.next()) jobStatusHistoryBuilder = jobStatusHistoryBuilder
                        .addInterval(JobStatus.of(rc1.getString("status")),
                                Interval.between(rc1.getDate("valid_start").toLocalDate(),
                                        rc1.getDate("valid_end").toLocalDate()));

                AggregateInformation aggregateInformation = future.get();

                JobStats stats = new JobStats(aggregateInformation.getTotalManDays(),
                        aggregateInformation.getAvgManPower(),
                        calculateMissingDates(startDate, endDate, aggregateInformation.getReportDates(),
                                jobStatusHistoryBuilder.build(), countSaturdays, countSundays));

                logger.info("[Jobs DAO] Stats for Job #{}: \n{}", id, stats);
                return stats;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("[Jobs DAO] An error occurred getting aggregate info from MongoDB...");
            logger.error(e);
        }

        throw new DatabaseException();
    }

    private static List<LocalDate> calculateMissingDates(@NonNull LocalDate startDate,
                                                  @NonNull LocalDate endDate,
                                                  @NonNull Set<LocalDate> dates,
                                                  @NonNull JobStatusHistory jobStatusHistory,
                                                  boolean countSaturdays, boolean countSundays) {
        // todo implement filtering of company holidays
        logger.traceEntry("{} calculateMissingDates(startDate={}, endDate={}, dates={}, jobStatusHistory={}, countSaturdays={}. countSundays={})", marker.getName(), startDate, endDate, dates, jobStatusHistory, countSaturdays, countSundays);
        Period period = Period.between(startDate, endDate.plusDays(1));
        List<Interval> activeIntervals = jobStatusHistory.getActiveIntervals();
        List<Interval> completedIntervals = jobStatusHistory.getCompletedIntervals();
        Predicate<LocalDate> saturdayFilter = countSaturdays ? (date) -> true : (date) -> date.getDayOfWeek() != DayOfWeek.SATURDAY;
        Predicate<LocalDate> sundayFilter = countSundays ? (date) -> true : (date) -> date.getDayOfWeek() != DayOfWeek.SUNDAY;
        return IntStream.range(0, period.getDays())
                .parallel()
                .mapToObj(startDate::plusDays)
                .filter(saturdayFilter)
                .filter(sundayFilter)
                .filter((date) -> !dates.contains(date))
                .filter((date) -> activeIntervals.stream().parallel().anyMatch((interval) -> interval.contains(date))
                        || completedIntervals.stream().parallel().anyMatch((interval) -> interval.contains(date)))
                .toList();
    }
}

// todo disallow use of NOT_STARTED job status