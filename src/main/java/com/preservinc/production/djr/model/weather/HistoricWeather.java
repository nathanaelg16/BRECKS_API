package com.preservinc.production.djr.model.weather;

public class HistoricWeather extends Weather {
    public void setTimestamp(Long timestamp) {
        super.setTimestamp(timestamp);
    }

    public void setDescription(String description) {
        if (description == null) this.description = "";
        else this.description = description;
    }

    public void setMinTemp(Number minTemp) {
        this.minTemp = minTemp;
    }

    public void setMaxTemp(Number maxTemp) {
        this.maxTemp = maxTemp;
    }
}
