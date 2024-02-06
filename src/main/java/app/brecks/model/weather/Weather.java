package app.brecks.model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
public abstract class Weather {
    protected LocalDateTime timestamp;
    protected String description;
    protected Number minTemp;
    protected Number maxTemp;

    public Weather() {
        this.timestamp = LocalDateTime.MIN;
        this.description = "";
        this.minTemp = Double.NEGATIVE_INFINITY;
        this.maxTemp = Double.POSITIVE_INFINITY;
    }

    protected void setTimestamp(Long timestamp) {
        this.timestamp = Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("America/New_York")).toLocalDateTime();
    }

    @JsonProperty("summary")
    @Override
    public String toString() {
        return String.format("L %d H %d %s", Math.round(minTemp.doubleValue()), Math.round(maxTemp.doubleValue()), description);
    }
}