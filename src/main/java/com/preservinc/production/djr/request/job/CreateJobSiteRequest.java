package com.preservinc.production.djr.request.job;

import com.preservinc.production.djr.model.job.JobStatus;
import com.preservinc.production.djr.request.Request;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;

@Getter
@Setter
public class CreateJobSiteRequest implements Request {
    private String address;
    private LocalDate startDate;
    private Integer teamID;
    private String status;

    @Override
    public boolean isWellFormed() {
        if (this.address == null) return false;
        if (this.startDate == null) return false;
        if (this.teamID == null) return false;

        if (this.status != null) {
            try {
                JobStatus.of(status);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CreateJobSiteRequest that = (CreateJobSiteRequest) o;

        return new EqualsBuilder().append(getTeamID(), that.getTeamID()).append(getAddress(), that.getAddress()).append(getStartDate(), that.getStartDate()).append(getStatus(), that.getStatus()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getAddress()).append(getStartDate()).append(getTeamID()).append(getStatus()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("address", address)
                .append("startDate", startDate)
                .append("teamID", teamID)
                .append("status", status)
                .toString();
    }
}
