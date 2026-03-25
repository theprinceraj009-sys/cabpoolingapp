package com.princeraj.campustaxipooling;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for LoginActivity.
 * Ensures the login form handles validation errors correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginUiTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmptyFieldsShowError() {
        // 1. Click login with empty fields
        onView(withId(R.id.loginBtn)).perform(click());

        // 2. Check for layout error
        onView(hasDescendant(withText("Email is required")))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidDomainShowsError() {
        // 1. Type invalid domain email
        onView(withId(R.id.emailEt))
                .perform(typeText("user@gmail.com"), closeSoftKeyboard());

        // 2. Click login
        onView(withId(R.id.loginBtn)).perform(click());

        // 3. Expected error for domain restriction
        String errorMsg = InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.email_domain_error);
        onView(hasDescendant(withText(errorMsg)))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testPasswordRequiredError() {
        // 1. Type valid email but empty password
        onView(withId(R.id.emailEt))
                .perform(typeText("21BCS1234@cuchd.in"), closeSoftKeyboard());

        // 2. Click login
        onView(withId(R.id.loginBtn)).perform(click());

        // 3. Check for password error
        onView(hasDescendant(withText("Password is required")))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButtonDisablesOnSubmit() {
        // 1. Fill fields
        onView(withId(R.id.emailEt))
                .perform(typeText("21BCS1234@cuchd.in"), closeSoftKeyboard());
        onView(withId(R.id.passwordEt))
                .perform(typeText("password123"), closeSoftKeyboard());

        // 2. Click login
        onView(withId(R.id.loginBtn)).perform(click());

        // 3. Button should be disabled immediately (during loading)
        onView(withId(R.id.loginBtn)).check(matches(isNotEnabled()));
    }
}
