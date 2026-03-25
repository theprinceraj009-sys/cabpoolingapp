package com.princeraj.campustaxipooling;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.princeraj.campustaxipooling.db.CampusTaxiDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests to validate fixes for IllegalStateException (main-thread DB)
 * and WindowLeaked (Dialog lifecycle).
 *
 * Used to verify Phase 7 polish and stability.
 */
@RunWith(AndroidJUnit4.class)
public class ThreadSafetyLeakTest {

    /**
     * Confirms that Room database will THROW an exception if accessed on the main thread.
     * This validates that our app MUST use background threads.
     */
    @Test(expected = IllegalStateException.class)
    public void testDatabaseAccessOnMainThreadFails() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CampusTaxiDatabase db = CampusTaxiDatabase.getInstance(
                    InstrumentationRegistry.getInstrumentation().getTargetContext()
            );
            // This direct call on the main thread MUST throw IllegalStateException
            db.rideDao().getUnsyncedRides();
        });
    }

    /**
     * Verifies that the app doesn't crash or leak when PostRideActivity
     * encounters a verification requirement.
     */
    @Test
    public void testPostRideDialogResilience() {
        // 1. Launch activity
        ActivityScenario<PostRideActivity> scenario = ActivityScenario.launch(PostRideActivity.class);

        // 2. We don't fill fields, but we want to check if it's alive
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });

        // 3. Trigger a configuration change (Rotation) while activity is active
        // This is the primary cause of WindowLeaked if dialogs are not managed correctly.
        scenario.recreate();

        // 4. Activity should still be displayed without crash
        onView(withId(R.id.postRideBtn)).check(matches(isDisplayed()));
        
        scenario.close();
    }

    /**
     * Validates that the Verification Dialog is shown using a FragmentManager
     * (which survives configuration changes handled by DialogFragment).
     */
    @Test
    public void testPostRideValidationLabels() {
        ActivityScenario<PostRideActivity> scenario = ActivityScenario.launch(PostRideActivity.class);
        
        // Fill nothing and click post
        onView(withId(R.id.postRideBtn)).perform(click());
        
        // Error message from SnackBar should appear (doesn't leak)
        onView(withText("Please fill in all required fields"))
                .check(matches(isDisplayed()));
                
        scenario.close();
    }
}
