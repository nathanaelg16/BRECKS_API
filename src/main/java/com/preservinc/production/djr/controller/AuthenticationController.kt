package com.preservinc.production.djr.controller

import com.preservinc.production.djr.auth.accesskey.AccessKey
import com.preservinc.production.djr.auth.accesskey.AccessKeyManager
import com.preservinc.production.djr.auth.jwt.AuthorizationToken
import com.preservinc.production.djr.exception.DatabaseException
import com.preservinc.production.djr.request.auth.UserLoginRequest
import com.preservinc.production.djr.request.auth.UserRegistrationRequest
import com.preservinc.production.djr.response.ErrorResponse
import com.preservinc.production.djr.response.RegistrationDetailsResponse
import com.preservinc.production.djr.service.authorization.IAuthorizationService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class AuthenticationController @Autowired constructor(private val authorizationService: IAuthorizationService, private val accessKeyManager: AccessKeyManager) {
    @PostMapping("/login")
    fun  login(@RequestBody userLogin: UserLoginRequest) : ResponseEntity<AuthorizationToken> {
        logger.info("[Auth Controller] Login request received for user: {}", userLogin.username)
        val token = this.authorizationService.authenticateUser(userLogin)
        return if (token == null) {
            logger.info("[Auth Controller] Unable to log in user {}", userLogin.username)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        } else {
            logger.info("[Auth Controller] User logged in successfully - returning JWT to user {}", userLogin.username)
            ResponseEntity.status(HttpStatus.OK).body(token)
        }
    }

    @PostMapping("/register")
    fun register(@RequestBody registration: UserRegistrationRequest, @RequestAttribute accessKey: AccessKey) : ResponseEntity<Any> {
        return try {
            val token = this.authorizationService.registerUser(registration, accessKey.email())
            if (token != null) {
                this.accessKeyManager.revokeAccessKey(accessKey)
                ResponseEntity.status(HttpStatus.OK).body(token)
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
            }
        } catch (e: DatabaseException) {
            logger.error(e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Unable to register user", "An unexpected database error occurred"))
        }
    }

    @PostMapping("/registration/checkUnique")
    fun checkUnique(@RequestBody username : String) : ResponseEntity<Any> {
        logger.info("[Auth Controller] Checking if username `{}` is unique", username)
        data class Response(val unique: Boolean)
        return ResponseEntity.ok().body(Response(authorizationService.checkUnique(username)))
    }

    @GetMapping("/registration/details")
    fun preloadedRegistrationDetails(@RequestAttribute accessKey: AccessKey) : ResponseEntity<RegistrationDetailsResponse> {
        logger.info("[Auth Controller] Fetching preloaded registration details for user identified by: {}", accessKey.email())
        return ResponseEntity.ok().body(authorizationService.getPreloadedRegistrationDetails(accessKey.email()))
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader(HttpHeaders.AUTHORIZATION) jwt: AuthorizationToken) : ResponseEntity<Any> {
        logger.info("[Auth Controller] Logging out user with jwt {}", jwt)
        return if (authorizationService.logout(jwt)) ResponseEntity.ok().build()
        else ResponseEntity.internalServerError().build()
    }

    @GetMapping("/resetPassword")
    fun resetPassword(@RequestParam("id") userIdentification: String, @RequestParam("type") identificationType: String) : ResponseEntity<Any> {
        logger.info("[Auth Controller] Received password reset GET request for user '{}' with identification type '{}'", identificationType, userIdentification)
        if (identificationType != "email" && identificationType != "username") return ResponseEntity.badRequest().body("Type must be either 'email' or 'username'")
        logger.info("[Auth Controller] Requesting a password reset for user with {}: {}", identificationType, userIdentification)
        authorizationService.resetPassword(userIdentification, identificationType)
        return ResponseEntity.accepted().build()
    }

    //@PostMapping("/resetPassword") todo implement more robust password resetting
    fun resetPassword(userLogin: UserLoginRequest) : ResponseEntity<Any> {
        logger.info("[Auth Controller] Received password reset POST request for user: {}", userLogin.username)
        authorizationService.setPassword(userLogin.username, userLogin.password)
        return ResponseEntity.accepted().build()
    }

    @GetMapping("/validate")
    fun validateUser() : ResponseEntity<Any> {
        logger.info("[Auth Controller] Validating JWT")
        return ResponseEntity.ok().build()
    }

    companion object {
        private val logger: Logger = LogManager.getLogger()
    }
}