package com.preservinc.production.djr.service.weather;

import com.preservinc.production.djr.model.weather.Weather;

import java.time.LocalDate;

public interface IWeatherService {
    Weather getTodaysWeather();
    Weather getWeatherOnDate(LocalDate date);
}
