package com.princeraj.campustaxipooling.ui.feed;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.repository.IRideRepository;
import com.princeraj.campustaxipooling.util.SafeResult;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * ViewModel for RideFeedFragment.
 * Demonstrates best practices with Hilt DI and SafeResult wrapper.
 *
 * Architecture:
 * - Fragment observes LiveData from ViewModel (UI state)
 * - ViewModel observes repository LiveData (data layer)
 * - Repository returns LiveData<SafeResult<T>> with standardized error handling
 *
 * Benefits:
 * - Separation of concerns (UI, business logic, data)
 * - Automatic lifecycle-aware observation
 * - Configuration change resilience (ViewModel survives rotation)
 * - Easy to test (mock IRideRepository)
 */
@HiltViewModel
public class RideFeedViewModel extends ViewModel {

    private static final String TAG = "RideFeedViewModel";

    private final IRideRepository rideRepository;

    // Public LiveData that Fragment observes
    private final MutableLiveData<SafeResult<List<Ride>>> ridesFeed = new MutableLiveData<>();
    private final MutableLiveData<Integer> pageSize = new MutableLiveData<>(20);

    // Private state
    private String currentCampusId = com.princeraj.campustaxipooling.util.AppConfig.getCampusId();
    private String currentUserUid;
    private boolean isLoadingMore = false;

    @Inject
    public RideFeedViewModel(IRideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    // ── Public Observable Methods ──────────────────────────────────────────────

    public LiveData<SafeResult<List<Ride>>> getRidesFeed() {
        return ridesFeed;
    }

    public LiveData<Integer> getPageSize() {
        return pageSize;
    }

    // ── Data Loading ───────────────────────────────────────────────────────────

    /**
     * Loads the initial ride feed from the repository.
     * Fragment should call this from onViewCreated().
     *
     * @param campusId the campus to filter by
     * @param currentUserUid the current user's UID (to exclude their own rides)
     */
    public void loadRidesFeed(String campusId, String currentUserUid) {
        this.currentCampusId = campusId;
        this.currentUserUid = currentUserUid;

        Log.d(TAG, "Loading rides for campus: " + campusId);

        // Subscribe to repository's LiveData
        rideRepository.getRideFeed(campusId, currentUserUid, pageSize.getValue() != null ? pageSize.getValue() : 20)
                .observeForever(result -> {
                    Log.d(TAG, "Ride feed result: " + result.getStatus());
                    ridesFeed.setValue(result);
                });
    }

    /**
     * Loads more rides (pagination).
     * Called when user scrolls to bottom of list.
     */
    public void loadMoreRides() {
        if (isLoadingMore) {
            Log.w(TAG, "Already loading more rides, ignoring duplicate request");
            return;
        }

        isLoadingMore = true;
        int newPageSize = (pageSize.getValue() != null ? pageSize.getValue() : 20) + 20;
        pageSize.setValue(newPageSize);

        Log.d(TAG, "Loading more rides, new page size: " + newPageSize);

        rideRepository.getRideFeed(currentCampusId, currentUserUid, newPageSize)
                .observeForever(result -> {
                    isLoadingMore = false;
                    ridesFeed.setValue(result);
                });
    }

    /**
     * Retries the previous feed load (if it failed).
     * User taps the retry button in the error state.
     */
    public void retryLoadRidesFeed() {
        Log.d(TAG, "Retrying ride feed load");
        loadRidesFeed(currentCampusId, currentUserUid);
    }

    /**
     * Refreshes the ride feed (pull-to-refresh).
     */
    public void refreshRidesFeed() {
        Log.d(TAG, "Refreshing ride feed");
        pageSize.setValue(20);  // Reset pagination
        loadRidesFeed(currentCampusId, currentUserUid);
    }

    // ── Single Ride Operations ─────────────────────────────────────────────────

    /**
     * Fetches a single ride by ID (used when viewing ride details).
     */
    public void loadRideDetails(String rideId) {
        Log.d(TAG, "Loading ride details for: " + rideId);

        rideRepository.getRideById(rideId)
                .observeForever(result -> {
                    // In a real app, you'd have separate LiveData for ride details
                    Log.d(TAG, "Ride details result: " + result.getStatus());
                });
    }

    /**
     * Sends a join request for a ride.
     * Returns a LiveData<SafeResult<Void>> that the Fragment observes.
     */
    public LiveData<SafeResult<Void>> sendJoinRequest(String rideId, String requesterName, String requesterUid, String posterUid) {
        MutableLiveData<SafeResult<Void>> resultLiveData = new MutableLiveData<>(SafeResult.loading());

        Log.d(TAG, "Sending join request for ride: " + rideId);

        // Create SeatRequest object
        com.princeraj.campustaxipooling.model.SeatRequest request = new com.princeraj.campustaxipooling.model.SeatRequest();
        request.setRequesterUid(requesterUid);
        request.setRequesterName(requesterName);
        request.setStatus("PENDING");

        // Repository returns LiveData<SafeResult<String>> (requestId)
        rideRepository.sendJoinRequest(rideId, request, posterUid)
                .observeForever(result -> {
                    if (result.isSuccess()) {
                        Log.d(TAG, "Join request sent successfully");
                        resultLiveData.setValue(SafeResult.success(null));
                    } else {
                        resultLiveData.setValue(SafeResult.error(
                                result.getException(),
                                result.getUserMessage()
                        ));
                    }
                });

        return resultLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");
        // Repository cleanup happens automatically via Hilt scope management
    }
}

