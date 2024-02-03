package com.preservinc.production.djr.model.job;

import com.preservinc.production.djr.model.team.Team;
import lombok.NonNull;

import java.time.LocalDate;

public record Job (int id, String identifier, @NonNull String address, LocalDate startDate, LocalDate endDate, @NonNull JobStatus status, @NonNull Team team) {
    @Override
    public String toString() {
        return "%s %s"
                .formatted(this.address, this.identifier == null ? "" : "(%s)".formatted(this.identifier))
                .strip();
    }
}
