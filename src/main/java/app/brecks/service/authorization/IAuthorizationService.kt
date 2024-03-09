package app.brecks.service.authorization

import app.brecks.auth.jwt.AuthorizationToken
import app.brecks.model.auth.User
import app.brecks.request.auth.UserLoginRequest
import app.brecks.request.auth.UserRegistrationRequest
import app.brecks.response.RegistrationDetailsResponse

interface IAuthorizationService {
    fun authenticateUser(userLoginRequest: UserLoginRequest): AuthorizationToken?
    fun registerUser(userRegistrationRequest: UserRegistrationRequest, userEmail: String): AuthorizationToken?
    fun checkUnique(username: String): Boolean
    fun validatePassword(password: String): Boolean
    fun setPassword(user: String, password: String)
    fun hashPassword(salt: String, password: String): ByteArray
    fun logout(authToken: AuthorizationToken): Boolean
    fun generateSalt(): String
    fun compareHashes(hash: ByteArray, userHash: ByteArray): Boolean
    fun generateAuthorizationToken(user: User): AuthorizationToken
    fun saltPassword(password: String, salt: String): String
    fun pepperPassword(password: String): String
    fun resetPassword(userIdentification: String, identificationType: String)
    fun getPreloadedRegistrationDetails(email: String): RegistrationDetailsResponse
    fun reissueAuthorizationToken(authToken: AuthorizationToken) : AuthorizationToken
}