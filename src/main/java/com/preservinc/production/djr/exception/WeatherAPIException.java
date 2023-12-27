package com.preservinc.production.djr.exception;

import lombok.Getter;

@Getter
public class WeatherAPIException extends Exception {
    public enum ExceptionType {
        BAD_REQUEST ("W-API-00"),
        UNAUTHORIZED ("W-API-01"),
        NOT_FOUND ("W-API-02"),
        RATE_LIMIT_EXCEEDED ("W-API-03"),
        SERVER_ERROR ("W-API-04");

        private final String code;

        ExceptionType(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    private final ExceptionType exceptionType;

    public WeatherAPIException(ExceptionType exceptionType) {
        super(exceptionType.code());
        this.exceptionType = exceptionType;
    }

    public WeatherAPIException(ExceptionType exceptionType, Throwable ex) {
        super(exceptionType.code(), ex);
        this.exceptionType = exceptionType;
    }
}
