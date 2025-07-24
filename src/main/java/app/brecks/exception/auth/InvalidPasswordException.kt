package app.brecks.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST, reason = "Password does not meet minimum security requirements.")
class InvalidPasswordException : RuntimeException()