package com.preservinc.production.djr.model.job;

import com.preservinc.production.djr.model.team.Team;
import lombok.NonNull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;

public record Job (int id, @NonNull String address, LocalDate startDate, LocalDate endDate, JobStatus status, Team team) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        return new EqualsBuilder().append(id, job.id).append(address, job.address).append(startDate, job.startDate).append(endDate, job.endDate).append(status, job.status).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(address).append(startDate).append(endDate).append(status).toHashCode();
    }

    @Override
    public String toString() {
        return this.address;
    }
}
