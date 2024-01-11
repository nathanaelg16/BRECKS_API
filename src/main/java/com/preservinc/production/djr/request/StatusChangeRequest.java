package com.preservinc.production.djr.request;

import com.preservinc.production.djr.model.job.JobStatus;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Setter
public class StatusChangeRequest implements Request {
    private String status;

    public JobStatus getStatus() {
        return  JobStatus.of(status);
    }

    @Override
    public boolean isWellFormed() {
        if (status == null) return false;

        try {
            JobStatus.of(status);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        StatusChangeRequest that = (StatusChangeRequest) o;

        return new EqualsBuilder().append(status, that.status).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(status).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("status", status)
                .toString();
    }
}
