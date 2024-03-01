package app.brecks.model.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
public class SummarizedReport {
    @JsonProperty
    @BsonProperty("reportDate")
    private LocalDate date;

    @JsonIgnore
    private Map<String, Integer> crew;

    @JsonProperty
    public int getCrewSize() {
        return this.crew.values().stream().reduce(0, Integer::sum);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("date", date)
                .append("crew", crew)
                .toString();
    }
}
