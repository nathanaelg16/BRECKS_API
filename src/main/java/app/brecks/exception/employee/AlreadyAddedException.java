package app.brecks.exception.employee;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "This email is already in use by another user")
public class AlreadyAddedException extends RuntimeException {}
