package com.princeraj.campustaxipooling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.princeraj.campustaxipooling.model.Ride;

import org.junit.Test;

/**
 * Enterprise QA: Unit testing for core business logic.
 * Ensures that ride status and seat availability calculations are correct across all states.
 */
public class RideModelTest {

    @Test
    public void testRideWithSeats_hasSeatsAvailable() {
        Ride ride = new Ride("uid", "Name", "CU", "Source", "Dest", Timestamp.now(), 100, 4);
        ride.setSeatsRemaining(4);
        ride.setStatus("ACTIVE");
        
        assertTrue("Ride with remaining seats and ACTIVE status should be available", 
                ride.hasSeatsAvailable());
    }

    @Test
    public void testRideWithNoSeats_notAvailable() {
        Ride ride = new Ride("uid", "Name", "CU", "Source", "Dest", Timestamp.now(), 100, 4);
        ride.setSeatsRemaining(0);
        ride.setStatus("FULL");
        
        assertFalse("Ride with 0 seats should not be available", ride.hasSeatsAvailable());
    }

    @Test
    public void testCancelledRide_notActive() {
        Ride ride = new Ride("uid", "Name", "CU", "Source", "Dest", Timestamp.now(), 100, 4);
        ride.setStatus("CANCELLED");
        ride.setDeleted(true);
        
        assertFalse("Cancelled or deleted ride should not be active", ride.isActive());
    }

    @Test
    public void testFareCalculation() {
        Ride ride = new Ride("uid", "Name", "CU", "Source", "Dest", Timestamp.now(), 400, 4);
        assertTrue("Fare per person should be 100", ride.getPerPersonFare() == 100f);
    }
}
