package app.brecks.model.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@JsonPropertyOrder({"id"})
public class SummarizedReport {
    @JsonIgnore
    @BsonId
    private ObjectId id;

    @JsonProperty
    @BsonProperty("reportDate")
    private LocalDate date;

    @JsonIgnore
    private Map<String, Integer> crew;

    @JsonProperty("id")
    @BsonIgnore
    public String objectId() {
        return this.id.toString();
    }

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
