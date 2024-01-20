package com.preservinc.production.djr.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class AccessKeyException extends Exception {
    public AccessKeyException() {
        super();
    }

    public AccessKeyException(String message) {
        super(message);
    }

    public AccessKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessKeyException(Throwable cause) {
        super(cause);
    }
}
