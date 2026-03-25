package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

/**
 * Represents a registered campus user.
 * Stored in Firestore: users/{uid}
 */
public class User {

    @DocumentId
    private String uid;
    private String name;
    private String email;
    private String rollNumber;
    private String department;
    private String role;          // "STUDENT" | "FACULTY" | "ADMIN"
    private String campusId;      // "CU_CHANDIGARH"
    private String profilePhotoUrl;
    private String phoneNumber;   // Hidden by default
    private String subscriptionTier; // "FREE" | "PREMIUM"
    private boolean isBanned;
    private boolean isAdmin;      // Explicit boolean for Firestore rules & guard checks
    // Phase 6: Privacy
    private boolean isPhoneVisibleToMatches = true;

    // Phase 4: Driver Verification & Ratings
    private boolean isVerifiedDriver = false;
    private String driverLicense;
    private String vehicleModel;
    private String vehicleNumber;
    private long ratingCount = 0;
    private double averageRating = 5.0; // Default rating
    
    private String banReason;
    private long reportCount;
    private String fcmToken;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Required empty constructor for Firestore deserialization
    public User() {}

    public User(String uid, String name, String email, String rollNumber,
                String department, String role, String campusId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.rollNumber = rollNumber;
        this.department = department;
        this.role = role;
        this.campusId = campusId;
        this.subscriptionTier = "FREE";
        this.isBanned = false;
        this.isAdmin = "ADMIN".equals(role);
        this.reportCount = 0;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getUid() { return uid; }
    
    @PropertyName("userId")
    public String getUserId() { return uid; }
    
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRollNumber() { return rollNumber; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public String getCampusId() { return campusId; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getSubscriptionTier() { return subscriptionTier; }
    public boolean isBanned() { return isBanned; }
    
    @PropertyName("isAdmin")
    public boolean getIsAdmin() { return isAdmin; }
    
    public boolean isVerifiedDriver() { return isVerifiedDriver; }
    public String getDriverLicense() { return driverLicense; }
    public String getVehicleModel() { return vehicleModel; }
    public String getVehicleNumber() { return vehicleNumber; }
    public long getRatingCount() { return ratingCount; }
    public double getAverageRating() { return averageRating; }

    public String getBanReason() { return banReason; }
    public long getReportCount() { return reportCount; }
    public String getFcmToken() { return fcmToken; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    @PropertyName("userId")
    public void setUserId(String userId) { this.uid = userId; }
    
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setRole(String role) { this.role = role; }
    public void setCampusId(String campusId) { this.campusId = campusId; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public void setBanned(boolean banned) { isBanned = banned; }
    
    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) { isAdmin = admin; }
    
    @PropertyName("premium")
    public void setPremium(boolean premium) {
        this.subscriptionTier = premium ? "PREMIUM" : "FREE";
    }

    public void setVerifiedDriver(boolean verifiedDriver) { isVerifiedDriver = verifiedDriver; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    
    public boolean isPhoneVisibleToMatches() { return isPhoneVisibleToMatches; }
    public void setPhoneVisibleToMatches(boolean phoneVisibleToMatches) { isPhoneVisibleToMatches = phoneVisibleToMatches; }
    
    public void setBanReason(String banReason) { this.banReason = banReason; }
    public void setReportCount(long reportCount) { this.reportCount = reportCount; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Exclude
    @PropertyName("premium")
    public boolean isPremium() {
        return "PREMIUM".equals(subscriptionTier);
    }

    /**
     * Returns true if user is an admin.
     * Checks both the boolean field (used in Firestore Security Rules)
     * and the role string (legacy support).
     */
    @Exclude
    public boolean isAdmin() {
        return isAdmin || "ADMIN".equals(role);
    }
}
