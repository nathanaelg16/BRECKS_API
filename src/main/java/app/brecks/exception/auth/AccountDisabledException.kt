package app.brecks.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN, reason = "Account has been disabled.")
class AccountDisabledException : RuntimeException()
