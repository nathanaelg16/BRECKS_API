package app.brecks.model.team;

import app.brecks.model.Views;
import app.brecks.model.job.Job;
import app.brecks.model.job.JobStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonView({Views.Default.class})
public class Team {
    private final int id;
    private final TeamMember projectManager;

    @JsonView({Views.Team.class})
    private List<TeamMember> teamMembers;

    @JsonView({Views.Team.class})
    private List<Job> assignedJobs;

    public Team(int id, TeamMember projectManager) {
        this.id = id;
        this.projectManager = projectManager;
    }

    @JsonView({Views.Teams.class})
    public Long numJobsActive() {
        if (assignedJobs != null)
            return assignedJobs.stream().filter((job) -> job.status().equals(JobStatus.ACTIVE)).count();
        else return null;
    }

    @JsonView({Views.Teams.class})
    public Long numJobsOnHold() {
        if (assignedJobs != null)
            return assignedJobs.stream().filter((job) -> job.status().equals(JobStatus.ON_HOLD)).count();
        else return null;
    }

    @JsonView({Views.Teams.class})
    public Long numJobsNotStarted() {
        if (assignedJobs != null)
            return assignedJobs.stream().filter((job) -> job.status().equals(JobStatus.NOT_STARTED)).count();
        else return null;
    }

    @JsonIgnore
    public TeamMember findTeamMemberByID(int id) {
        return teamMembers.parallelStream()
                .filter(tm -> tm.getID() == id)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public List<TeamMember> findTeamMembersByRole(TeamMemberRole role) {
        return teamMembers.parallelStream()
                .filter(tm -> tm.teamRole().equals(role))
                .toList();
    }
}

