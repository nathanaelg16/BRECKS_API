package com.preservinc.production.djr.request;

import com.preservinc.production.djr.model.job.JobStatus;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Getter
@Setter
public class StatusChangeRequest implements Request {
    private Integer id;
    private String status;

    @Override
    public boolean isWellFormed() {
        if (id == null) return false;
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

        return new EqualsBuilder().append(getId(), that.getId()).append(getStatus(), that.getStatus()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getId()).append(getStatus()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("status", status)
                .toString();
    }
}
