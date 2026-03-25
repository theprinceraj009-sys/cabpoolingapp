package com.princeraj.campustaxipooling;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for the Ride Lifecycle.
 * Validates the flow from searching to posting a ride.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RideLifecycleTest {

    @Rule
    public ActivityScenarioRule<HomeActivity> activityRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testNavigateToPostRide() {
        // 1. Click on FAB in RideFeedFragment
        onView(withId(R.id.postRideFab)).perform(click());

        // 2. Verify navigation to PostRideActivity
        intended(hasComponent(PostRideActivity.class.getName()));
    }

    @Test
    public void testPostRideValidation() {
        // 1. Navigate to PostRideActivity
        onView(withId(R.id.postRideFab)).perform(click());

        // 2. Click Post button without filling fields
        onView(withId(R.id.postRideBtn)).perform(click());

        // 3. Verify presence of SnackBar error (using text from R.string.fill_all_fields via matches)
        onView(withText("Please fill in all required fields"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSourceDestinationValidation() {
        // 1. Navigate to PostRideActivity
        onView(withId(R.id.postRideFab)).perform(click());

        // 2. Type into source but leave others empty
        onView(withId(R.id.sourceEt)).perform(typeText("CU Campus"), closeSoftKeyboard());

        // 3. Click Post
        onView(withId(R.id.postRideBtn)).perform(click());

        // 4. Still should show error
        onView(withText("Please fill in all required fields"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDateAndTimePickersExist() {
        // 1. Navigate to PostRideActivity
        onView(withId(R.id.postRideFab)).perform(click());

        // 2. Click on Date field
        onView(withId(R.id.dateEt)).perform(click());

        // 3. Check if DatePicker is displayed (using class name matcher or similar)
        onView(withClassName(org.hamcrest.Matchers.equalTo(android.widget.DatePicker.class.getName())))
                .check(matches(isDisplayed()));
    }
}
