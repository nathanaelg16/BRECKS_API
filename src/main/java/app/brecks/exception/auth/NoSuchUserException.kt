package app.brecks.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT, reason = "A user with the specified identifier was not found.")
class NoSuchUserException(msg: String) : RuntimeException(msg)