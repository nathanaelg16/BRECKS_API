package com.preservinc.production.djr.exception.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN, reason = "Exceeded maximum failed login attempts.")
class AccountLockedException : RuntimeException()
