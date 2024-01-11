package com.preservinc.production.djr.dao;

import com.preservinc.production.djr.dao.jobs.JobsDAO;
import com.preservinc.production.djr.dao.teams.ITeamsDAO;
import com.preservinc.production.djr.model.Employee;
import com.preservinc.production.djr.model.job.Job;
import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.model.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class JobsDAOTest {

    private final DataSource dataSource;
    private final ITeamsDAO teamsDAO;
    private JobsDAO jobsDAO;

    static class TeamsDAOMock implements ITeamsDAO {
        @Override
        public Team getTeam(int teamID, boolean includeJobs) {
            return new Team(1, new Employee(2, null, null, null,
                    null, null, null, null));
        }
    }

    @Autowired
    public JobsDAOTest(DataSource dataSource) {
        this.dataSource = dataSource;
        this.teamsDAO = new TeamsDAOMock();
    }

    @BeforeEach
    void setUp() {
        this.jobsDAO = new JobsDAO(dataSource, teamsDAO);
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
            assertEquals(1, job.team().getTeamID());
        });
    }
}
