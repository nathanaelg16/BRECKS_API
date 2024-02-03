package com.preservinc.production.djr.controller;

import com.preservinc.production.djr.exception.BadRequestException;
import com.preservinc.production.djr.exception.ServerException;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.weather.IWeatherService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {
    private static final Logger logger = LogManager.getLogger();
    private final IWeatherService weatherService;

    @Autowired
    public WeatherController(IWeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<Weather> getWeather(@RequestParam Map<String, String> params) {
        logger.info("[Weather Controller] Received request for weather with params: {}", params);
        Weather weather = null;
        if (params.isEmpty()) weather = this.weatherService.getCurrentWeather();
        else if (params.containsKey("date")) {
            LocalDate date = LocalDate.parse(params.get("date"));
            weather = this.weatherService.getWeatherOnDate(date);
        } else throw new BadRequestException();

        if (weather != null) return ResponseEntity.ok(weather);
        else throw new ServerException();
    }
}
