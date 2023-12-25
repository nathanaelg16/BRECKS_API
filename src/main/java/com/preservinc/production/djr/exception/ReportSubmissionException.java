package com.preservinc.production.djr.exception;

import lombok.Getter;

@Getter
public class ReportSubmissionException extends RuntimeException {
    public enum ExceptionType {
        INVALID_REPORT_DATE ("R01"),
        INVALID_JOB_SITE ("R02"),
        INVALID_WORK_AREA ("R03"),
        INVALID_SUBCONTRACTOR ("R04"),
        WEATHER_NOT_FOUND ("W01"),
        WEATHER_NOT_SPECIFIED ("W02");

        private final String code;

        ExceptionType(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    private final ExceptionType exceptionType;

    public ReportSubmissionException(ExceptionType exceptionType) {
        super(exceptionType.code());
        this.exceptionType = exceptionType;
    }
}
