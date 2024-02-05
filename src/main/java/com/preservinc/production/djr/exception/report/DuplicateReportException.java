package com.preservinc.production.djr.exception.report;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "A report for this date already exists.")
public class DuplicateReportException extends RuntimeException {}
