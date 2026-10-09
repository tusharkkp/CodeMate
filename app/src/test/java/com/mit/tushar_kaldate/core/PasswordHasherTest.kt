package com.mit.tushar_kaldate.core

import com.mit.tushar_kaldate.core.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun generateSalt_producesNonEmptyUniqueSalts() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()

        assertNotNull(salt1)
        assertNotNull(salt2)
        assertTrue(salt1.isNotBlank())
        assertTrue(salt2.isNotBlank())
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun hashPassword_deterministicForSameSaltAndPassword() {
        val salt = PasswordHasher.generateSalt()
        val password = "StrongPassword123!"

        val hash1 = PasswordHasher.hashPassword(password, salt)
        val hash2 = PasswordHasher.hashPassword(password, salt)

        assertEquals(hash1, hash2)
    }

    @Test
    fun hashPassword_differentForDifferentSalts() {
        val salt1 = PasswordHasher.generateSalt()
        val salt2 = PasswordHasher.generateSalt()
        val password = "StrongPassword123!"

        val hash1 = PasswordHasher.hashPassword(password, salt1)
        val hash2 = PasswordHasher.hashPassword(password, salt2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPassword_returnsTrueForCorrectPassword() {
        val salt = PasswordHasher.generateSalt()
        val password = "CorrectHorseBatteryStaple"
        val hash = PasswordHasher.hashPassword(password, salt)

        assertTrue(PasswordHasher.verifyPassword(password, salt, hash))
    }

    @Test
    fun verifyPassword_returnsFalseForWrongPassword() {
        val salt = PasswordHasher.generateSalt()
        val password = "CorrectHorseBatteryStaple"
        val hash = PasswordHasher.hashPassword(password, salt)

        assertFalse(PasswordHasher.verifyPassword("WrongPassword", salt, hash))
    }
}
