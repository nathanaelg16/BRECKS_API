package com.preservinc.production.djr.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "All fields are required, and usernames must be unique.")
class RegistrationException : RuntimeException()
