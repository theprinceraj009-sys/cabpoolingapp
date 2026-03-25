package com.princeraj.campustaxipooling.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for caching User profiles locally.
 * Used in Phase 3: Offline Resilience
 */
@Entity(
        tableName = "users",
        indices = {
                @Index("email"),
                @Index("campus_id")
        }
)
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String uid;

    private String name;
    private String email;

    @ColumnInfo(name = "roll_number")
    private String rollNumber;

    private String department;
    private String role;  // STUDENT, FACULTY, ADMIN

    @ColumnInfo(name = "campus_id")
    private String campusId;

    @ColumnInfo(name = "profile_photo_url")
    private String profilePhotoUrl;

    @ColumnInfo(name = "phone_number")
    private String phoneNumber;

    @ColumnInfo(name = "subscription_tier")
    private String subscriptionTier;  // FREE, PREMIUM

    @ColumnInfo(name = "is_banned")
    private boolean isBanned;

    @ColumnInfo(name = "is_admin")
    private boolean isAdmin;

    @ColumnInfo(name = "ban_reason")
    private String banReason;

    @ColumnInfo(name = "report_count")
    private long reportCount;

    @ColumnInfo(name = "fcm_token")
    private String fcmToken;

    @ColumnInfo(name = "created_at")
    private Long createdAt;

    @ColumnInfo(name = "updated_at")
    private Long updatedAt;

    @ColumnInfo(name = "synced_at")
    private Long syncedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public UserEntity() {}

    @androidx.room.Ignore
    public UserEntity(String uid, String name, String email, String rollNumber, 
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

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }

    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }

    public long getReportCount() { return reportCount; }
    public void setReportCount(long reportCount) { this.reportCount = reportCount; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public Long getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Long syncedAt) { this.syncedAt = syncedAt; }
}

