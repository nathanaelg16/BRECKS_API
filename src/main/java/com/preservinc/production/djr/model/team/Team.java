package com.preservinc.production.djr.model.team;

import com.preservinc.production.djr.model.employee.Employee;
import com.preservinc.production.djr.model.job.Job;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Team {
    private final int teamID;
    private final Employee projectManager;
    private Map<Employee, TeamMemberRole> teamMembers;
    private List<Job> assignedJobs;

    public Team(int teamID, Employee projectManager) {
        this.teamID = teamID;
        this.projectManager = projectManager;
    }

    public Pair<Employee, TeamMemberRole> findTeamMemberByID(int id) {
        return teamMembers.keySet()
                .parallelStream()
                .filter(employee -> employee.id() == id)
                .findFirst()
                .map(employee -> Pair.of(employee, teamMembers.get(employee)))
                .orElse(null);
    }

    public List<Employee> findTeamMembersByRole(TeamMemberRole role) {
        return teamMembers.entrySet()
                .parallelStream()
                .filter(entry -> entry.getValue().equals(role))
                .map(HashMap.Entry::getKey)
                .toList();
    }
}

