package com.preservinc.production.djr.exception.report;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "R01")
public class InvalidReportDateException extends RuntimeException {}
