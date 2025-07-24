package app.brecks.service.weather;

import app.brecks.model.weather.Weather;

import java.time.LocalDate;

public interface IWeatherService {
    Weather getCurrentWeather();
    Weather getWeatherOnDate(LocalDate date);
}
