package app.brecks.exception.email;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The email address provided could not be validated.")
public class InvalidEmailAddressException extends RuntimeException {}
