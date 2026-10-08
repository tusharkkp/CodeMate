package com.mit.tushar_kaldate

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun homeScreenAndBottomNavigationTabs() {
        // Initial Home Screen check
        onView(withId(R.id.cardGreeting)).check(matches(isDisplayed()))
        onView(withId(R.id.cardEngineStatus)).check(matches(isDisplayed()))

        // Chat tab
        onView(withId(R.id.navigation_chat)).perform(click())
        onView(withId(R.id.chatRoot)).check(matches(isDisplayed()))

        // Learn tab
        onView(withId(R.id.navigation_learn)).perform(click())
        onView(withId(R.id.learnRoot)).check(matches(isDisplayed()))

        // Quiz tab
        onView(withId(R.id.navigation_quiz)).perform(click())
        onView(withId(R.id.quizRoot)).check(matches(isDisplayed()))

        // Progress tab
        onView(withId(R.id.navigation_progress)).perform(click())
        onView(withId(R.id.progressRoot)).check(matches(isDisplayed()))

        // Back to Home tab
        onView(withId(R.id.navigation_home)).perform(click())
        onView(withId(R.id.cardGreeting)).check(matches(isDisplayed()))
    }

    @Test
    fun homeTileNavigatesToExplain() {
        onView(withId(R.id.tileExplain)).perform(click())
        onView(withId(R.id.explainRoot)).check(matches(isDisplayed()))
    }
}
