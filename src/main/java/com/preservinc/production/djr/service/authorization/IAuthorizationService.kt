package com.preservinc.production.djr.service.authorization

import com.preservinc.production.djr.auth.AuthorizationToken
import com.preservinc.production.djr.model.auth.User
import com.preservinc.production.djr.request.auth.UserLoginRequest
import com.preservinc.production.djr.request.auth.UserRegistrationRequest

interface IAuthorizationService {
    fun authenticateUser(userLoginRequest: UserLoginRequest): AuthorizationToken?
    fun registerUser(userRegistrationRequest: UserRegistrationRequest): AuthorizationToken?
    fun checkUnique(username: String): Boolean
    fun validatePassword(password: String): Boolean
    fun setPassword(user: String, password: String)
    fun hashPassword(salt: String, password: String): String
    fun logout(authToken: AuthorizationToken): Boolean
    fun hexEncode(str: String): String
    fun generateSalt(): String
    fun compareHashes(hash: String, userHash: String): Boolean
    fun generateAuthorizationToken(user: User): AuthorizationToken
    fun saltPassword(password: String, salt: String): String
    fun pepperPassword(password: String): String
    fun resetPassword(userIdentification: String, identificationType: String)
}