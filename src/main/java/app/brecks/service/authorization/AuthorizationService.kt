package app.brecks.service.authorization

import app.brecks.auth.jwt.AuthorizationToken
import app.brecks.auth.jwt.RevokedTokens
import app.brecks.dao.auth.IAuthenticationDAO
import app.brecks.dao.employees.IEmployeeDAO
import app.brecks.exception.DatabaseException
import app.brecks.exception.auth.*
import app.brecks.model.auth.SignInMethod
import app.brecks.model.auth.User
import app.brecks.model.auth.UserStatus
import app.brecks.request.auth.UserLoginRequest
import app.brecks.request.auth.UserRegistrationRequest
import app.brecks.request.employee.AddEmployeeRequest
import app.brecks.response.RegistrationDetailsResponse
import app.brecks.service.email.IEmailService
import app.brecks.util.Constants
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CharacterPredicates
import org.apache.commons.text.RandomStringGenerator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.sql.SQLException
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Service
open class AuthorizationService @Autowired constructor(
    private val emailService: IEmailService,
    private val authenticationDAO: IAuthenticationDAO,
    private val employeeDAO: IEmployeeDAO,
    private val secretKey: SecretKey,
    private val revokedTokens: RevokedTokens,
    private val jwtParser: JwtParser,
    @Value("\${authentication.password.pepper}") private val pepper: String,
) : IAuthorizationService {
    override fun authenticateUser(userLoginRequest: UserLoginRequest): AuthorizationToken? {
        logger.info("[Auth Service] Attempting to authenticate user {}", userLoginRequest.username)
        if (!userLoginRequest.isWellFormed) return null
        val user = authenticationDAO.findUser(userLoginRequest.username) ?: return null

        if (user.status == UserStatus.LOCKED) {
            authenticationDAO.loginAttempt(user.id, SignInMethod.PASSWORD, false, "Account locked")
            throw AccountLockedException()
        } else if (user.status == UserStatus.INACTIVE) {
            authenticationDAO.loginAttempt(user.id, SignInMethod.PASSWORD, false, "Account disabled")
            throw AccountDisabledException()
        }

        val salt = user.salt; val userHash = user.password
        val hash = hashPassword(salt, userLoginRequest.password)
        val validLogin = compareHashes(hash, userHash)
        authenticationDAO.loginAttempt(user.id, SignInMethod.PASSWORD, validLogin, if (validLogin) null else "Invalid password")
        return if (validLogin)
            generateAuthorizationToken(user)
        else null
    }

    override fun registerUser(userRegistrationRequest: UserRegistrationRequest): AuthorizationToken? {
        logger.info("[Auth Service] Registering user {}", userRegistrationRequest.username)

        if (!userRegistrationRequest.isWellFormed) throw RegistrationException()

        var userEmail: String
        val rsg = RandomStringGenerator.Builder().filteredBy(CharacterPredicates.ASCII_ALPHA_NUMERALS).build()
        do {
            userEmail = rsg.generate(6, 12).plus("@brecks.cc");
        } while (authenticationDAO.checkForUserRegistration(userEmail))

        val addEmployeeRequest = AddEmployeeRequest(userRegistrationRequest.firstName, userRegistrationRequest.lastName, "Demo User", userEmail, false)
        this.employeeDAO.createEmployee(addEmployeeRequest)

        return registerUser(userRegistrationRequest, userEmail)
    }

    override fun registerUser(userRegistrationRequest: UserRegistrationRequest, userEmail: String): AuthorizationToken? {
        logger.info("[Auth Service] Registering user {} with email {}", userRegistrationRequest.username, userEmail)
        if (!userRegistrationRequest.isWellFormed) throw RegistrationException()
        if (authenticationDAO.checkForUserRegistration(userEmail)) throw AlreadyRegisteredException()
        if (!validatePassword(userRegistrationRequest.password!!)) throw InvalidPasswordException()
        val salt = generateSalt()
        val hash = hashPassword(salt, userRegistrationRequest.password!!)
        try {
            authenticationDAO.registerUser(
                userEmail,
                userRegistrationRequest.username!!,
                userRegistrationRequest.displayName!!,
                hash,
                salt
            )
        } catch (e: SQLException) {
            logger.error(e)
            throw DatabaseException()
        }

        return generateAuthorizationToken(authenticationDAO.findUser(userRegistrationRequest.username!!))
    }

    override fun checkUnique(username: String): Boolean {
        logger.info("[Auth Service] Checking if username is unique...")
        if (username.isBlank()) throw RegistrationException()
        else return authenticationDAO.isUsernameUnique(username)
    }

    override fun validatePassword(password: String): Boolean {
        logger.info("[Auth Service] Performing password validations")
        val minLength = password.length >= Constants.MIN_PASSWORD_LENGTH
        val maxLength = password.length <= Constants.MAX_PASSWORD_LENGTH
        val specialChar = StringUtils.containsAny(password, "!@#$%^&*()-+=.,/?';\"[]{}:><")
        val numbers = password.contains("[0-9]+".toRegex())
        val lowercase = password.contains("[a-z]+".toRegex())
        val uppercase = password.contains("[A-Z]+".toRegex())
        return minLength && maxLength && specialChar && numbers && lowercase && uppercase
    }

    override fun setPassword(user: String, password: String) {
        logger.info("[Auth Service] Setting password for user: {}", user)
        if (password.isNotBlank() && validatePassword(password)) {
            val salt = generateSalt()
            val hash = hashPassword(salt, password)
            authenticationDAO.setPassword(user, hash, salt)
            authenticationDAO.unlockAccount(user)
        } else throw IllegalArgumentException("Password does not meet requirements.")
    }

    override fun hashPassword(salt: String, password: String): ByteArray {
        val pepperedPassword = pepperPassword(password)
        val saltedPassword = saltPassword(pepperedPassword, salt)
        return MessageDigest.getInstance("SHA-256").digest(saltedPassword.toByteArray())
    }

    override fun logout(authToken: AuthorizationToken): Boolean {
        return this.revokedTokens.add(authToken)
    }

    override fun generateSalt(): String {
        return RandomStringGenerator.Builder().filteredBy(CharacterPredicates.ASCII_ALPHA_NUMERALS).build()
            .generate(8, 20)
    }

    override fun compareHashes(hash: ByteArray, userHash: ByteArray): Boolean {
        logger.info("[Auth Service] Comparing password hashes")
        return hash.contentEquals(userHash)
    }

    override fun generateAuthorizationToken(user: User): AuthorizationToken {
        logger.info("[Auth Service] Generating JWT Auth Token for user: {}", user.username)
        return generateAuthorizationToken(user.username, user.id)
    }

    override fun reissueAuthorizationToken(authToken: AuthorizationToken) : AuthorizationToken {
        val jwsToken = jwtParser.parseSignedClaims(authToken.token)
        val body = jwsToken.payload
        logger.info("[Auth Service] Issuing a new JWT Auth Token for user: {}", body.subject)
        return generateAuthorizationToken(body.subject, (body["userID"] as Int))
    }

    private fun generateAuthorizationToken(username: String, userID: Int) : AuthorizationToken {
        return AuthorizationToken(
            Jwts.builder()
                .subject(username)
                .claim("userID", userID)
                .issuer("BRECKS Authorization Service")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(Constants.DEFAULT_TIMEOUT)))
                .signWith(secretKey)
                .compact()
        )
    }

    override fun saltPassword(password: String, salt: String): String {
        val mid = salt.length / 2

        return StringBuilder()
            .append(salt.substring(0, mid))
            .append(password)
            .append(salt.substring(mid))
            .toString()
    }

    override fun pepperPassword(password: String): String {
        val pass = StringUtils.reverse(password)
        val mid = pass.length / 2

        return StringBuilder()
            .append(pass.substring(0, mid))
            .append(pepper)
            .append(pass.substring(mid))
            .toString()
    }

    override fun resetPassword(userIdentification: String, identificationType: String) {
        if (userIdentification.isBlank()) throw Exception("Invalid user identification")
        val user = when (identificationType) {
            "email" -> this.authenticationDAO.findUserByEmail(userIdentification)
            "username" -> this.authenticationDAO.findUser(userIdentification)
            else -> throw Exception("Invalid identification type")
        }
        if (user == null) throw NoSuchUserException(userIdentification)
        else emailService.sendPasswordResetEmail(user.email)
    }

    override fun getPreloadedRegistrationDetails(email: String): RegistrationDetailsResponse {
        val employee = this.employeeDAO.findEmployeeByEmail(email)
        return RegistrationDetailsResponse(employee.getEmail(), employee.getFirstName(), employee.getLastName(), employee.getRole())
    }

    companion object {
        private val logger: Logger = LogManager.getLogger()
    }
}

// todo implement differentiation between admin users and non-admin users