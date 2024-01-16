package com.preservinc.production.djr.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND, reason = "A registration for the specified user was not found.")
class RegistrationNotFoundException : RuntimeException()
