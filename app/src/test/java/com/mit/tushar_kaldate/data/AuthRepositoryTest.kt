package com.mit.tushar_kaldate.data

import com.mit.tushar_kaldate.core.security.PasswordHasher
import com.mit.tushar_kaldate.data.local.dao.UserDao
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.prefs.SessionStore
import com.mit.tushar_kaldate.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    private val userDao: UserDao = mockk(relaxed = true)
    private val sessionStore: SessionStore = mockk(relaxed = true)
    private val repository = AuthRepository(userDao, sessionStore)

    @Test
    fun register_rejectsBlankName() = runTest {
        val result = repository.register("", "test@codemate.com", "password123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun register_rejectsInvalidEmail() = runTest {
        val result = repository.register("Learner", "not-an-email", "password123")
        assertTrue(result.isFailure)
    }

    @Test
    fun register_rejectsShortPassword() = runTest {
        val result = repository.register("Learner", "learner@codemate.com", "12345")
        assertTrue(result.isFailure)
    }

    @Test
    fun register_rejectsDuplicateEmail() = runTest {
        val existing = UserEntity(
            id = 1L,
            name = "Existing",
            email = "learner@codemate.com",
            passwordHash = "hash",
            salt = "salt"
        )
        coEvery { userDao.getUserByEmail("learner@codemate.com") } returns existing

        val result = repository.register("Learner", "learner@codemate.com", "password123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun register_hashesPasswordAndSetsSessionOnSuccess() = runTest {
        coEvery { userDao.getUserByEmail("new@codemate.com") } returns null

        val slot = slot<UserEntity>()
        coEvery { userDao.insertUser(capture(slot)) } returns 42L

        val result = repository.register("New User", "new@codemate.com", "securePassword123")
        assertTrue(result.isSuccess)

        val user = result.getOrThrow()
        assertEquals(42L, user.id)
        assertEquals("new@codemate.com", slot.captured.email)
        assertFalse(slot.captured.passwordHash.contains("securePassword123"))
        assertTrue(PasswordHasher.verifyPassword("securePassword123", slot.captured.salt, slot.captured.passwordHash))

        coVerify { sessionStore.setSession(42L, isGuest = false) }
    }

    @Test
    fun login_succeedsWithCorrectPassword() = runTest {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("mySecretPassword", salt)
        val user = UserEntity(
            id = 10L,
            name = "Alice",
            email = "alice@codemate.com",
            passwordHash = hash,
            salt = salt
        )
        coEvery { userDao.getUserByEmail("alice@codemate.com") } returns user

        val result = repository.login("alice@codemate.com", "mySecretPassword")
        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrThrow().id)
        coVerify { sessionStore.setSession(10L, isGuest = false) }
    }

    @Test
    fun login_failsWithIncorrectPassword() = runTest {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword("mySecretPassword", salt)
        val user = UserEntity(
            id = 10L,
            name = "Alice",
            email = "alice@codemate.com",
            passwordHash = hash,
            salt = salt
        )
        coEvery { userDao.getUserByEmail("alice@codemate.com") } returns user

        val result = repository.login("alice@codemate.com", "wrongPassword")
        assertTrue(result.isFailure)
    }

    @Test
    fun loginAsGuest_createsGuestAndSetsSession() = runTest {
        coEvery { userDao.insertUser(any()) } returns 99L

        val result = repository.loginAsGuest()
        assertTrue(result.isSuccess)
        val guest = result.getOrThrow()
        assertTrue(guest.isGuest)
        assertEquals(99L, guest.id)
        coVerify { sessionStore.setSession(99L, isGuest = true) }
    }

    @Test
    fun logout_clearsSession() = runTest {
        repository.logout()
        coVerify { sessionStore.clearSession() }
    }
}
