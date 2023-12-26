package com.preservinc.production.djr;

import com.preservinc.production.djr.model.Report;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.email.IEmailService;
import com.preservinc.production.djr.service.weather.WeatherService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class WeatherServiceTest {
    private static final Logger logger = LogManager.getLogger();
    private WeatherService weatherService;

    @Autowired
    private Properties config;

    class EmailServiceMock implements IEmailService {
        @Override
        public void sendReportEmail(File report) {
            // TODO: Implement this
        }

        @Override
        public void sendReportSubmissionNotification(Report report) {
            // TODO: Implement this
        }

        @Override
        public void notifySysAdmin(Throwable ex) {
            // TODO: Implement this
            ex.printStackTrace();
        }
    }

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(new EmailServiceMock(), config);
    }

    @Test
    void testGetTodaysWeather() {
        LocalDateTime requestTimestamp = LocalDateTime.now(ZoneId.of("America/New_York"));
        Weather weather = weatherService.getTodaysWeather();
        logger.info("Today's weather: {}", weather);
        logger.info("Request timestamp: {} - Response timestamp: {}", requestTimestamp, weather.getTimestamp());
        assertTrue(weather.getTimestamp().isAfter(requestTimestamp.minusHours(1)));
        assertTrue(weather.getTimestamp().isBefore(requestTimestamp.plusMinutes(5)));
    }

    @Test
    void testGetWeatherOnDate() {
        LocalDate requestDate = LocalDate.of(2023, 12, 12);
        Weather weather = weatherService.getWeatherOnDate(requestDate);
        logger.info("Weather on {}: {}", requestDate, weather);
        logger.info("Response timestamp: {}", weather.getTimestamp());
        assertTrue(weather.getTimestamp().isAfter(ChronoLocalDateTime.from(requestDate.atStartOfDay(ZoneId.of("America/New_York")))));
    }
}
