package app.brecks.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST, reason = "A search parameter has been duplicated.")
class DuplicateSearchParameterException(param: String) : RuntimeException(param) {}
