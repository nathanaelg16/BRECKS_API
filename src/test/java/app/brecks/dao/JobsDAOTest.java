package app.brecks.dao;

import app.brecks.dao.jobs.JobsDAO;
import app.brecks.dao.teams.ITeamsDAO;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStatus;
import app.brecks.model.team.Team;
import app.brecks.model.team.TeamMember;
import app.brecks.model.team.TeamMemberRole;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class JobsDAOTest {

    private final DataSource dataSource;
    private final MongoDatabase mongoDatabase;
    private final ITeamsDAO teamsDAO;
    private JobsDAO jobsDAO;

    // todo rewrite tests

    static class TeamsDAOMock implements ITeamsDAO {

        @Override
        public List<Team> getTeams() throws SQLException {
            return null;
        }

        @Override
        public Team getTeam(int teamID, boolean includeJobs) {
            return new Team(1, new TeamMember(2, null, null, null,
                    null, null, null, null, TeamMemberRole.PROJECT_MANAGER));
        }
    }

    @Autowired
    public JobsDAOTest(DataSource dataSource, MongoDatabase database) {
        this.dataSource = dataSource;
        this.mongoDatabase = database;
        this.teamsDAO = new TeamsDAOMock();
    }

    @BeforeEach
    void setUp() {
        this.jobsDAO = new JobsDAO(dataSource, mongoDatabase, teamsDAO);
    }

    @Test
    void testSearch() {
        assertDoesNotThrow(() -> {
            assertEquals(1, this.jobsDAO.search(1, null, null, null, null, JobStatus.ON_HOLD).size());
            assertEquals(5, this.jobsDAO.search(1, null, null, null, null, JobStatus.ACTIVE).size());
            assertEquals(17, this.jobsDAO.search(null, null, null, null, null, null).size());
            assertEquals(2, this.jobsDAO.search(null, LocalDate.of(2023, 9, 1), null, null, null, null).size());
            assertEquals(1, this.jobsDAO.search(null, null, null, null, LocalDate.of(2023, 10, 16), null).size());
        });
    }

    @Test
    void testGetJobAddress() {
        assertDoesNotThrow(() -> assertEquals("601 W 110th St", this.jobsDAO.getJobAddress(1)));
    }

    @Test
    void testGetJob() {
        assertDoesNotThrow(() -> {
            Job job = this.jobsDAO.getJob(1);
            assertEquals("601 W 110th St", job.address());
            assertEquals(1, job.team().getId());
        });
    }
}
