package com.princeraj.campustaxipooling.repository;

import androidx.lifecycle.LiveData;

import com.princeraj.campustaxipooling.model.Report;
import com.princeraj.campustaxipooling.util.SafeResult;

import java.util.List;

/**
 * Repository interface for all report and admin operations.
 */
public interface IReportRepository {

    /**
     * Submits a user report.
     * Atomically increments the reported user's reportCount.
     */
    LiveData<SafeResult<Void>> submitReport(Report report);

    /**
     * Gets all pending reports (admin only).
     */
    LiveData<SafeResult<List<Report>>> getPendingReports();

    /**
     * Gets all reports made against a specific user (admin only).
     */
    LiveData<SafeResult<List<Report>>> getReportsForUser(String targetUid);

    /**
     * Admin marks a report as reviewed with a note.
     */
    LiveData<SafeResult<Void>> reviewReport(String reportId, String status, String adminNote);

    /**
     * Admin bans a user.
     */
    LiveData<SafeResult<Void>> banUser(String targetUid, String reason);

    /**
     * Admin lifts a ban on a user.
     */
    LiveData<SafeResult<Void>> unbanUser(String targetUid);
}

