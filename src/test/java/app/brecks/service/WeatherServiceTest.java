package app.brecks.service;

import app.brecks.model.employee.Employee;
import app.brecks.model.job.Job;
import app.brecks.model.report.Report;
import app.brecks.model.weather.Weather;
import app.brecks.service.email.IEmailService;
import app.brecks.service.weather.WeatherService;
import jakarta.mail.MessagingException;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class WeatherServiceTest {
    private static final Logger logger = LogManager.getLogger();
    private WeatherService weatherService;

    @Autowired
    private Environment env;

    static class EmailServiceMock implements IEmailService {
        @Override
        public void sendReportEmail(Employee author, Job job, LocalDate reportDate, File report) throws SQLException, IOException, MessagingException {
            logger.info("[TEST] [EmailServiceMock] Sending report email");
        }

        @Override
        public void sendReportSubmissionNotification(Report report, Job job) {
            logger.info("[TEST] [EmailServiceMock] Sending report submission notification email");
        }

        @Override
        public void notifySysAdmin(Throwable ex) {
            logger.info("[TEST] [EmailServiceMock] Notifying SysAdmin of error");
            logger.error(ex);
        }

        @Override
        public void sendPasswordResetEmail(@NonNull String email) {
            logger.info("[TEST] [EmailServiceMock] send password reset");
        }

        @Override
        public void notifyAccountCreation(String email) throws MessagingException, IOException {
            logger.info("[TEST] [EmailServiceMock] Notifying account creation");
        }
    }

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(new EmailServiceMock(), env);
    }

    @Test
    void testGetTodaysWeather() {
        LocalDateTime requestTimestamp = LocalDateTime.now(ZoneId.of("America/New_York"));
        Weather weather = weatherService.getCurrentWeather();
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
