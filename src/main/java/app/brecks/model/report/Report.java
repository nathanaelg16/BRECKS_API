package app.brecks.model.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import app.brecks.model.employee.Employee;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Report {
    private ObjectId id;
    private int jobID;
    private LocalDate reportDate;
    private String weather;
    private Map<String, Integer> crew;
    private String visitors;
    private List<String> workDescriptions;
    private List<String> materials;
    private Employee reportBy;

    @BsonProperty("crewSize")
    public int getCrewSize() {
        return this.crew.values().stream().reduce(0, Integer::sum);
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