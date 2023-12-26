package com.preservinc.production.djr.service.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preservinc.production.djr.exception.WeatherAPIException;
import com.preservinc.production.djr.model.weather.CurrentWeather;
import com.preservinc.production.djr.model.weather.HistoricWeather;
import com.preservinc.production.djr.model.weather.Weather;
import com.preservinc.production.djr.service.email.IEmailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class WeatherService implements IWeatherService {
    private static final Logger logger = LogManager.getLogger();
    private final IEmailService emailService;
    private final Properties config;
    private final HttpClient client;
    private LocalDateTime lastUpdate;
    private Weather weather;
    private boolean hasExceededRateLimit;
    private LocalDateTime rateLimitExceededTimestamp;

    @Autowired
    public WeatherService(IEmailService emailService, Properties config) {
        this.emailService = emailService;
        this.config = config;
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
        this.fetchWeather();
    }

    public Weather getTodaysWeather() {
        if (lastUpdate == null) this.fetchWeather();
        if (lastUpdate.isBefore(LocalDateTime.now().minusHours(1))) this.fetchWeather();
        return weather;
    }

    public Weather getWeatherOnDate(LocalDate date) {
        logger.info("[Weather Service] Fetching weather for date %s...".formatted(date.toString()));

        String CONDITION_ENDPOINT = config.getProperty("weather.timeMachine.condition.endpoint");
        String MIN_MAX_TEMP_ENDPOINT = config.getProperty("weather.timeMachine.minMaxTemp.endpoint");
        String LAT = config.getProperty("weather.lat");
        String LON = config.getProperty("weather.lon");
        String UNITS = config.getProperty("weather.units");
        String APPID = config.getProperty("weather.appid");
        String DATETIME = String.valueOf(ZonedDateTime.of(date.atTime(12, 0, 0), ZoneId.of("America/New_York")).toEpochSecond());
        String TZ = ZoneId.of("America/New_York").getRules().getOffset(date.atTime(12, 0, 0)).toString();

        HttpRequest weatherConditionRequest = HttpRequest.newBuilder().uri(URI.create(String.format("%s?lat=%s&lon=%s&dt=%s&units=%s&appid=%s", CONDITION_ENDPOINT, LAT, LON, DATETIME, UNITS, APPID))).timeout(Duration.ofSeconds(10)).GET().build();

        HttpRequest minMaxTempRequest = HttpRequest.newBuilder().uri(URI.create(String.format("%s?lat=%s&lon=%s&date=%s&tz=%s&appid=%s&units=%s&lang=%s", MIN_MAX_TEMP_ENDPOINT, LAT, LON, date.format(DateTimeFormatter.ISO_LOCAL_DATE), TZ, APPID, UNITS, "en"))).timeout(Duration.ofSeconds(10)).GET().build();

        Function<String, JsonNode> responseHandler = json -> {
            try {
                return new ObjectMapper().readTree(json);
            } catch (JsonProcessingException e) {
                logger.error("[Weather Service] Failed to parse weather response: %s".formatted(json));
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<Object> weatherConditionResponse = sendAsyncRequest(weatherConditionRequest, responseHandler);
        CompletableFuture<Object> minMaxTempResponse = sendAsyncRequest(minMaxTempRequest, responseHandler);

        HistoricWeather weather = new HistoricWeather();

        try {
            JsonNode weatherConditionData = (JsonNode) weatherConditionResponse.get();
            JsonNode weatherData = weatherConditionData.get("data").get(0);

            List<String> descriptions = new ArrayList<>();
            weatherData.get("weather").elements().forEachRemaining(node -> descriptions.add(node.get("description").asText()));
            weather.setDescription(String.join(",", descriptions));
            weather.setTimestamp(weatherData.get("dt").asLong());
        } catch (RuntimeException | ExecutionException e) {
            if (e.getCause() instanceof WeatherAPIException || e.getCause() instanceof JsonProcessingException) {
                logger.error("[Weather Service] Failed to fetch weather conditions: %s".formatted(e.getMessage()));
                emailService.notifySysAdmin(e.getCause());
            }
            weather.setDescription(null);
        } catch (InterruptedException e) {
            logger.error("[Weather Service] Failed to fetch weather conditions: %s".formatted(e.getMessage()));
            weather.setDescription(null);
        }

        try {
            JsonNode minMaxTempData = (JsonNode) minMaxTempResponse.get();
            JsonNode tempData = minMaxTempData.get("temperature");
            weather.setMinTemp(tempData.get("min").asDouble());
            weather.setMaxTemp(tempData.get("max").asDouble());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WeatherAPIException) {
                logger.error("[Weather Service] Failed to fetch temperature statistics: %s".formatted(e.getMessage()));
                emailService.notifySysAdmin(e.getCause());
            }
            return null;
        } catch (InterruptedException e) {
            logger.error("[Weather Service] Failed to fetch temperature statistics: %s".formatted(e.getMessage()));
            return null;
        }
        return weather;
    }

    private void fetchWeather() {
        logger.info("[Weather Service] Fetching weather...");

        String ENDPOINT = config.getProperty("weather.endpoint");
        String LAT = config.getProperty("weather.lat");
        String LON = config.getProperty("weather.lon");
        String UNITS = config.getProperty("weather.units");
        String APPID = config.getProperty("weather.appid");

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format("%s?lat=%s&lon=%s&units=%s&appid=%s", ENDPOINT, LAT, LON, UNITS, APPID))).timeout(Duration.ofSeconds(10)).GET().build();

        Function<String, CurrentWeather> responseHandler = json -> {
            try {
                return new ObjectMapper().readValue(json, CurrentWeather.class);
            } catch (JsonProcessingException e) {
                logger.error("[Weather Service] Failed to parse weather response: %s".formatted(json));
                throw new RuntimeException(e);
            }
        };

        try {
            this.weather = (CurrentWeather) sendRequest(request, responseHandler);
        } catch (IOException | InterruptedException e) {
            this.weather = null;
        } catch (WeatherAPIException e) {
            logger.error("[Weather Service] Failed to fetch weather: %s".formatted(e.getMessage()));
            emailService.notifySysAdmin(e);
            this.weather = null;
        } catch (RuntimeException e) {
            logger.error("[Weather Service] Failed to fetch weather: %s".formatted(e.getMessage()));
            if (e.getCause() instanceof WeatherAPIException || e.getCause() instanceof JsonProcessingException) {
                emailService.notifySysAdmin(e.getCause());
            }
            this.weather = null;
        } finally {
            this.lastUpdate = LocalDateTime.now();
        }
    }

    private Object sendRequest(HttpRequest request, Function<String, ?> responseHandler) throws WeatherAPIException, IOException, InterruptedException {
        if (checkRateLimit()) throw new WeatherAPIException(WeatherAPIException.ExceptionType.RATE_LIMIT_EXCEEDED);

        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 500)
                throw new WeatherAPIException(WeatherAPIException.ExceptionType.SERVER_ERROR);
            switch (response.statusCode()) {
                case 400 -> throw new WeatherAPIException(WeatherAPIException.ExceptionType.BAD_REQUEST);
                case 401 -> throw new WeatherAPIException(WeatherAPIException.ExceptionType.UNAUTHORIZED);
                case 404 -> throw new WeatherAPIException(WeatherAPIException.ExceptionType.NOT_FOUND);
                case 429 -> {
                    this.setRateLimitedExceeded();
                    throw new WeatherAPIException(WeatherAPIException.ExceptionType.RATE_LIMIT_EXCEEDED);
                }
                default -> {
                    return responseHandler.apply(response.body());
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("[Weather Service] Failed to fetch weather: %s".formatted(e.getMessage()));
            throw e;
        }
    }

    private CompletableFuture<Object> sendAsyncRequest(HttpRequest request, Function<String, ?> responseHandler) {
        CompletableFuture<Object> response = new CompletableFuture<Object>().orTimeout(25, TimeUnit.SECONDS);

        if (checkRateLimit()) {
            response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.RATE_LIMIT_EXCEEDED));
            return response;
        }

        this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).handle((r, e) -> {
            if (e != null) response.completeExceptionally(e);
            else if (r.statusCode() >= 500)
                response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.SERVER_ERROR));
            else switch (r.statusCode()) {
                    case 400 ->
                            response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.BAD_REQUEST));
                    case 401 ->
                            response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.UNAUTHORIZED));
                    case 404 ->
                            response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.NOT_FOUND));
                    case 429 -> {
                        this.setRateLimitedExceeded();
                        response.completeExceptionally(new WeatherAPIException(WeatherAPIException.ExceptionType.RATE_LIMIT_EXCEEDED));
                    }
                    default -> {
                    }
                }
            return r;
        }).thenApply(HttpResponse::body).thenApply(responseHandler).exceptionally(e -> {
            response.completeExceptionally(e);
            return null;
        }).thenAccept(response::complete);
        return response;
    }

    private void setRateLimitedExceeded() {
        this.hasExceededRateLimit = true;
        this.rateLimitExceededTimestamp = LocalDateTime.now();
    }

    private boolean checkRateLimit() {
        if (!this.hasExceededRateLimit) return false;
        else if (this.rateLimitExceededTimestamp.isBefore(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))) {
            this.hasExceededRateLimit = false;
            this.rateLimitExceededTimestamp = null;
            return false;
        } else return true;
    }
}

// todo move Weather Service to One Call API, store weather in database and only fetch when new weather is needed
