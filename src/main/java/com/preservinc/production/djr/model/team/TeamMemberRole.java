package com.preservinc.production.djr.model.team;

import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum TeamMemberRole {
    PROJECT_MANAGER("PM"),
    ASSISTANT_PROJECT_MANAGER("APM"),
    PROJECT_SUPERVISOR("PS"),
    FOREMAN("FM"),
    OTHER("OTHER");

    private static final Map<String, TeamMemberRole> map = new HashMap<>(values().length, 1);

    static {
        for (TeamMemberRole role : values()) map.put(role.role, role);
    }

    private final String role;

    TeamMemberRole(String role) {
        this.role = role;
    }

    public static TeamMemberRole of(@NonNull String role) {
        TeamMemberRole teamMemberRole = map.get(role);

        if (teamMemberRole == null) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        return teamMemberRole;
    }

    @Override
    public String toString() {
        return this.role;
    }
}
