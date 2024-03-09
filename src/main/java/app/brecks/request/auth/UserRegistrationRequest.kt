package app.brecks.request.auth

import app.brecks.request.Request
import org.apache.commons.lang3.StringUtils
import java.util.*

class UserRegistrationRequest : Request {
    var displayName: String? = null

    var username: String? = null
        set(value) {
            field = value?.lowercase(Locale.getDefault())
        }

    var password: String? = null

    constructor()

    constructor(displayName: String, username: String, password: String) {
        this.displayName = displayName
        this.username = username
        this.password = password
    }

    override fun isWellFormed(): Boolean {
        return booleanArrayOf(
            !this.displayName.isNullOrBlank(),
            !this.username.isNullOrBlank(),
            StringUtils.isAlphanumeric(username),
            !this.password.isNullOrBlank(),
        ).all { it }
    }
}