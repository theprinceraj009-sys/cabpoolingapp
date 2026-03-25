package com.princeraj.campustaxipooling.util;

/**
 * ERP-level Configuration system. 
 * Allows dynamic campus switching and feature flagging.
 */
public class AppConfig {

    // Default configuration
    private static String currentCampusId = "CU_CHANDIGARH";
    private static String campusDomain = "@cuchd.in";

    public static String getCampusId() {
        return currentCampusId;
    }

    public static String getCampusDomain() {
        return campusDomain;
    }

    /**
     * ERP Enhancement: Allow switching campus at runtime (e.g. for multi-campus admins).
     */
    public static void setCampus(String campusId, String domain) {
        currentCampusId = campusId;
        campusDomain = domain;
    }

    // Performance tokens
    public static final int MAX_CACHE_SIZE = 100;
    public static final long SYNC_INTERVAL_MS = 15 * 60 * 1000; // 15 mins
}
