package app.brecks.model.report;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public final class ReportHistories {
    private ObjectId id;
    private int jobID;
    private LocalDate reportDate;
    private List<Report> versions;

    public void setVersions(List<Report> versions) {
        this.versions = versions;
        versions.sort(Comparator.comparing(Report::getId).reversed());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ReportHistories that = (ReportHistories) o;

        return new EqualsBuilder().append(jobID, that.jobID).append(id, that.id).append(reportDate, that.reportDate).append(versions, that.versions).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(jobID).append(reportDate).append(versions).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("jobID", jobID)
                .append("reportDate", reportDate)
                .append("versions", versions)
                .toString();
    }
}