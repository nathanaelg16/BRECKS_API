package app.brecks.controller;

import app.brecks.model.Views;
import app.brecks.model.team.Team;
import app.brecks.service.team.ITeamService;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private static final Logger logger = LogManager.getLogger();
    private final ITeamService teamService;

    public TeamController(ITeamService teamService) {
        this.teamService = teamService;
    }

    @JsonView(Views.Teams.class)
    @GetMapping
    public ResponseEntity<List<Team>> getTeams() {
        logger.traceEntry("getTeams()");
        return ResponseEntity.ok(this.teamService.getTeams());
    }
}
