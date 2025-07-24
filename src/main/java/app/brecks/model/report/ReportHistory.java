package app.brecks.model.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public final class ReportHistory {
    @BsonId
    @JsonIgnore
    private ObjectId id;
    private int jobID;
    private LocalDate reportDate;
    private List<Report> history;

    @JsonProperty("id")
    @BsonIgnore
    public String objectId() {
        return this.id.toString();
    }

    public void setHistory(List<Report> history) {
        this.history = history;
        history.sort(Comparator.comparing(Report::getId).reversed());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ReportHistory that = (ReportHistory) o;

        return new EqualsBuilder().append(jobID, that.jobID).append(id, that.id).append(reportDate, that.reportDate).append(history, that.history).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(jobID).append(reportDate).append(history).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("jobID", jobID)
                .append("reportDate", reportDate)
                .append("history", history)
                .toString();
    }
}