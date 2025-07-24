package app.brecks.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT, reason = "User has already been verified.")
class AlreadyVerifiedException : RuntimeException()
