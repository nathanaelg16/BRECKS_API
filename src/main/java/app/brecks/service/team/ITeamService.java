package app.brecks.service.team;

import app.brecks.model.team.Team;

import java.util.List;

public interface ITeamService {
    List<Team> getTeams();

    Team getTeam(int id);
}
