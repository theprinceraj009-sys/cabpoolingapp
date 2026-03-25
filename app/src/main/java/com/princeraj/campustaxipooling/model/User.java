package com.princeraj.campustaxipooling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;

/**
 * Represents a registered campus user.
 * Stored in Firestore: users/{uid}
 */
public class User implements java.io.Serializable {

    @DocumentId
    private String uid;
    private String name;
    private String email;
    private String rollNumber;
    private String department;
    private String role; // "STUDENT" | "FACULTY" | "ADMIN"
    private String campusId; // "CU_CHANDIGARH"
    private String profilePhotoUrl;
    private String userId; // Some older docs might have userId instead of uid

    private Object phoneNumber; // Hidden by default
    private String subscriptionTier; // "FREE" | "PREMIUM"

    @PropertyName("isBanned")
    private boolean isBanned;

    @PropertyName("isAdmin")
    private boolean isAdmin;

    @PropertyName("isPhoneVisibleToMatches")
    private boolean isPhoneVisibleToMatches = true;

    @PropertyName("verifiedDriver")
    private boolean verifiedDriver = false;

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
    public User() {
    }

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
    public String getUid() {
        return uid;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getRole() {
        return role;
    }

    public String getCampusId() {
        return campusId;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public Object getPhoneNumber() {
        return phoneNumber;
    }

    @Exclude
    public String obtainPhoneNumberString() {
        return phoneNumber != null ? String.valueOf(phoneNumber) : null;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    @PropertyName("isBanned")
    public boolean isBanned() {
        return isBanned;
    }

    @PropertyName("isAdmin")
    public boolean isAdmin() {
        return isAdmin;
    }

    @PropertyName("isPhoneVisibleToMatches")
    public boolean isPhoneVisibleToMatches() {
        return isPhoneVisibleToMatches;
    }

    @PropertyName("isVerifiedDriver")
    public boolean isVerifiedDriver() {
        return verifiedDriver;
    }

    @PropertyName("verifiedDriver")
    public boolean getVerifiedDriver() {
        return verifiedDriver;
    }

    public String getDriverLicense() {
        return driverLicense;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public String getBanReason() {
        return banReason;
    }

    public long getReportCount() {
        return reportCount;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCampusId(String campusId) {
        this.campusId = campusId;
    }

    public void setProfilePhotoUrl(String url) {
        this.profilePhotoUrl = url;
    }

    public void setPhoneNumber(Object p) {
        this.phoneNumber = p;
    }

    @Exclude
    public void assignPhoneNumberString(String p) {
        this.phoneNumber = p;
    }

    public void setSubscriptionTier(String s) {
        this.subscriptionTier = s;
    }

    @PropertyName("isBanned")
    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @PropertyName("isPhoneVisibleToMatches")
    public void setPhoneVisibleToMatches(boolean v) {
        isPhoneVisibleToMatches = v;
    }

    @PropertyName("isVerifiedDriver")
    public void setVerifiedDriver(boolean v) {
        this.verifiedDriver = v;
    }

    public void setDriverLicense(String l) {
        this.driverLicense = l;
    }

    public void setVehicleModel(String m) {
        this.vehicleModel = m;
    }

    public void setVehicleNumber(String n) {
        this.vehicleNumber = n;
    }

    public void setRatingCount(long c) {
        this.ratingCount = c;
    }

    public void setAverageRating(double r) {
        this.averageRating = r;
    }

    public void setBanReason(String r) {
        this.banReason = r;
    }

    public void setReportCount(long c) {
        this.reportCount = c;
    }

    public void setFcmToken(String t) {
        this.fcmToken = t;
    }

    public void setCreatedAt(Timestamp t) {
        this.createdAt = t;
    }

    public void setUpdatedAt(Timestamp t) {
        this.updatedAt = t;
    }

    // ── Helpers (EXCLUDED from database) ───────────────────────────────────────
    @Exclude
    public boolean isPremium() {
        return "PREMIUM".equals(subscriptionTier);
    }

    @Exclude
    public boolean checkAdminPrivileges() {
        return isAdmin || "ADMIN".equals(role);
    }
}
