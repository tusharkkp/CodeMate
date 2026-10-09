package com.mit.tushar_kaldate

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthSessionTest {

    private lateinit var app: CodeMateApp

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<CodeMateApp>()
        runBlocking {
            app.container.authRepository.logout()
        }
    }

    @Test
    fun guestLoginFlow_navigatesToHome_andLogoutReturnsToLogin() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Should be on login screen
            onView(withId(R.id.cardLogin)).check(matches(isDisplayed()))

            // 2. Click Continue as Guest
            onView(withId(R.id.btnGuestLogin)).perform(click())

            // 3. Should arrive on Home screen
            onView(withId(R.id.cardGreeting)).check(matches(isDisplayed()))

            // 4. Navigate to Settings
            scenario.onActivity { activity ->
                val navHost = activity.supportFragmentManager.findFragmentById(R.id.navHostFragment) as androidx.navigation.fragment.NavHostFragment
                navHost.navController.navigate(R.id.navigation_settings)
            }
            onView(withId(R.id.cardUserProfile)).check(matches(isDisplayed()))

            // 5. Click Logout and confirm dialog
            onView(withId(R.id.btnLogout)).perform(click())
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.action_logout)).perform(click())

            // 6. Should return to Login screen
            onView(withId(R.id.cardLogin)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun registerFlow_createsUser_andNavigatesToHome() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Navigate from Login to Register
            onView(withId(R.id.btnGoToRegister)).perform(click())
            onView(withId(R.id.cardRegister)).check(matches(isDisplayed()))

            // 2. Fill registration fields
            val email = "learner_${System.currentTimeMillis()}@codemate.app"
            onView(withId(R.id.etRegisterName)).perform(replaceText("Learner"), closeSoftKeyboard())
            onView(withId(R.id.etRegisterEmail)).perform(replaceText(email), closeSoftKeyboard())
            onView(withId(R.id.etRegisterPassword)).perform(replaceText("pass1234"), closeSoftKeyboard())
            onView(withId(R.id.etRegisterConfirmPassword)).perform(replaceText("pass1234"), closeSoftKeyboard())

            // 3. Click Register
            onView(withId(R.id.btnRegister)).perform(click())

            // 4. Should reach Home screen
            onView(withId(R.id.cardGreeting)).check(matches(isDisplayed()))
        }
    }
}
