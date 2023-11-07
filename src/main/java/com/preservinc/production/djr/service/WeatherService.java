package com.preservinc.production.djr.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WeatherService {
    private static final Logger logger = LogManager.getLogger();

    private LocalDateTime lastUpdate;
    private String weather;

    public WeatherService() {
        this.fetchWeather();
    }

    public String getTodaysWeather() {
        if (lastUpdate == null) this.fetchWeather();
    }
}
