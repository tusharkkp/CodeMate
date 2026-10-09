package com.mit.tushar_kaldate.data.repository

import com.mit.tushar_kaldate.core.security.PasswordHasher
import com.mit.tushar_kaldate.data.local.dao.UserDao
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.prefs.SessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
open class AuthRepository(
    private val userDao: UserDao,
    private val sessionStore: SessionStore
) {

    open val isLoggedIn: Flow<Boolean> = sessionStore.isLoggedIn

    open val currentUser: Flow<UserEntity?> = sessionStore.userId.flatMapLatest { userId ->
        if (userId != null) {
            userDao.observeUserById(userId)
        } else {
            flowOf(null)
        }
    }

    open suspend fun register(name: String, email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be blank"))
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("Email is already registered"))
        }

        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(password, salt)
        val user = UserEntity(
            name = trimmedName,
            email = trimmedEmail,
            passwordHash = passwordHash,
            salt = salt,
            isGuest = false
        )

        val id = userDao.insertUser(user)
        val createdUser = user.copy(id = id)
        sessionStore.setSession(id, isGuest = false)
        return Result.success(createdUser)
    }

    open suspend fun login(email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return Result.failure(IllegalArgumentException("User not found with this email"))

        if (user.isGuest) {
            sessionStore.setSession(user.id, isGuest = true)
            return Result.success(user)
        }

        val isValid = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }

        sessionStore.setSession(user.id, isGuest = false)
        return Result.success(user)
    }

    open suspend fun loginAsGuest(): Result<UserEntity> {
        val guestEmail = "guest_${System.currentTimeMillis()}@codemate.local"
        val user = UserEntity(
            name = "Guest Learner",
            email = guestEmail,
            passwordHash = "",
            salt = "",
            isGuest = true
        )
        val id = userDao.insertUser(user)
        val guestUser = user.copy(id = id)
        sessionStore.setSession(id, isGuest = true)
        return Result.success(guestUser)
    }

    open suspend fun logout() {
        sessionStore.clearSession()
    }
}
