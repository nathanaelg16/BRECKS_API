package app.brecks.model.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentWeather extends Weather {

    @JsonProperty("dt")
    public void setTimestamp(Long timestamp) {
        super.setTimestamp(timestamp);
    }

    @JsonProperty("weather")
    public void setDescription(List<Map<String, Object>> weather) {
        List<String> descriptions = new ArrayList<>();
        for (Map<String, Object> w : weather) descriptions.add((String) w.get("description"));
        this.description = String.join(", ", descriptions).strip();
    }

    @JsonProperty("main")
    public void setMinMaxTemp(Map<String, Object> main) {
        this.minTemp = (Number) main.get("temp_min");
        this.maxTemp = (Number) main.get("temp_max");
    }
}
