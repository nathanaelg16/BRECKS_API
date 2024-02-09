package app.brecks.model.team;

import app.brecks.model.job.Job;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Team {
    private final int teamID;
    private final TeamMember projectManager;

    @JsonIgnore
    private List<TeamMember> teamMembers;

    @JsonIgnore
    private List<Job> assignedJobs;

    public Team(int teamID, TeamMember projectManager) {
        this.teamID = teamID;
        this.projectManager = projectManager;
    }

    public TeamMember findTeamMemberByID(int id) {
        return teamMembers.parallelStream()
                .filter(tm -> tm.id() == id)
                .findFirst()
                .orElse(null);
    }

    public List<TeamMember> findTeamMembersByRole(TeamMemberRole role) {
        return teamMembers.parallelStream()
                .filter(tm -> tm.teamRole().equals(role))
                .toList();
    }
}

