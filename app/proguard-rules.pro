# ProGuard Rules for CampusTaxiPooling Enterprise
# ── Hilt / Dagger (Dependency Injection) ──────────────────────────
-keep class androidx.hilt.** { *; }
-keep class dagger.hilt.** { *; }

# ── Room (Persistence) ─────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# ── Firebase (Real-time DB / Auth) ──────────────────────────────
-keep class com.google.firebase.** { *; }
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# ── Models (Keep data classes for serialization) ───────────────────
-keep class com.princeraj.campustaxipooling.model.** { *; }

# ── Debugging Help (Preserve line numbers for Crashlytics) ───────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile