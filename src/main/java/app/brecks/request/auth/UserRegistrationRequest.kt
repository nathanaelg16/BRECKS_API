package app.brecks.request.auth

import app.brecks.request.Request
import org.apache.commons.lang3.StringUtils
import java.util.*

class UserRegistrationRequest : Request {
    var displayName: String? = null
    var firstName: String? = null
    var lastName: String? = null

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

    constructor(firstName: String, lastName: String, displayName: String, username: String, password: String) {
        this.firstName = firstName
        this.lastName = lastName
        this.displayName = displayName
        this.username = username
        this.password = password
    }

    constructor(firstName: String, lastName: String, username: String, password: String) {
        this.firstName = firstName
        this.lastName = lastName
        this.displayName = firstName
        this.username = username
        this.password = password
    }

    override fun isWellFormed(): Boolean {
        if (this.displayName.isNullOrBlank()) this.displayName = this.firstName;

        return booleanArrayOf(
            !this.firstName.isNullOrBlank(),
            !this.lastName.isNullOrBlank(),
            !this.displayName.isNullOrBlank(),
            !this.username.isNullOrBlank(),
            StringUtils.isAlphanumeric(username),
            !this.password.isNullOrBlank(),
        ).all { it }
    }
}