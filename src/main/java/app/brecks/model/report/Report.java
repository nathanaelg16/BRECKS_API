package app.brecks.model.report;

import app.brecks.model.employee.Employee;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id"})
public final class Report {
    @BsonId
    @JsonIgnore
    private ObjectId id;
    private int jobID;
    @JsonProperty("date")
    private LocalDate reportDate;
    private String weather;
    private Map<String, Integer> crew;
    private String visitors;
    private List<String> workDescriptions = List.of();
    private List<String> materials = List.of();
    private Employee reportBy;

    @JsonGetter("id")
    @BsonIgnore
    public String objectId() {
        return this.id.toString();
    }

    @JsonSetter("id")
    @BsonIgnore
    public void objectId(String id) throws IllegalArgumentException {
        this.id = new ObjectId(id);
    }

    @JsonProperty
    @BsonIgnore
    public int getTimestamp() {
        return this.id.getTimestamp();
    }

    @BsonProperty("crewSize")
    public int getCrewSize() {
        return this.crew.values().stream().reduce(0, Integer::sum);
    }

    @JsonIgnore
    @BsonIgnore
    public Report clearID() {
        this.id = null;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Report report = (Report) o;

        return new EqualsBuilder().append(jobID, report.jobID).append(id, report.id).append(reportDate, report.reportDate).append(weather, report.weather).append(crew, report.crew).append(visitors, report.visitors).append(workDescriptions, report.workDescriptions).append(materials, report.materials).append(reportBy, report.reportBy).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(jobID).append(reportDate).append(weather).append(crew).append(visitors).append(workDescriptions).append(materials).append(reportBy).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("jobID", jobID)
                .append("reportDate", reportDate)
                .append("weather", weather)
                .append("crew", crew)
                .append("crewSize", crew.values().stream().reduce(0, Integer::sum))
                .append("visitors", visitors)
                .append("workDescriptions", workDescriptions)
                .append("materials", materials)
                .append("reportBy", reportBy)
                .toString();
    }
}