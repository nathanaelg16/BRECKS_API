package app.brecks.exception.report;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "R03")
public class InvalidCrewException extends RuntimeException {}