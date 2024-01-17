package com.preservinc.production.djr.request.auth

import com.preservinc.production.djr.request.Request
import org.apache.commons.lang3.StringUtils
import java.util.*

class UserRegistrationRequest : Request {
    var displayName: String? = null

    var email: String? = null
        set(value) {
            field = value?.lowercase(Locale.getDefault())
        }

    var username: String? = null
        set(value) {
            field = value?.lowercase(Locale.getDefault())
        }

    var password: String? = null

    constructor()

    constructor(displayName: String, email: String, username: String, password: String) {
        this.displayName = displayName
        this.email = email
        this.username = username
        this.password = password
    }

    override fun isWellFormed(): Boolean {
        return booleanArrayOf(
            !this.displayName.isNullOrBlank(),
            !this.username.isNullOrBlank(),
            StringUtils.isAlphanumeric(username),
            !this.password.isNullOrBlank(),
            !this.email.isNullOrBlank()
        ).all { it }
    }
}