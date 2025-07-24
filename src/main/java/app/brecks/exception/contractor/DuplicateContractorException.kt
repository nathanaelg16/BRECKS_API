package app.brecks.exception.contractor

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "A contractor with the same entity-name or short-name already exists.")
class DuplicateContractorException : RuntimeException() {}