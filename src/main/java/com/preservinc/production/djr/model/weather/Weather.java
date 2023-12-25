package com.preservinc.production.djr.model.weather;

import lombok.Getter;

import java.time.*;

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

    @Override
    public String toString() {
        return String.format("L %d H %d %s", Math.round(minTemp.doubleValue()), Math.round(maxTemp.doubleValue()), description);
    }
}