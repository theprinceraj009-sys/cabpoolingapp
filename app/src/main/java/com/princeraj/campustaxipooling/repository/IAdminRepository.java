package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;
import com.princeraj.campustaxipooling.model.User;
import com.princeraj.campustaxipooling.model.Ride;
import com.princeraj.campustaxipooling.util.SafeResult;
import java.util.List;
import java.util.Map;

/**
 * Interface for Admin-level operations and analytics.
 */
public interface IAdminRepository {
    
    // Stats & Metrics
    LiveData<SafeResult<Map<String, Long>>> getSystemStats();
    
    // User Management
    LiveData<SafeResult<List<User>>> getAllUsers();
    void updateUserBanStatus(String uid, boolean isBanned, String reason);
    
    // Ride Overview
    LiveData<SafeResult<List<Ride>>> getAllRides();
    
    // Report Handling
    LiveData<SafeResult<List<com.princeraj.campustaxipooling.model.Report>>> getPendingReports();
    LiveData<SafeResult<Void>> reviewReport(String reportId, String status, String adminNote);
    LiveData<SafeResult<Void>> banUser(String uid, String reason);
    LiveData<SafeResult<Void>> deleteRide(String rideId);
}
